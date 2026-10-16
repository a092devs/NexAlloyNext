package io.github.nexalloy.manager

import android.content.Context
import io.github.nexalloy.core.BuildConfig
import io.github.nexalloy.core.compatibility.VersionComparator
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Provenance classification of a patch source.
 */
enum class SourceProvenance {
    OFFICIAL,
    COMMUNITY,
    NEXALLOY,
    CUSTOM;

    companion object {
        fun fromUrl(url: String): SourceProvenance {
            val lower = url.lowercase().trim()
            return when {
                lower.contains("morpheapp") -> OFFICIAL
                lower.contains("a092devs/nexalloy") || lower.contains("a092devs") -> NEXALLOY
                lower.contains("2eno") || lower.contains("crimera") || lower.contains("hoo-dles") -> COMMUNITY
                else -> CUSTOM
            }
        }
    }
}

/**
 * A place NexAlloy downloads patch bundles from, like a Morphe patch source.
 *
 * [url] is either a GitHub repository (`owner/repo` or `https://github.com/owner/repo`), whose
 * latest release provides the bundles as `.nxb` assets, or the URL of a bundle JSON file:
 * ```json
 * { "version": "1.2.0", "download_url": "https://…/bundle.nxb", "description": "changelog" }
 * ```
 */
data class PatchSource(
    val id: String,
    val name: String,
    val url: String,
    /** Also consider GitHub pre-releases. */
    val prerelease: Boolean = false,
    /** Whether this source is enabled for catalog discovery and updates. */
    val enabled: Boolean = true,
    /** Provenance classification of this source. */
    val provenance: SourceProvenance = SourceProvenance.CUSTOM,
    /** Release version whose bundles are installed, null if none are. */
    val installedVersion: String? = null,
    /** Epoch timestamp of the last successful refresh. */
    val lastRefreshTime: Long = 0L,
    /** Error message from the last refresh attempt, if any. */
    val lastError: String? = null,
) {
    val githubRepository: String?
        get() = GITHUB_REPO.matchEntire(url.trim().removeSuffix("/").removeSuffix(".git"))?.groupValues?.get(1)

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("url", url)
        .put("prerelease", prerelease)
        .put("enabled", enabled)
        .put("provenance", provenance.name)
        .put("installedVersion", installedVersion)
        .put("lastRefreshTime", lastRefreshTime)
        .put("lastError", lastError)

    companion object {
        private val GITHUB_REPO = Regex("""(?:https?://github\.com/)?([\w.-]+/[\w.-]+)""")

        fun fromJson(json: JSONObject): PatchSource {
            val url = json.getString("url")
            val provenanceName = json.optString("provenance")
            val provenance = runCatching { SourceProvenance.valueOf(provenanceName) }
                .getOrElse { SourceProvenance.fromUrl(url) }

            return PatchSource(
                id = json.getString("id"),
                name = json.getString("name"),
                url = url,
                prerelease = json.optBoolean("prerelease", false),
                enabled = json.optBoolean("enabled", true),
                provenance = provenance,
                installedVersion = json.optString("installedVersion").takeIf { it.isNotEmpty() && it != "null" },
                lastRefreshTime = json.optLong("lastRefreshTime", 0L),
                lastError = json.optString("lastError").takeIf { it.isNotEmpty() && it != "null" },
            )
        }

        fun create(
            name: String,
            url: String,
            prerelease: Boolean = false,
            provenance: SourceProvenance = SourceProvenance.fromUrl(url),
        ) = PatchSource(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifEmpty { url.substringAfterLast('/') },
            url = url.trim(),
            prerelease = prerelease,
            enabled = true,
            provenance = provenance,
        )

        /** The unified NexAlloy Patches bundle (NexAlloy, 2eno, Piko). */
        val default = PatchSource(
            id = "default",
            name = "NexAlloy Unified Patches",
            url = "a092devs/NexAlloyNext-Patches",
            provenance = SourceProvenance.NEXALLOY,
        )

        /** Known curated community and upstream sources available for one-tap addition. */
        val UPSTREAM_2ENO = PatchSource(
            id = "2eno_upstream",
            name = "2eno NexAlloy Patches",
            url = "2eno/NexAlloyNext-Patches",
            provenance = SourceProvenance.COMMUNITY,
        )

        val COMMUNITY_PIKO = PatchSource(
            id = "crimera_piko",
            name = "Piko Patches (Instagram & X)",
            url = "crimera/piko",
            provenance = SourceProvenance.COMMUNITY,
        )

        val COMMUNITY_2ENO_MORPHE = PatchSource(
            id = "2eno_morphe",
            name = "2eno Patches (Spotify & Extras)",
            url = "2eno/2eno-patches",
            provenance = SourceProvenance.COMMUNITY,
        )

        val OFFICIAL_MORPHE = PatchSource(
            id = "morphe_official",
            name = "Official Morphe Patches",
            url = "MorpheApp/morphe-patches",
            provenance = SourceProvenance.OFFICIAL,
        )

        val PRESET_SOURCES = listOf(default, UPSTREAM_2ENO, COMMUNITY_PIKO, COMMUNITY_2ENO_MORPHE, OFFICIAL_MORPHE)
    }
}

