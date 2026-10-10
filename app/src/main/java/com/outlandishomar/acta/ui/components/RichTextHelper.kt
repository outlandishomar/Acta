package com.outlandishomar.acta.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
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
import java.util.regex.Pattern

data class RichSpan(
    val start: Int,
    val end: Int,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderline: Boolean = false,
    val isStrikethrough: Boolean = false,
    val headingLevel: Int = 0, // 0 = normal, 1 = H1, 2 = H2, 3 = H3
    val highlightColor: Long? = null // Color.value as Long
)

object RichTextHelper {

    val HighlightColors = listOf(
        Color(0xFFFFD600), // Yellow
        Color(0xFFFF8A65), // Coral / Orange
        Color(0xFF69F0AE), // Green
        Color(0xFF40C4FF), // Cyan
        Color(0xFF7C4DFF), // Blue / Purple
        Color(0xFFFF5252), // Red
        Color(0xFFE040FB)  // Magenta
    )

    fun parseToRichSpans(raw: String): Pair<String, List<RichSpan>> {
        if (raw.isBlank()) return Pair("", emptyList())

        val hasHtml = raw.contains("<") && raw.contains(">")
        if (hasHtml) {
            return parseHtmlToRichSpans(raw)
        }

        val hasMarkdown = raw.contains("**") || raw.contains("~~") || raw.contains("==") ||
                raw.contains("<u>") || raw.startsWith("# ") || raw.contains("\n# ")

        if (hasMarkdown) {
            return parseMarkdownToRichSpans(raw)
        }

        return Pair(raw, emptyList())
    }

    private fun parseHtmlToRichSpans(html: String): Pair<String, List<RichSpan>> {
        val spans = mutableListOf<RichSpan>()
        val plainTextBuilder = StringBuilder()

        val normalized = html.replace("<br>", "\n").replace("<br/>", "\n").replace("<br />", "\n")

        val tagRegex = Pattern.compile("(<[^>]+>|[^<]+)")
        val matcher = tagRegex.matcher(normalized)

        var isBold = false
        var isItalic = false
        var isUnderline = false
        var isStrikethrough = false
        var currentHighlight: Long? = null
        var currentHeading = 0

        while (matcher.find()) {
            val token = matcher.group()
            if (token.startsWith("<")) {
                val tagLower = token.lowercase().trim()
                when {
                    tagLower == "<b>" || tagLower == "<strong>" -> isBold = true
                    tagLower == "</b>" || tagLower == "</strong>" -> isBold = false
                    tagLower == "<i>" || tagLower == "<em>" -> isItalic = true
                    tagLower == "</i>" || tagLower == "</em>" -> isItalic = false
                    tagLower == "<u>" -> isUnderline = true
                    tagLower == "</u>" -> isUnderline = false
                    tagLower == "<s>" || tagLower == "<del>" || tagLower == "<strike>" -> isStrikethrough = true
                    tagLower == "</s>" || tagLower == "</del>" || tagLower == "</strike>" -> isStrikethrough = false
                    tagLower.startsWith("<h1") -> currentHeading = 1
                    tagLower == "</h1>" -> currentHeading = 0
                    tagLower.startsWith("<h2") -> currentHeading = 2
                    tagLower == "</h2>" -> currentHeading = 0
                    tagLower.startsWith("<h3") -> currentHeading = 3
                    tagLower == "</h3>" -> currentHeading = 0
                    tagLower.startsWith("<mark") -> {
                        val colorMatch = Regex("background(?:-color)?:\\s*(#[0-9a-fA-F]{3,8})").find(token)
                        if (colorMatch != null) {
                            try {
                                val hex = colorMatch.groupValues[1].trim()
                                if (hex.equals("#000000", ignoreCase = true) || hex.equals("#000", ignoreCase = true)) {
                                    // Fix for previous buggy black highlight
                                    currentHighlight = Color(0xFFFFD600).value.toLong()
                                } else {
                                    val parsed = android.graphics.Color.parseColor(hex)
                                    currentHighlight = Color(parsed).value.toLong()
                                }
                            } catch (e: Exception) {
                                currentHighlight = Color(0xFFFFD600).value.toLong()
                            }
                        } else {
                            currentHighlight = Color(0xFFFFD600).value.toLong()
                        }
                    }
                    tagLower == "</mark>" -> currentHighlight = null
                    tagLower == "<p>" || tagLower == "<div>" -> {
                        if (plainTextBuilder.isNotEmpty() && !plainTextBuilder.endsWith("\n")) {
                            plainTextBuilder.append("\n")
                        }
                    }
                    tagLower == "</p>" || tagLower == "</div>" -> {
                        if (!plainTextBuilder.endsWith("\n")) {
                            plainTextBuilder.append("\n")
                        }
                    }
                }
            } else {
                val start = plainTextBuilder.length
                plainTextBuilder.append(token)
                val end = plainTextBuilder.length

                if (isBold || isItalic || isUnderline || isStrikethrough || currentHighlight != null || currentHeading > 0) {
                    spans.add(
                        RichSpan(
                            start = start,
                            end = end,
                            isBold = isBold,
                            isItalic = isItalic,
                            isUnderline = isUnderline,
                            isStrikethrough = isStrikethrough,
                            headingLevel = currentHeading,
                            highlightColor = currentHighlight
                        )
                    )
                }
            }
        }

        return Pair(plainTextBuilder.toString(), mergeSpans(spans))
    }

