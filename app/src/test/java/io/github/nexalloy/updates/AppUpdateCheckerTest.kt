package io.github.nexalloy.updates

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppUpdateCheckerTest {

    @Test
    fun parseAtomFeedExtractsLatestReleaseAndApk() {
        val sampleXml = """
<?xml version="1.0" encoding="UTF-8"?>
<feed xmlns="http://www.w3.org/2005/Atom">
  <entry>
    <link rel="alternate" type="text/html" href="https://github.com/a092devs/NexAlloyNext/releases/tag/3.4.3"/>
    <title>3.4.3</title>
    <content type="html">&lt;p&gt;Responsive UI Layout&lt;/p&gt;</content>
  </entry>
  <entry>
    <link rel="alternate" type="text/html" href="https://github.com/a092devs/NexAlloyNext/releases/tag/3.4.2"/>
    <title>3.4.2</title>
    <content type="html">&lt;p&gt;UI Rollback&lt;/p&gt;</content>
  </entry>
</feed>
""".trimIndent()

        val release = AppUpdateChecker.parseAtomFeed(sampleXml, "a092devs/NexAlloyNext")
        assertNotNull(release)
        assertEquals("3.4.3", release.tag)
        assertEquals(3, release.versionCode)
        assertTrue(release.apkUrl?.endsWith(".apk") == true)
        assertEquals("https://github.com/a092devs/NexAlloyNext/releases/tag/3.4.3", release.pageUrl)
    }

    @Test
    fun parseApiResponseExtractsLatestRelease() {
        val sampleJson = """
[
  {
    "tag_name": "3.4.3",
    "draft": false,
    "prerelease": false,
    "body": "Responsive UI updates",
    "html_url": "https://github.com/a092devs/NexAlloyNext/releases/tag/3.4.3",
    "assets": [
      {
        "name": "NexAlloyNext-3.4.3.apk",
        "browser_download_url": "https://github.com/a092devs/NexAlloyNext/releases/download/3.4.3/NexAlloyNext-3.4.3.apk"
      }
    ]
  }
]
""".trimIndent()

        val release = AppUpdateChecker.parseApiResponse(sampleJson)
        assertNotNull(release)
        assertEquals("3.4.3", release.tag)
        assertEquals(3, release.versionCode)
        assertEquals("https://github.com/a092devs/NexAlloyNext/releases/download/3.4.3/NexAlloyNext-3.4.3.apk", release.apkUrl)
    }
}