/** A release of a [PatchSource]. */
data class SourceRelease(
    val version: String,
    val changelog: String,
    val pageUrl: String?,
    /** File name to download URL of each bundle in the release. */
    val bundles: List<Pair<String, String>>,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("version", version)
        put("changelog", changelog)
        put("pageUrl", pageUrl)
        val array = JSONArray()
        bundles.forEach { (name, downloadUrl) ->
            array.put(JSONObject().put("name", name).put("url", downloadUrl))
        }
        put("bundles", array)
    }

    companion object {
        fun fromJson(json: JSONObject): SourceRelease {
            val bundleArray = json.optJSONArray("bundles")
            val bundleList = if (bundleArray != null) {
                (0 until bundleArray.length()).mapNotNull { i ->
                    val obj = bundleArray.optJSONObject(i) ?: return@mapNotNull null
                    obj.getString("name") to obj.getString("url")
                }
            } else emptyList()

            return SourceRelease(
                version = json.getString("version"),
                changelog = json.optString("changelog", ""),
                pageUrl = json.optString("pageUrl").takeIf { it.isNotEmpty() && it != "null" },
                bundles = bundleList,
            )
        }
    }
}

/** Persistent cache for source releases so metadata survives network failures and app restarts. */
class SourceReleaseCache(context: Context) {
    private val prefs = context.getSharedPreferences("sources_release_cache", Context.MODE_PRIVATE)

    fun get(sourceId: String): SourceRelease? {
        val raw = prefs.getString(sourceId, null) ?: return null
        return runCatching { SourceRelease.fromJson(JSONObject(raw)) }.getOrNull()
    }

    fun put(sourceId: String, release: SourceRelease) {
        prefs.edit().putString(sourceId, release.toJson().toString()).apply()
    }

    fun remove(sourceId: String) {
        prefs.edit().remove(sourceId).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}

/** Persists the configured sources in the manager's own preferences. */
class SourceStore(context: Context) {
    private val prefs = context.getSharedPreferences("sources", Context.MODE_PRIVATE)

    fun all(): List<PatchSource> {
        val raw = prefs.getString(KEY, null) ?: return listOf(PatchSource.default)
        val loaded = runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { PatchSource.fromJson(array.getJSONObject(it)) }
        }.getOrDefault(listOf(PatchSource.default))

        val migrated = migrateSources(loaded)
        if (migrated != loaded) {
            write(migrated)
        }
        return migrated
    }

    private fun migrateSources(sources: List<PatchSource>): List<PatchSource> {
        val result = mutableListOf<PatchSource>()
        var defaultIncluded = false

        for (source in sources) {
            if (source.id == "default" || source.url.contains("a092devs/NexAlloy", ignoreCase = true)) {
                if (!defaultIncluded) {
                    result.add(
                        PatchSource.default.copy(
                            installedVersion = source.installedVersion ?: PatchSource.default.installedVersion,
                            enabled = source.enabled,
                            lastRefreshTime = source.lastRefreshTime,
                            lastError = source.lastError,
                        )
                    )
                    defaultIncluded = true
                }
            } else {
                // Preserve all other sources! Do not wipe or collapse them!
                result.add(source)
            }
        }

        if (!defaultIncluded) {
            result.add(0, PatchSource.default)
        }
        return result.distinctBy { it.id }
    }