    private fun parseMarkdownToRichSpans(raw: String): Pair<String, List<RichSpan>> {
        val lines = raw.split('\n')
        val plainTextBuilder = StringBuilder()
        val spans = mutableListOf<RichSpan>()

        lines.forEachIndexed { index, line ->
            if (index > 0) plainTextBuilder.append("\n")

            var heading = 0
            var content = line
            when {
                line.startsWith("### ") -> { heading = 3; content = line.removePrefix("### ") }
                line.startsWith("## ") -> { heading = 2; content = line.removePrefix("## ") }
                line.startsWith("# ") -> { heading = 1; content = line.removePrefix("# ") }
            }

            val lineStart = plainTextBuilder.length
            var cursor = 0
            val tokenRegex = Regex("(\\*\\*.*?\\*\\*|\\*.*?\\*|~~.*?~~|<u>.*?</u>|_.*?_|==.*?==)")
            val matches = tokenRegex.findAll(content).toList()

            for (match in matches) {
                if (match.range.first > cursor) {
                    plainTextBuilder.append(content.substring(cursor, match.range.first))
                }
                val matchedStr = match.value
                val s = plainTextBuilder.length
                when {
                    matchedStr.startsWith("**") && matchedStr.endsWith("**") && matchedStr.length >= 4 -> {
                        plainTextBuilder.append(matchedStr.substring(2, matchedStr.length - 2))
                        spans.add(RichSpan(s, plainTextBuilder.length, isBold = true, headingLevel = heading))
                    }
                    matchedStr.startsWith("*") && matchedStr.endsWith("*") && matchedStr.length >= 2 -> {
                        plainTextBuilder.append(matchedStr.substring(1, matchedStr.length - 1))
                        spans.add(RichSpan(s, plainTextBuilder.length, isItalic = true, headingLevel = heading))
                    }
                    matchedStr.startsWith("~~") && matchedStr.endsWith("~~") && matchedStr.length >= 4 -> {
                        plainTextBuilder.append(matchedStr.substring(2, matchedStr.length - 2))
                        spans.add(RichSpan(s, plainTextBuilder.length, isStrikethrough = true, headingLevel = heading))
                    }
                    matchedStr.startsWith("<u>") && matchedStr.endsWith("</u>") && matchedStr.length >= 7 -> {
                        plainTextBuilder.append(matchedStr.substring(3, matchedStr.length - 4))
                        spans.add(RichSpan(s, plainTextBuilder.length, isUnderline = true, headingLevel = heading))
                    }
                    matchedStr.startsWith("_") && matchedStr.endsWith("_") && matchedStr.length >= 2 -> {
                        plainTextBuilder.append(matchedStr.substring(1, matchedStr.length - 1))
                        spans.add(RichSpan(s, plainTextBuilder.length, isUnderline = true, headingLevel = heading))
                    }
                    matchedStr.startsWith("==") && matchedStr.endsWith("==") && matchedStr.length >= 4 -> {
                        plainTextBuilder.append(matchedStr.substring(2, matchedStr.length - 2))
                        spans.add(RichSpan(s, plainTextBuilder.length, highlightColor = Color(0xFFFFD600).value.toLong(), headingLevel = heading))
                    }
                    else -> plainTextBuilder.append(matchedStr)
                }
                cursor = match.range.last + 1
            }
            if (cursor < content.length) {
                val s = plainTextBuilder.length
                plainTextBuilder.append(content.substring(cursor))
                if (heading > 0) {
                    spans.add(RichSpan(s, plainTextBuilder.length, headingLevel = heading))
                }
            } else if (heading > 0 && matches.isEmpty()) {
                spans.add(RichSpan(lineStart, plainTextBuilder.length, headingLevel = heading))
            }
        }

        return Pair(plainTextBuilder.toString(), mergeSpans(spans))
    }

