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
        val releases = JSONArray(SourceClient.get("https://api.github.com/repos/${BuildConfig.UPDATE_REPOSITORY}/releases?per_page=20"))
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
}
