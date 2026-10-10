package com.outlandishomar.acta.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.outlandishomar.acta.ui.theme.Blue500
import com.outlandishomar.acta.ui.theme.TextPrimary
import com.outlandishomar.acta.ui.theme.TextSecondary

object MarkdownHelper {

    fun applyHeading(value: TextFieldValue): TextFieldValue {
        return transformLines(value) { _, line ->
            when {
                line.startsWith("### ") -> line.removePrefix("### ")
                line.startsWith("## ") -> "### " + line.removePrefix("## ")
                line.startsWith("# ") -> "## " + line.removePrefix("# ")
                else -> "# " + line.removePrefix("• ").removePrefix("- ").replace(Regex("^\\d+\\.\\s*"), "")
            }
        }
    }

    fun applyBold(value: TextFieldValue): TextFieldValue {
        return wrapOrInsert(value, "**", "**")
    }

    fun applyItalic(value: TextFieldValue): TextFieldValue {
        return wrapOrInsert(value, "*", "*")
    }

    fun applyUnderline(value: TextFieldValue): TextFieldValue {
        return wrapOrInsert(value, "<u>", "</u>")
    }

    fun applyStrikethrough(value: TextFieldValue): TextFieldValue {
        return wrapOrInsert(value, "~~", "~~")
    }

    fun applyHighlight(value: TextFieldValue): TextFieldValue {
        return wrapOrInsert(value, "==", "==")
    }

    fun applyBulletList(value: TextFieldValue): TextFieldValue {
        return transformLines(value) { _, line ->
            when {
                line.startsWith("• ") -> line.removePrefix("• ")
                line.startsWith("- ") -> line.removePrefix("- ")
                else -> "• " + line.removePrefix("# ").removePrefix("## ").removePrefix("### ").replace(Regex("^\\d+\\.\\s*"), "")
            }
        }
    }

    fun applyNumberedList(value: TextFieldValue): TextFieldValue {
        return transformLines(value) { index, line ->
            val cleaned = line.removePrefix("• ").removePrefix("- ").removePrefix("# ").removePrefix("## ").removePrefix("### ").replace(Regex("^\\d+\\.\\s*"), "")
            if (line.matches(Regex("^\\d+\\.\\s*.*"))) {
                cleaned
            } else {
                "${index + 1}. $cleaned"
            }
        }
    }

    fun applyIndent(value: TextFieldValue): TextFieldValue {
        return transformLines(value) { _, line ->
            "    $line"
        }
    }

    fun applyOutdent(value: TextFieldValue): TextFieldValue {
        return transformLines(value) { _, line ->
            when {
                line.startsWith("    ") -> line.removePrefix("    ")
                line.startsWith("\t") -> line.removePrefix("\t")
                line.startsWith("  ") -> line.removePrefix("  ")
                line.startsWith("• ") -> line.removePrefix("• ")
                line.startsWith("- ") -> line.removePrefix("- ")
                line.matches(Regex("^\\d+\\.\\s*.*")) -> line.replace(Regex("^\\d+\\.\\s*"), "")
                else -> line
            }
        }
    }

    private fun wrapOrInsert(value: TextFieldValue, prefix: String, suffix: String): TextFieldValue {
        val text = value.text
        val selection = value.selection
        if (selection.collapsed) {
            val insertPos = selection.start.coerceIn(0, text.length)
            val newText = text.substring(0, insertPos) + prefix + suffix + text.substring(insertPos)
            val newCursor = insertPos + prefix.length
            return value.copy(
                text = newText,
                selection = TextRange(newCursor)
            )
        } else {
            val start = minOf(selection.start, selection.end).coerceIn(0, text.length)
            val end = maxOf(selection.start, selection.end).coerceIn(0, text.length)
            val selectedText = text.substring(start, end)

            // If selected text is already wrapped inside selection, unwrap it
            if (selectedText.startsWith(prefix) && selectedText.endsWith(suffix) && selectedText.length >= prefix.length + suffix.length) {
                val unwrapped = selectedText.substring(prefix.length, selectedText.length - suffix.length)
                val newText = text.substring(0, start) + unwrapped + text.substring(end)
                return value.copy(
                    text = newText,
                    selection = TextRange(start, start + unwrapped.length)
                )
            }

            // If selection is wrapped by prefix/suffix outside
            if (start >= prefix.length && end + suffix.length <= text.length &&
                text.substring(start - prefix.length, start) == prefix &&
                text.substring(end, end + suffix.length) == suffix
            ) {
                val newText = text.substring(0, start - prefix.length) + selectedText + text.substring(end + suffix.length)
                return value.copy(
                    text = newText,
                    selection = TextRange(start - prefix.length, start - prefix.length + selectedText.length)
                )
            }

            // Wrap selection
            val newText = text.substring(0, start) + prefix + selectedText + suffix + text.substring(end)
            return value.copy(
                text = newText,
                selection = TextRange(start, end + prefix.length + suffix.length)
            )
        }
    }