    fun mergeSpans(spans: List<RichSpan>): List<RichSpan> {
        val valid = spans.filter { it.start < it.end }.sortedWith(compareBy({ it.start }, { it.end }))
        if (valid.isEmpty()) return emptyList()

        val result = mutableListOf<RichSpan>()
        var current = valid.first()

        for (i in 1 until valid.size) {
            val next = valid[i]
            if (current.end == next.start &&
                current.isBold == next.isBold &&
                current.isItalic == next.isItalic &&
                current.isUnderline == next.isUnderline &&
                current.isStrikethrough == next.isStrikethrough &&
                current.headingLevel == next.headingLevel &&
                current.highlightColor == next.highlightColor
            ) {
                current = current.copy(end = next.end)
            } else {
                result.add(current)
                current = next
            }
        }
        result.add(current)
        return result
    }

    fun serializeToHtml(text: String, spans: List<RichSpan>): String {
        if (text.isEmpty()) return ""
        if (spans.isEmpty()) return text

        val lines = text.split('\n')
        val sb = StringBuilder()
        var offset = 0

        lines.forEachIndexed { index, line ->
            if (index > 0) sb.append("\n")
            val lineStart = offset
            val lineEnd = offset + line.length

            val lineSpans = spans.filter { it.start < lineEnd && it.end > lineStart }
            val lineHeading = lineSpans.map { it.headingLevel }.filter { it > 0 }.maxOrNull() ?: 0

            if (lineHeading > 0) {
                sb.append("<h$lineHeading>")
            }

            var localCursor = 0
            val lineLen = line.length

            while (localCursor < lineLen) {
                val globalPos = lineStart + localCursor
                val activeSpans = lineSpans.filter { it.start <= globalPos && it.end > globalPos }

                val isBold = activeSpans.any { it.isBold }
                val isItalic = activeSpans.any { it.isItalic }
                val isUnderline = activeSpans.any { it.isUnderline }
                val isStrikethrough = activeSpans.any { it.isStrikethrough }
                val highlight = activeSpans.mapNotNull { it.highlightColor }.firstOrNull()

                var nextChange = lineLen
                for (span in lineSpans) {
                    val localStart = (span.start - lineStart).coerceIn(0, lineLen)
                    val localEnd = (span.end - lineStart).coerceIn(0, lineLen)
                    if (localStart > localCursor && localStart < nextChange) nextChange = localStart
                    if (localEnd > localCursor && localEnd < nextChange) nextChange = localEnd
                }

                val segment = line.substring(localCursor, nextChange)
                var formatted = segment

                if (isBold) formatted = "<b>$formatted</b>"
                if (isItalic) formatted = "<i>$formatted</i>"
                if (isUnderline) formatted = "<u>$formatted</u>"
                if (isStrikethrough) formatted = "<s>$formatted</s>"
                if (highlight != null) {
                    val color = Color(highlight.toULong())
                    val r = (color.red * 255f).toInt().coerceIn(0, 255)
                    val g = (color.green * 255f).toInt().coerceIn(0, 255)
                    val b = (color.blue * 255f).toInt().coerceIn(0, 255)
                    val hex = String.format("#%02X%02X%02X", r, g, b)
                    formatted = "<mark style=\"background:$hex\">$formatted</mark>"
                }

                sb.append(formatted)
                localCursor = nextChange
            }

            if (lineHeading > 0) {
                sb.append("</h$lineHeading>")
            }

            offset += lineLen + 1
        }

        return sb.toString()
    }