    fun get(id: String) = all().find { it.id == id }

    fun save(source: PatchSource) = write(all().filter { it.id != source.id } + source)

    fun setEnabled(id: String, enabled: Boolean) {
        val updated = all().map { if (it.id == id) it.copy(enabled = enabled) else it }
        write(updated)
    }

    fun updateStatus(id: String, lastRefresh: Long, lastError: String?) {
        val updated = all().map {
            if (it.id == id) it.copy(lastRefreshTime = lastRefresh, lastError = lastError) else it
        }
        write(updated)
    }

    fun remove(id: String) = write(all().filter { it.id != id })

    private fun write(sources: List<PatchSource>) {
        val array = JSONArray()
        sources.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private companion object {
        const val KEY = "sources"
    }
}

/** Fetches releases of patch sources. Blocking; call off the main thread. */
object SourceClient {
    const val BUNDLE_EXTENSION = ".nxb"

    fun latestRelease(source: PatchSource): SourceRelease {
        val repository = source.githubRepository
        return if (repository != null && !source.url.contains("raw.githubusercontent.com")) {
            latestGitHubRelease(repository, source.prerelease)
        } else {
            jsonRelease(source.url)
        }
    }

    private fun latestGitHubRelease(repository: String, prerelease: Boolean): SourceRelease {
        val response = runCatching {
            get("https://api.github.com/repos/$repository/releases?per_page=20")
        }.getOrElse { error ->
            throw SourceException("Failed to fetch releases from $repository: ${error.message}")
        }
        val releases = JSONArray(response)
        val candidates = (0 until releases.length()).map { releases.getJSONObject(it) }
            .filter { !it.optBoolean("draft") }
            .filter { it.getJSONArray("assets").let { assets ->
                (0 until assets.length()).any { i -> assets.getJSONObject(i).getString("name").endsWith(BUNDLE_EXTENSION) }
            } }
            .sortedWith { r1, r2 ->
                VersionComparator.compare(r2.optString("tag_name", ""), r1.optString("tag_name", ""))
            }
        // Without a stable release, fall back to pre-releases, e.g. for projects still in alpha.
        val allowPrerelease = prerelease || candidates.none { !it.optBoolean("prerelease") }
        for (release in candidates) {
            if (release.optBoolean("prerelease") && !allowPrerelease) continue
            val assets = release.getJSONArray("assets")
            val bundles = (0 until assets.length()).map { assets.getJSONObject(it) }
                .filter { it.getString("name").endsWith(BUNDLE_EXTENSION) }
                .map { it.getString("name") to it.getString("browser_download_url") }
            if (bundles.isEmpty()) continue
            return SourceRelease(
                version = release.getString("tag_name"),
                changelog = release.optString("body"),
                pageUrl = release.optString("html_url"),
                bundles = bundles,
            )
        }
        throw SourceException("No release of $repository contains a $BUNDLE_EXTENSION patch bundle")
    }

    private fun jsonRelease(url: String): SourceRelease {
        val json = JSONObject(get(url))
        val downloadUrl = json.optString("download_url").takeIf { it.isNotEmpty() }
            ?: throw SourceException("$url has no download_url")
        return SourceRelease(
            version = json.optString("version", downloadUrl),
            changelog = json.optString("description"),
            pageUrl = null,
            bundles = listOf(downloadUrl.substringAfterLast('/') to downloadUrl),
        )
    }

    fun get(url: String): String = open(url).inputStream.use { it.readBytes().decodeToString() }

    fun open(url: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.setRequestProperty("User-Agent", "NexAlloy/${BuildConfig.VERSION_NAME}")
        if (url.startsWith("https://api.github.com/")) {
            connection.setRequestProperty("Accept", "application/vnd.github+json")
        }
        val code = connection.responseCode
        if (code !in 200..299) throw SourceException("HTTP $code for $url")
        return connection
    }
}

class SourceException(message: String) : Exception(message)
