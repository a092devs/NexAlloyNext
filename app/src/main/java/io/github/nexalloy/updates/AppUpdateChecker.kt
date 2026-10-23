package io.github.nexalloy.updates

import io.github.nexalloy.core.BuildConfig
import io.github.nexalloy.core.compatibility.VersionComparator
import io.github.nexalloy.manager.SourceClient
import org.json.JSONArray

/** A release of NexAlloy itself. */
class AppRelease(
    val tag: String,
    val versionCode: Int,
    val notes: String,
    val pageUrl: String,
    /** Download URL of the APK asset, if the release has one. */
    val apkUrl: String?,
) {
    val isNewer get() = VersionComparator.compare(tag, BuildConfig.VERSION_NAME) > 0 || (versionCode > BuildConfig.VERSION_CODE)
}

/** Checks the GitHub releases of NexAlloy for updates. Blocking; call off the main thread. */
object AppUpdateChecker {
    /** The version code a release tag `X.Y.Z` or `X.Y.Z-suffix` stands for, Z. 0 for other tags, e.g. nightly builds. */
    fun versionCodeOf(tag: String): Int =
        Regex("""v?\d+\.\d+\.(\d+)(-.*)?""").matchEntire(tag)?.groupValues?.get(1)?.toInt() ?: 0

    /** The latest release, preferring stable ones; pre-releases count while there is no stable release. */
    fun latest(): AppRelease? {
        val repo = BuildConfig.UPDATE_REPOSITORY
        // Try the GitHub REST API first
        val apiResult = runCatching {
            val response = SourceClient.get("https://api.github.com/repos/$repo/releases?per_page=20")
            parseApiResponse(response)
        }
        if (apiResult.isSuccess && apiResult.getOrNull() != null) {
            return apiResult.getOrNull()
        }

        // Fallback: parse releases.atom directly from github.com (immune to GitHub API rate limits)
        return runCatching {
            val atomXml = SourceClient.get("https://github.com/$repo/releases.atom")
            parseAtomFeed(atomXml, repo)
        }.getOrElse { fallbackError ->
            throw apiResult.exceptionOrNull() ?: fallbackError
        }
    }

    internal fun parseApiResponse(jsonText: String): AppRelease? {
        val releases = JSONArray(jsonText)
        val candidates = (0 until releases.length()).map { releases.getJSONObject(it) }
            .filter { !it.optBoolean("draft") && VersionComparator.compare(it.getString("tag_name"), "0.0.0") > 0 }
        val stable = candidates.filter { !it.optBoolean("prerelease") }
        val release = (stable.ifEmpty { candidates }).maxWithOrNull { a, b ->
            VersionComparator.compare(a.getString("tag_name"), b.getString("tag_name"))
        } ?: return null
        val assets = release.getJSONArray("assets")
        val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
            .firstOrNull { it.getString("name").endsWith(".apk") }
        return AppRelease(
            tag = release.getString("tag_name"),
            versionCode = versionCodeOf(release.getString("tag_name")),
            notes = release.optString("body"),
            pageUrl = release.getString("html_url"),
            apkUrl = apk?.getString("browser_download_url"),
        )
    }

    internal fun parseAtomFeed(atomXml: String, repo: String): AppRelease? {
        val entryRegex = Regex("""<entry>([\s\S]*?)</entry>""")
        val titleRegex = Regex("""<title>([^<]+)</title>""")
        val linkRegex = Regex("""<link[^>]*href="([^"]+)"""")
        val contentRegex = Regex("""<content[^>]*>([\s\S]*?)</content>""")

        data class AtomEntry(val tag: String, val pageUrl: String, val content: String)

        val entries = entryRegex.findAll(atomXml).mapNotNull { match ->
            val block = match.groupValues[1]
            val link = linkRegex.find(block)?.groupValues?.get(1).orEmpty()
            val tag = link.substringAfterLast("/releases/tag/", "").takeIf { it.isNotEmpty() }
                ?: titleRegex.find(block)?.groupValues?.get(1)?.trim().orEmpty()
            if (tag.isEmpty() || VersionComparator.compare(tag, "0.0.0") <= 0) return@mapNotNull null
            val content = contentRegex.find(block)?.groupValues?.get(1).orEmpty()
            AtomEntry(tag, link, content)
        }.toList()

        if (entries.isEmpty()) return null

        val stable = entries.filter { !it.tag.contains('-') && it.tag != "autobuild" }
        val chosen = (stable.ifEmpty { entries }).maxWithOrNull { a, b ->
            VersionComparator.compare(a.tag, b.tag)
        } ?: return null

        var apkUrl: String? = null
        val expandedAssets = runCatching {
            SourceClient.get("https://github.com/$repo/releases/expanded_assets/${chosen.tag}")
        }.getOrNull()

        if (expandedAssets != null) {
            val apkMatch = Regex("""href="(/[^"]+/releases/download/[^"]+?\.apk)"""").find(expandedAssets)
            if (apkMatch != null) {
                apkUrl = "https://github.com" + apkMatch.groupValues[1]
            }
        }

        if (apkUrl == null) {
            apkUrl = "https://github.com/$repo/releases/download/${chosen.tag}/NexAlloyNext-${chosen.tag}.apk"
        }

        return AppRelease(
            tag = chosen.tag,
            versionCode = versionCodeOf(chosen.tag),
            notes = chosen.content,
            pageUrl = chosen.pageUrl,
            apkUrl = apkUrl,
        )
    }
}
