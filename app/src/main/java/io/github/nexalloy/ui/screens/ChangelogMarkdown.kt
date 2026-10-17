package io.github.nexalloy.ui.screens

/**
 * One run of changelog text with the inline markdown styling that applies to it. [ChangelogText]
 * maps these runs onto an annotated string so release notes never show the raw markers.
 */
internal data class MarkdownSpan(
    val text: String,
    val code: Boolean = false,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val strikethrough: Boolean = false,
    val url: String? = null,
)

/**
 * Matches the inline markdown GitHub release notes use, in priority order: `code` spans,
 * [markdown](links), **bold**, __bold__, ~~strikethrough~~, *italic*, _italic_ and bare http(s)
 * URLs. Asterisk and underscore emphasis follow the GFM flanking rules loosely: the delimiters may
 * not hug word characters, so identifiers like `intent_aware_ad_pivot` stay plain.
 */
private val INLINE_MARKDOWN = Regex(
    "`([^`\\n]+)`" +
        "|\\[([^\\]\\n]+)]\\(([^)\\n]+)\\)" +
        "|\\*\\*([^*\\n]+)\\*\\*" +
        "|__([^_\\n]+)__" +
        "|~~([^~\\n]+)~~" +
        "|(?<![\\w*])\\*(?!\\s)([^*\\n]+?)(?<=\\S)\\*(?![\\w*])" +
        "|(?<![\\w_])_(?!\\s)([^_\\n]+?)(?<=\\S)_(?![\\w_])" +
        "|https?://[^\\s<>\\[\\]()]+",
)

/**
 * Splits one line of inline markdown into styled runs. Styled content is parsed recursively so a
 * code span inside bold keeps both, while code spans themselves are taken verbatim.
 */
internal fun parseMarkdownSpans(text: String): List<MarkdownSpan> =
    parseInlineMarkdown(text, MarkdownSpan(""))

private fun parseInlineMarkdown(text: String, base: MarkdownSpan): List<MarkdownSpan> {
    val spans = mutableListOf<MarkdownSpan>()
    var index = 0
    for (match in INLINE_MARKDOWN.findAll(text)) {
        if (match.range.first > index) {
            spans += base.copy(text = text.substring(index, match.range.first))
        }
        when {
            match.groups[1] != null -> spans += base.copy(text = match.groupValues[1], code = true)
            match.groups[2] != null -> spans += base.copy(text = match.groupValues[2], url = match.groupValues[3])
            match.groups[4] != null -> spans += parseInlineMarkdown(match.groupValues[4], base.copy(bold = true))
            match.groups[5] != null -> spans += parseInlineMarkdown(match.groupValues[5], base.copy(bold = true))
            match.groups[6] != null -> spans += parseInlineMarkdown(match.groupValues[6], base.copy(strikethrough = true))
            match.groups[7] != null -> spans += parseInlineMarkdown(match.groupValues[7], base.copy(italic = true))
            match.groups[8] != null -> spans += parseInlineMarkdown(match.groupValues[8], base.copy(italic = true))
            else -> {
                // Bare URL: keep sentence punctuation out of the link itself.
                val raw = match.value
                val url = raw.trimEnd { it in ".,;:!?" }
                spans += base.copy(text = url, url = url)
                if (url.length < raw.length) {
                    spans += base.copy(text = raw.substring(url.length))
                }
            }
        }
        index = match.range.last + 1
    }
    if (index < text.length) {
        spans += base.copy(text = text.substring(index))
    }
    return spans.filter { it.text.isNotEmpty() }
}
