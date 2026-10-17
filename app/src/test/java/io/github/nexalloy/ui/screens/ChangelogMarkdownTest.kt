package io.github.nexalloy.ui.screens

import kotlin.test.Test
import kotlin.test.assertEquals

class ChangelogMarkdownTest {

    @Test
    fun inlineCodeRendersWithoutBackticks() {
        assertEquals(
            listOf(
                MarkdownSpan("the "),
                MarkdownSpan("piko", code = true),
                MarkdownSpan(" bundle is removed"),
            ),
            parseMarkdownSpans("the `piko` bundle is removed"),
        )
    }

    @Test
    fun realisticReleaseNoteKeepsCodeRuns() {
        assertEquals(
            listOf(
                MarkdownSpan("X ("),
                MarkdownSpan("com.twitter.android", code = true),
                MarkdownSpan(" 12.35.0-prod.01): new app entry"),
            ),
            parseMarkdownSpans("X (`com.twitter.android` 12.35.0-prod.01): new app entry"),
        )
    }

    @Test
    fun boldBareUrlAndMarkdownLinkAreStyled() {
        assertEquals(
            listOf(
                MarkdownSpan("Full Changelog", bold = true),
                MarkdownSpan(": "),
                MarkdownSpan("https://example.com/compare", url = "https://example.com/compare"),
                MarkdownSpan(" see "),
                MarkdownSpan("the notes", url = "https://example.com/notes"),
            ),
            parseMarkdownSpans("**Full Changelog**: https://example.com/compare see [the notes](https://example.com/notes)"),
        )
    }

    @Test
    fun nestedCodeInsideBoldKeepsBothStyles() {
        assertEquals(
            listOf(
                MarkdownSpan("bold ", bold = true),
                MarkdownSpan("code", code = true, bold = true),
            ),
            parseMarkdownSpans("**bold `code`**"),
        )
    }

    @Test
    fun underscoresInsideIdentifiersStayPlain() {
        assertEquals(
            listOf(MarkdownSpan("intent_aware_ad_pivot and utm_x")),
            parseMarkdownSpans("intent_aware_ad_pivot and utm_x"),
        )
    }

    @Test
    fun bulletMarkerIsNotItalic() {
        assertEquals(listOf(MarkdownSpan("* item")), parseMarkdownSpans("* item"))
    }

    @Test
    fun italicAndStrikethrough() {
        assertEquals(
            listOf(
                MarkdownSpan("the "),
                MarkdownSpan("fast", italic = true),
                MarkdownSpan(" "),
                MarkdownSpan("gone", strikethrough = true),
                MarkdownSpan(" path"),
            ),
            parseMarkdownSpans("the *fast* ~~gone~~ path"),
        )
    }

    @Test
    fun trailingSentencePunctuationStaysOutOfUrl() {
        assertEquals(
            listOf(
                MarkdownSpan("see "),
                MarkdownSpan("https://example.com/x", url = "https://example.com/x"),
                MarkdownSpan("."),
            ),
            parseMarkdownSpans("see https://example.com/x."),
        )
    }

    @Test
    fun bulletAbsorbsSoftWrappedContinuationLines() {
        val notes = """
            - The `piko` bundle is removed: it carried only the X hooks
              for `12.19.1-release.0`, which no longer match.
            - Docs updated.
        """.trimIndent()
        assertEquals(
            listOf(
                ChangelogBlock(
                    "The `piko` bundle is removed: it carried only the X hooks for `12.19.1-release.0`, which no longer match.",
                    bullet = true,
                ),
                ChangelogBlock("Docs updated.", bullet = true),
            ),
            changelogBlocks(notes),
        )
    }

    @Test
    fun blankLinesKeepBlocksSeparate() {
        assertEquals(
            listOf(
                ChangelogBlock("first paragraph wraps"),
                ChangelogBlock(""),
                ChangelogBlock("second paragraph"),
            ),
            changelogBlocks("first paragraph\nwraps\n\nsecond paragraph"),
        )
    }

    @Test
    fun headersNeverMergeWithFollowingLines() {
        assertEquals(
            listOf(
                ChangelogBlock("1.0.0", header = true),
                ChangelogBlock(""),
                ChangelogBlock("item", bullet = true),
            ),
            changelogBlocks("## 1.0.0\n\n- item"),
        )
    }
}
