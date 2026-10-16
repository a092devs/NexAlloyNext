package io.github.nexalloy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.nexalloy.activity.SettingApplication
import io.github.nexalloy.manager.BundleManager
import java.io.File

/**
 * Debug builds only: installs a patch bundle from a file on this device without the UI,
 * which makes bundle deployment testable over adb on devices where input injection is blocked.
 *
 * ```
 * adb shell am broadcast -a io.github.nexalloy.next.DEBUG_INSTALL_BUNDLE \
 *     -n io.github.nexalloy.next/io.github.nexalloy.DebugBundleInstallReceiver \
 *     --es path /data/data/io.github.nexalloy.next/files/nexalloy.nxb
 * ```
 *
 * The outcome is written to `files/bundle-install-result.txt`.
 */
class DebugBundleInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val path = intent.getStringExtra("path") ?: return
        val pending = goAsync()
        Thread {
            val resultFile = File(context.filesDir, "bundle-install-result.txt")
            try {
                resultFile.writeText("RUNNING")
                // The app is normally launched before the broadcast, but the service bind may still be pending.
                val deadline = System.currentTimeMillis() + 6_000
                while (SettingApplication.mService == null && System.currentTimeMillis() < deadline) {
                    Thread.sleep(100)
                }
                val app = context.applicationContext as SettingApplication
                val manager = BundleManager(app, SettingApplication.mService)
                val bundle = File(path).inputStream().use { manager.install(it, null) }
                manager.syncToService()
                resultFile.writeText("OK ${bundle.id} ${bundle.version} ${bundle.fileName}")
            } catch (t: Throwable) {
                resultFile.writeText("FAIL ${t.javaClass.simpleName}: ${t.message}")
            } finally {
                pending.finish()
            }
        }.start()
    }
}