    private fun transformLines(
        value: TextFieldValue,
        transformer: (lineIndex: Int, lineText: String) -> String
    ): TextFieldValue {
        val text = value.text
        val selection = value.selection
        val start = minOf(selection.start, selection.end).coerceIn(0, text.length)
        val end = maxOf(selection.start, selection.end).coerceIn(0, text.length)

        val lineStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', end).let { if (it == -1) text.length else it }

        val beforeBlock = text.substring(0, lineStart)
        val targetBlock = text.substring(lineStart, lineEnd)
        val afterBlock = text.substring(lineEnd)

        val lines = targetBlock.split('\n')
        val transformedLines = lines.mapIndexed { idx, line -> transformer(idx, line) }
        val newBlock = transformedLines.joinToString("\n")

        val newText = beforeBlock + newBlock + afterBlock
        val lengthDiff = newBlock.length - targetBlock.length

        val newStart = if (selection.collapsed) {
            (selection.start + lengthDiff).coerceIn(0, newText.length)
        } else {
            lineStart
        }
        val newEnd = if (selection.collapsed) {
            newStart
        } else {
            (lineStart + newBlock.length).coerceIn(0, newText.length)
        }

        return value.copy(
            text = newText,
            selection = TextRange(newStart, newEnd)
        )
    }

    fun renderMarkdownToAnnotatedString(raw: String): AnnotatedString {
        val lines = raw.split('\n')
        return buildAnnotatedString {
            lines.forEachIndexed { index, line ->
                if (index > 0) append("\n")
                when {
                    line.startsWith("### ") -> {
                        val content = line.removePrefix("### ")
                        val start = length
                        append(content)
                        addStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, color = TextPrimary), start, length)
                    }
                    line.startsWith("## ") -> {
                        val content = line.removePrefix("## ")
                        val start = length
                        append(content)
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 19.sp, color = TextPrimary), start, length)
                    }
                    line.startsWith("# ") -> {
                        val content = line.removePrefix("# ")
                        val start = length
                        append(content)
                        addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 21.sp, color = TextPrimary), start, length)
                    }
                    else -> {
                        appendInlineFormatted(line)
                    }
                }
            }
        }
    }

    private fun AnnotatedString.Builder.appendInlineFormatted(text: String) {
        var cursor = 0
        val tokenRegex = Regex("(\\*\\*.*?\\*\\*|\\*.*?\\*|~~.*?~~|<u>.*?</u>|_.*?_|==.*?==)")
        val matches = tokenRegex.findAll(text).toList()

        for (match in matches) {
            if (match.range.first > cursor) {
                append(text.substring(cursor, match.range.first))
            }
            val matchedStr = match.value
            when {
                matchedStr.startsWith("**") && matchedStr.endsWith("**") && matchedStr.length >= 4 -> {
                    val inner = matchedStr.substring(2, matchedStr.length - 2)
                    val start = length
                    append(inner)
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary), start, length)
                }
                matchedStr.startsWith("*") && matchedStr.endsWith("*") && matchedStr.length >= 2 -> {
                    val inner = matchedStr.substring(1, matchedStr.length - 1)
                    val start = length
                    append(inner)
                    addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
                }
                matchedStr.startsWith("~~") && matchedStr.endsWith("~~") && matchedStr.length >= 4 -> {
                    val inner = matchedStr.substring(2, matchedStr.length - 2)
                    val start = length
                    append(inner)
                    addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), start, length)
                }
                matchedStr.startsWith("<u>") && matchedStr.endsWith("</u>") && matchedStr.length >= 7 -> {
                    val inner = matchedStr.substring(3, matchedStr.length - 4)
                    val start = length
                    append(inner)
                    addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, length)
                }
                matchedStr.startsWith("_") && matchedStr.endsWith("_") && matchedStr.length >= 2 -> {
                    val inner = matchedStr.substring(1, matchedStr.length - 1)
                    val start = length
                    append(inner)
                    addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, length)
                }
                matchedStr.startsWith("==") && matchedStr.endsWith("==") && matchedStr.length >= 4 -> {
                    val inner = matchedStr.substring(2, matchedStr.length - 2)
                    val start = length
                    append(inner)
                    addStyle(SpanStyle(background = Color(0x66FFD600), fontWeight = FontWeight.Medium), start, length)
                }
                else -> append(matchedStr)
            }
            cursor = match.range.last + 1
        }
        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}

class MarkdownVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val builder = AnnotatedString.Builder(raw)

        val lines = raw.split('\n')
        var currentOffset = 0
        for (line in lines) {
            val lineLength = line.length
            when {
                line.startsWith("### ") -> {
                    builder.addStyle(
                        SpanStyle(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = TextPrimary
                        ),
                        currentOffset,
                        currentOffset + lineLength
                    )
                    builder.addStyle(
                        SpanStyle(color = TextSecondary.copy(alpha = 0.4f)),
                        currentOffset,
                        currentOffset + 4
                    )
                }
                line.startsWith("## ") -> {
                    builder.addStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = TextPrimary
                        ),
                        currentOffset,
                        currentOffset + lineLength
                    )
                    builder.addStyle(
                        SpanStyle(color = TextSecondary.copy(alpha = 0.4f)),
                        currentOffset,
                        currentOffset + 3
                    )
                }
                line.startsWith("# ") -> {
                    builder.addStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = TextPrimary
                        ),
                        currentOffset,
                        currentOffset + lineLength
                    )
                    builder.addStyle(
                        SpanStyle(color = TextSecondary.copy(alpha = 0.4f)),
                        currentOffset,
                        currentOffset + 2
                    )
                }
                line.startsWith("• ") || line.startsWith("- ") -> {
                    builder.addStyle(
                        SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = Blue500
                        ),
                        currentOffset,
                        currentOffset + 2
                    )
                }
                line.matches(Regex("^\\d+\\.\\s*.*")) -> {
                    val match = Regex("^\\d+\\.\\s*").find(line)
                    if (match != null) {
                        builder.addStyle(
                            SpanStyle(
                                fontWeight = FontWeight.Bold,
                                color = Blue500
                            ),
                            currentOffset,
                            currentOffset + match.value.length
                        )
                    }
                }
            }
            currentOffset += lineLength + 1
        }

        // Bold: **text**
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
        boldRegex.findAll(raw).forEach { match ->
            builder.addStyle(
                SpanStyle(fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1
            )
        }

        // Italic: *text* (avoiding **)
        val italicRegex = Regex("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)")
        italicRegex.findAll(raw).forEach { match ->
            builder.addStyle(
                SpanStyle(fontStyle = FontStyle.Italic),
                match.range.first,
                match.range.last + 1
            )
        }

        // Strikethrough: ~~text~~
        val strikeRegex = Regex("~~(.*?)~~")
        strikeRegex.findAll(raw).forEach { match ->
            builder.addStyle(
                SpanStyle(textDecoration = TextDecoration.LineThrough),
                match.range.first,
                match.range.last + 1
            )
        }

        // Underline: <u>text</u> or _text_
        val underlineRegex = Regex("<u>(.*?)</u>")
        underlineRegex.findAll(raw).forEach { match ->
            builder.addStyle(
                SpanStyle(textDecoration = TextDecoration.Underline),
                match.range.first,
                match.range.last + 1
            )
        }
        val underscoreRegex = Regex("(?<!_)_(?!_)(.*?)(?<!_)_(?!_)")
        underscoreRegex.findAll(raw).forEach { match ->
            builder.addStyle(
                SpanStyle(textDecoration = TextDecoration.Underline),
                match.range.first,
                match.range.last + 1
            )
        }

        // Highlight: ==text==
        val highlightRegex = Regex("==(.*?)==")
        highlightRegex.findAll(raw).forEach { match ->
            builder.addStyle(
                SpanStyle(
                    background = Color(0x66FFD600),
                    fontWeight = FontWeight.Medium
                ),
                match.range.first,
                match.range.last + 1
            )
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}