    fun buildVisualAnnotatedString(text: String, spans: List<RichSpan>): AnnotatedString {
        val builder = AnnotatedString.Builder(text)
        val lines = text.split('\n')
        var offset = 0

        // Format bullet and numbered prefixes
        for (line in lines) {
            val lineLen = line.length
            when {
                line.startsWith("• ") -> {
                    builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Blue500), offset, offset + 2)
                }
                line.matches(Regex("^\\d+\\.\\s*.*")) -> {
                    val match = Regex("^\\d+\\.\\s*").find(line)
                    if (match != null) {
                        builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Blue500), offset, offset + match.value.length)
                    }
                }
            }
            offset += lineLen + 1
        }

        // Apply spans
        for (span in spans) {
            val s = span.start.coerceIn(0, text.length)
            val e = span.end.coerceIn(0, text.length)
            if (s < e) {
                var spanStyle = SpanStyle()
                if (span.isBold) spanStyle = spanStyle.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                if (span.isItalic) spanStyle = spanStyle.copy(fontStyle = FontStyle.Italic)
                if (span.isUnderline && span.isStrikethrough) {
                    spanStyle = spanStyle.copy(textDecoration = TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough)))
                } else if (span.isUnderline) {
                    spanStyle = spanStyle.copy(textDecoration = TextDecoration.Underline)
                } else if (span.isStrikethrough) {
                    spanStyle = spanStyle.copy(textDecoration = TextDecoration.LineThrough)
                }
                if (span.highlightColor != null) {
                    spanStyle = spanStyle.copy(background = Color(span.highlightColor.toULong()), fontWeight = FontWeight.Medium)
                }
                if (span.headingLevel == 1) {
                    spanStyle = spanStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                } else if (span.headingLevel == 2) {
                    spanStyle = spanStyle.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                } else if (span.headingLevel == 3) {
                    spanStyle = spanStyle.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                }

                builder.addStyle(spanStyle, s, e)
            }
        }

        return builder.toAnnotatedString()
    }

    fun renderRichText(raw: String): AnnotatedString {
        val (text, spans) = parseToRichSpans(raw)
        return buildVisualAnnotatedString(text, spans)
    }

    fun renderForTaskSheet(raw: String): AnnotatedString {
        return renderRichText(raw)
    }

    fun adjustSpansForInsertion(
        spans: List<RichSpan>,
        insertPos: Int,
        count: Int,
        isBold: Boolean,
        isItalic: Boolean,
        isUnderline: Boolean,
        isStrikethrough: Boolean,
        highlight: Color?,
        heading: Int
    ): List<RichSpan> {
        val result = mutableListOf<RichSpan>()
        val newSpan = if (isBold || isItalic || isUnderline || isStrikethrough || highlight != null || heading > 0) {
            RichSpan(
                start = insertPos,
                end = insertPos + count,
                isBold = isBold,
                isItalic = isItalic,
                isUnderline = isUnderline,
                isStrikethrough = isStrikethrough,
                headingLevel = heading,
                highlightColor = highlight?.let { it.value.toLong() }
            )
        } else null

        for (span in spans) {
            when {
                span.end <= insertPos -> result.add(span)
                span.start >= insertPos -> result.add(span.copy(start = span.start + count, end = span.end + count))
                else -> {
                    result.add(span.copy(end = span.end + count))
                }
            }
        }
        if (newSpan != null) {
            result.add(newSpan)
        }
        return mergeSpans(result)
    }

    fun adjustSpansForDeletion(
        spans: List<RichSpan>,
        deletePos: Int,
        count: Int
    ): List<RichSpan> {
        val deleteEnd = deletePos + count
        val result = mutableListOf<RichSpan>()
        for (span in spans) {
            when {
                span.end <= deletePos -> result.add(span)
                span.start >= deleteEnd -> result.add(span.copy(start = span.start - count, end = span.end - count))
                span.start >= deletePos && span.end <= deleteEnd -> {
                    // Entire span deleted
                }
                span.start < deletePos && span.end > deleteEnd -> {
                    result.add(span.copy(end = span.end - count))
                }
                span.start < deletePos && span.end <= deleteEnd -> {
                    result.add(span.copy(end = deletePos))
                }
                span.start >= deletePos && span.start < deleteEnd && span.end > deleteEnd -> {
                    val newStart = deletePos
                    val newEnd = span.end - count
                    if (newStart < newEnd) {
                        result.add(span.copy(start = newStart, end = newEnd))
                    }
                }
            }
        }
        return mergeSpans(result)
    }

    fun applyStyleToRange(
        spans: List<RichSpan>,
        rangeStart: Int,
        rangeEnd: Int,
        transform: (RichSpan) -> RichSpan
    ): List<RichSpan> {
        val result = mutableListOf<RichSpan>()
        val dummy = RichSpan(rangeStart, rangeEnd)
        result.add(transform(dummy))

        for (span in spans) {
            if (span.end <= rangeStart || span.start >= rangeEnd) {
                result.add(span)
            } else {
                if (span.start < rangeStart) {
                    result.add(span.copy(end = rangeStart))
                }
                if (span.end > rangeEnd) {
                    result.add(span.copy(start = rangeEnd))
                }
                val overlapStart = maxOf(span.start, rangeStart)
                val overlapEnd = minOf(span.end, rangeEnd)
                result.add(transform(span.copy(start = overlapStart, end = overlapEnd)))
            }
        }
        return mergeSpans(result)
    }

    fun toggleBold(textFieldValue: TextFieldValue, spans: List<RichSpan>, isBoldActive: Boolean): Pair<List<RichSpan>, Boolean> {
        val sel = textFieldValue.selection
        if (sel.collapsed) return Pair(spans, !isBoldActive)
        val start = sel.min
        val end = sel.max
        val already = spans.any { it.start <= start && it.end >= end && it.isBold }
        val newBold = !already
        val updated = applyStyleToRange(spans, start, end) { it.copy(isBold = newBold) }
        return Pair(updated, newBold)
    }

    fun toggleItalic(textFieldValue: TextFieldValue, spans: List<RichSpan>, isItalicActive: Boolean): Pair<List<RichSpan>, Boolean> {
        val sel = textFieldValue.selection
        if (sel.collapsed) return Pair(spans, !isItalicActive)
        val start = sel.min
        val end = sel.max
        val already = spans.any { it.start <= start && it.end >= end && it.isItalic }
        val newItalic = !already
        val updated = applyStyleToRange(spans, start, end) { it.copy(isItalic = newItalic) }
        return Pair(updated, newItalic)
    }

    fun toggleUnderline(textFieldValue: TextFieldValue, spans: List<RichSpan>, isUnderlineActive: Boolean): Pair<List<RichSpan>, Boolean> {
        val sel = textFieldValue.selection
        if (sel.collapsed) return Pair(spans, !isUnderlineActive)
        val start = sel.min
        val end = sel.max
        val already = spans.any { it.start <= start && it.end >= end && it.isUnderline }
        val newUnderline = !already
        val updated = applyStyleToRange(spans, start, end) { it.copy(isUnderline = newUnderline) }
        return Pair(updated, newUnderline)
    }

    fun toggleStrikethrough(textFieldValue: TextFieldValue, spans: List<RichSpan>, isStrikethroughActive: Boolean): Pair<List<RichSpan>, Boolean> {
        val sel = textFieldValue.selection
        if (sel.collapsed) return Pair(spans, !isStrikethroughActive)
        val start = sel.min
        val end = sel.max
        val already = spans.any { it.start <= start && it.end >= end && it.isStrikethrough }
        val newStrike = !already
        val updated = applyStyleToRange(spans, start, end) { it.copy(isStrikethrough = newStrike) }
        return Pair(updated, newStrike)
    }

    fun applyHighlightColor(textFieldValue: TextFieldValue, spans: List<RichSpan>, color: Color?): List<RichSpan> {
        val sel = textFieldValue.selection
        if (sel.collapsed) return spans
        val start = sel.min
        val end = sel.max
        val colorValue = color?.let { it.value.toLong() }
        return applyStyleToRange(spans, start, end) { it.copy(highlightColor = colorValue) }
    }

    fun applyHeadingToLine(textFieldValue: TextFieldValue, spans: List<RichSpan>, level: Int): List<RichSpan> {
        val text = textFieldValue.text
        val sel = textFieldValue.selection
        val start = sel.min
        val lineStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', start).let { if (it == -1) text.length else it }
        if (lineStart >= lineEnd) return spans
        return applyStyleToRange(spans, lineStart, lineEnd) { it.copy(headingLevel = level) }
    }

    fun toggleBulletList(textFieldValue: TextFieldValue, spans: List<RichSpan>): Pair<TextFieldValue, List<RichSpan>> {
        val text = textFieldValue.text
        val sel = textFieldValue.selection
        val start = sel.min
        val lineStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', start).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)

        val (newLine, cursorOffset) = when {
            line.startsWith("• ") -> Pair(line.removePrefix("• "), -2)
            line.matches(Regex("^\\d+\\.\\s*.*")) -> {
                val prefix = Regex("^\\d+\\.\\s*").find(line)!!.value
                Pair("• " + line.removePrefix(prefix), 2 - prefix.length)
            }
            else -> Pair("• $line", 2)
        }

        val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        val diff = newLine.length - line.length
        val updatedSpans = if (diff > 0) {
            adjustSpansForInsertion(spans, lineStart, diff, false, false, false, false, null, 0)
        } else if (diff < 0) {
            adjustSpansForDeletion(spans, lineStart, -diff)
        } else spans

        val newCursor = (sel.start + cursorOffset).coerceIn(0, newText.length)
        return Pair(TextFieldValue(newText, TextRange(newCursor)), updatedSpans)
    }

    fun toggleNumberedList(textFieldValue: TextFieldValue, spans: List<RichSpan>): Pair<TextFieldValue, List<RichSpan>> {
        val text = textFieldValue.text
        val sel = textFieldValue.selection
        val start = sel.min
        val lineStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', start).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)

        val (newLine, cursorOffset) = when {
            line.matches(Regex("^\\d+\\.\\s*.*")) -> {
                val prefix = Regex("^\\d+\\.\\s*").find(line)!!.value
                Pair(line.removePrefix(prefix), -prefix.length)
            }
            line.startsWith("• ") -> {
                Pair("1. " + line.removePrefix("• "), 3 - 2)
            }
            else -> Pair("1. $line", 3)
        }

        val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        val diff = newLine.length - line.length
        val updatedSpans = if (diff > 0) {
            adjustSpansForInsertion(spans, lineStart, diff, false, false, false, false, null, 0)
        } else if (diff < 0) {
            adjustSpansForDeletion(spans, lineStart, -diff)
        } else spans

        val newCursor = (sel.start + cursorOffset).coerceIn(0, newText.length)
        return Pair(TextFieldValue(newText, TextRange(newCursor)), updatedSpans)
    }

    fun indentLine(textFieldValue: TextFieldValue, spans: List<RichSpan>): Pair<TextFieldValue, List<RichSpan>> {
        val text = textFieldValue.text
        val sel = textFieldValue.selection
        val start = sel.min
        val lineStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val newText = text.substring(0, lineStart) + "    " + text.substring(lineStart)
        val updatedSpans = adjustSpansForInsertion(spans, lineStart, 4, false, false, false, false, null, 0)
        return Pair(TextFieldValue(newText, TextRange(sel.start + 4)), updatedSpans)
    }

    fun outdentLine(textFieldValue: TextFieldValue, spans: List<RichSpan>): Pair<TextFieldValue, List<RichSpan>> {
        val text = textFieldValue.text
        val sel = textFieldValue.selection
        val start = sel.min
        val lineStart = text.lastIndexOf('\n', (start - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', start).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)

        val removeCount = when {
            line.startsWith("    ") -> 4
            line.startsWith("  ") -> 2
            line.startsWith("\t") -> 1
            line.startsWith("• ") -> 2
            line.matches(Regex("^\\d+\\.\\s*.*")) -> Regex("^\\d+\\.\\s*").find(line)!!.value.length
            else -> 0
        }
        if (removeCount == 0) return Pair(textFieldValue, spans)

        val newText = text.substring(0, lineStart) + line.substring(removeCount) + text.substring(lineEnd)
        val updatedSpans = adjustSpansForDeletion(spans, lineStart, removeCount)
        return Pair(TextFieldValue(newText, TextRange((sel.start - removeCount).coerceAtLeast(lineStart))), updatedSpans)
    }
}

class RichVisualTransformation(private val spansProvider: () -> List<RichSpan>) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val annotated = RichTextHelper.buildVisualAnnotatedString(raw, spansProvider())
        return TransformedText(annotated, OffsetMapping.Identity)
    }
}
