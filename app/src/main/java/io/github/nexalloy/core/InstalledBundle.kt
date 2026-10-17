package io.github.nexalloy.core

import android.content.SharedPreferences
import io.github.nexalloy.AppTarget
import io.github.nexalloy.core.compatibility.CompatibilityEngine
import org.json.JSONArray
import org.json.JSONObject

/**
 * A patch bundle installed through the manager.
 *
 * The registry of installed bundles lives in the remote preferences, so hooked apps know which
 * bundles target them without loading every bundle. The bundle APK itself is a remote file.
 */
data class InstalledBundle(
    /** Package name of the bundle APK. */
    val id: String,
    val name: String,
    val version: String,
    val versionCode: Long,
    val author: String,
    val description: String,
    /** Name of the remote file holding the bundle APK. */
    val fileName: String,
    val enabled: Boolean,
    /** Id of the [PatchSource] the bundle was installed from, or null if imported from a file. */
    val sourceId: String?,
    /** Package name to display name of every app the bundle patches. */
    val apps: Map<String, String>,
    /** Package id of the bundle's resources, 0 if unknown. */
    val resourcePackageId: Int = 0,
    /** Package name to the app versions the bundle was tested with, newest first. */
    val targets: Map<String, List<AppTarget>> = emptyMap(),
    /** Package name to the brand color (0xRRGGBB) the bundle declares for the app. */
    val appIconColors: Map<String, Int> = emptyMap(),
    /** Compatibility series (e.g. "3.2"). */
    val compatibilitySeries: String = "",
    /** Minimum compatible NexAlloyNext manager version. */
    val minManagerVersion: String = "",
    /** Maximum compatible NexAlloyNext manager version. */
    val maxManagerVersion: String = "",
) {
    /** The version this bundle recommends for an app, or null for any version. */
    fun recommendedVersion(packageName: String): String? =
        CompatibilityEngine.recommendedVersion(targets[packageName].orEmpty())

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("version", version)
        .put("versionCode", versionCode)
        .put("author", author)
        .put("description", description)
        .put("fileName", fileName)
        .put("enabled", enabled)
        .put("sourceId", sourceId)
        .put("apps", JSONObject(apps))
        .put("resourcePackageId", resourcePackageId)
        .put("compatibilitySeries", compatibilitySeries)
        .put("minManagerVersion", minManagerVersion)
        .put("maxManagerVersion", maxManagerVersion)
        .put("appIconColors", JSONObject(appIconColors))
        .put("targets", JSONObject().apply {
            targets.forEach { (packageName, versions) ->
                put(packageName, JSONArray().apply {
                    versions.forEach { put(JSONObject().put("version", it.version).put("experimental", it.experimental)) }
                })
            }
        })

    companion object {
        fun fromJson(json: JSONObject): InstalledBundle {
            val appsMap = json.optJSONObject("apps")?.let { apps ->
                apps.keys().asSequence().associateWith { apps.optString(it, "") }
            }.orEmpty()

            val colorsMap = json.optJSONObject("appIconColors")?.let { colors ->
                colors.keys().asSequence().mapNotNull { key ->
                    colors.optInt(key).takeIf { it != 0 }?.let { key to it }
                }.toMap()
            }.orEmpty()

            val targetsMap = json.optJSONObject("targets")?.let { targets ->
                targets.keys().asSequence().associateWith { packageName ->
                    val array = targets.optJSONArray(packageName) ?: return@associateWith emptyList()
                    (0 until array.length()).mapNotNull { i ->
                        val obj = array.optJSONObject(i)
                        if (obj != null) {
                            val v = obj.optString("version", "")
                            if (v.isNotEmpty()) AppTarget(v, obj.optBoolean("experimental", false)) else null
                        } else {
                            val str = array.optString(i, "")
                            if (str.isNotEmpty()) AppTarget(str, false) else null
                        }
                    }
                }
            }.orEmpty()

            return InstalledBundle(
                id = json.optString("id", ""),
                name = json.optString("name", "Unknown Bundle"),
                version = json.optString("version", "1.0.0"),
                versionCode = json.optLong("versionCode", 0L),
                author = json.optString("author", ""),
                description = json.optString("description", ""),
                fileName = json.optString("fileName", ""),
                enabled = json.optBoolean("enabled", true),
                sourceId = json.optString("sourceId").takeIf { it.isNotEmpty() && it != "null" },
                apps = appsMap,
                resourcePackageId = json.optInt("resourcePackageId", 0),
                compatibilitySeries = json.optString("compatibilitySeries", ""),
                minManagerVersion = json.optString("minManagerVersion", ""),
                maxManagerVersion = json.optString("maxManagerVersion", ""),
                appIconColors = colorsMap,
                targets = targetsMap,
            )
        }
    }
}

/** Reads and writes the installed bundles in the shared remote preferences. */
object BundleRegistry {
    /** Remote preferences group shared with hooked apps. */
    const val PREFS_GROUP = "bundles"
    private const val KEY = "installed"

    fun read(prefs: SharedPreferences): List<InstalledBundle> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                runCatching { InstalledBundle.fromJson(array.getJSONObject(i)) }.getOrNull()
            }
        }.getOrDefault(emptyList())
    }

    fun write(prefs: SharedPreferences, bundles: List<InstalledBundle>) {
        val array = JSONArray()
        bundles.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY, array.toString()).commit()
    }
}
