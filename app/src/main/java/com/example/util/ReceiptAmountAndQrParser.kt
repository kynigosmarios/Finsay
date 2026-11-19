package com.example.util

import com.google.mlkit.vision.text.Text
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.regex.Pattern

internal data class InternalQrData(
    val amount: Double = 0.0,
    val merchantAfm: String = "",
    val timestamp: Long = 0L
)

/**
 * Extracted helper responsible for receipt amount detection (OCR + spatial bounding boxes),
 * AADE QR parsing, AFM extraction, and receipt date parsing.
 */
internal object ReceiptAmountAndQrParser {

    private fun removeGreekAccents(text: String): String {
        return text
            .replace('ά', 'α').replace('έ', 'ε').replace('ή', 'η')
            .replace('ί', 'ι').replace('ό', 'ο').replace('ύ', 'υ').replace('ώ', 'ω')
            .replace('Ά', 'Α').replace('Έ', 'Ε').replace('Ή', 'Η')
            .replace('Ί', 'Ι').replace('Ό', 'Ο').replace('Ύ', 'Υ').replace('Ώ', 'Ω')
            .replace('ΐ', 'ι').replace('ΰ', 'υ').replace('ü', 'u').replace('ú', 'u')
            .replace('ý', 'y')
    }

    private val SYNOLO_REGEX = Regex(
        """(?iu)(?:(?:ΓΕΝΙΚ[ΟO0οό]|ΓΕΝ\.?|ΤΕΛΙΚ[ΟO0οό]|ΠΛΗΡΩΤΕ[ΟO0οό]|ΚΑΘΑΡ[ΟO0οό]|ΣΥΝΟΛΙΚ[ΗΟA0ηοa]|TOTAL|GRAND)\s+)?(?:[ΣςσSZE3C£Fsze]\s*[ΥυύΎΰϋYUVyuvúüýI1\u038E\u03CD\u03CE\u03C5]\s*[ΝνNVMHnvumh\u03BD]\s*[ΟοόΌO0QDo0\u03BF\u03CC]\s*[ΛλLA^4/li1I|tdk\u03BB]\s*[ΟοόΌO0QDo0\u03BF\u03CC](?:[ΝνNvn\u03BD])?|(?:Σ[Υυ]νολικ[ήηόο]|Synolik[ho])\s*(?:Αξ[ίι]α|Ποσ[όο]|Axia|Poso)?|TOTAL|T\s*O\s*T\s*A\s*L|AMOUNT\s*DUE)"""
    )

    private val PRICE_REGEX = Regex(
        """([0-9]{1,4}(?:[.,][0-9]{3})*[.,][0-9]{2}|[0-9]+[.,][0-9]{2})"""
    )

    fun findSynoloMatchEnd(line: String): Int? {
        val normLine = removeGreekAccents(line).uppercase(Locale.ROOT)
        if (normLine.contains("ΕΞΟΙΚΟΝΟΜ") || normLine.contains("EXOIKONOM") ||
            normLine.contains("ΕΚΠΤΩΣ") || normLine.contains("EKPTWS") ||
            normLine.contains("ΟΦΕΛΟΣ") || normLine.contains("OFELOS") ||
            normLine.contains("ΚΕΡΔΟΣ") || normLine.contains("KERDOS") ||
            normLine.contains("SAVING") || normLine.contains("DISCOUNT") ||
            normLine.contains("POINTS") || normLine.contains("ΠΟΝΤ")
        ) {
            return null
        }

        val match = SYNOLO_REGEX.find(line)
        if (match != null) {
            val after = line.substring(match.range.last + 1).trim()
            val normAfter = removeGreekAccents(after).uppercase(Locale.ROOT)
            val isIgnoredModifier = normAfter.startsWith("ΤΕΜΑΧΙ") ||
                normAfter.startsWith("ΤΕΜ") ||
                normAfter.startsWith("TEMAXI") ||
                normAfter.startsWith("TEM") ||
                normAfter.startsWith("ΕΙΔΩΝ") ||
                normAfter.startsWith("EIDWN") ||
                normAfter.startsWith("ΠΟΣΟΤ") ||
                normAfter.startsWith("POSOT") ||
                normAfter.startsWith("ITEMS") ||
                normAfter.startsWith("QTY") ||
                normAfter.startsWith("PCS") ||
                normAfter.contains("ΕΞΟΙΚΟΝΟΜ") ||
                normAfter.contains("ΕΚΠΤΩΣ") ||
                normAfter.contains("ΟΦΕΛΟΣ") ||
                normAfter.contains("ΚΕΡΔΟΣ")
            if (isIgnoredModifier) return null
            return match.range.last + 1
        }

        val words = line.split(Regex("\\s+"))
        var currIdx = 0
        for (w in words) {
            val start = line.indexOf(w, currIdx)
            val unaccented = removeGreekAccents(w)
            val cleaned = unaccented.replace(Regex("[^a-zA-Zα-ωΑ-Ω0-9]"), "").uppercase(Locale.ROOT)
            if (cleaned.length in 5..8 && isFuzzySynolo(cleaned)) {
                val after = if (start >= 0) line.substring(start + w.length).trim() else ""
                val normAfter = removeGreekAccents(after).uppercase(Locale.ROOT)
                val isIgnoredModifier = normAfter.startsWith("ΤΕΜΑΧΙ") ||
                    normAfter.startsWith("ΤΕΜ") ||
                    normAfter.startsWith("TEMAXI") ||
                    normAfter.startsWith("TEM") ||
                    normAfter.startsWith("ΕΙΔΩΝ") ||
                    normAfter.startsWith("EIDWN") ||
                    normAfter.startsWith("ITEMS") ||
                    normAfter.startsWith("QTY") ||
                    normAfter.contains("ΕΞΟΙΚΟΝΟΜ") ||
                    normAfter.contains("ΕΚΠΤΩΣ") ||
                    normAfter.contains("ΟΦΕΛΟΣ") ||
                    normAfter.contains("ΚΕΡΔΟΣ")
                if (!isIgnoredModifier) {
                    return if (start >= 0) start + w.length else line.length
                }
            }
            if (start >= 0) currIdx = start + w.length
        }
        return null
    }

    private fun isFuzzySynolo(word: String): Boolean {
        val targets = listOf(
            "ΣΥΝΟΛΟ", "ΣΥΝΟΛΟΝ", "EYNOAO", "ZYNOAO", "SYNOAO", "SYNOLO", "TOTAL", "ΣΥΝΟΑΟ",
            "EVVOLO", "ZVVOLO", "EVVOIO", "ZVVOIO", "SVVOLO", "EUVODO", "EVNOLO", "ZVNOLO",
            "ZVVOTO", "EVVOTO", "ZUVOTO", "EUVOTO", "ZUVODO", "ΣΥΝΟΛΙΚΗ", "SYNOLIKH"
        )
        return targets.any { levenshteinDistance(word, it) <= 1 }
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
            }
        }
        return dp[s1.length][s2.length]
    }

    fun extractTotalAmount(lines: List<String>, visionText: Text? = null): Double {
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            val matchEnd = findSynoloMatchEnd(line) ?: continue

            val textToRight = line.substring(matchEnd).trim()
            if (textToRight.isNotBlank()) {
                val matches = PRICE_REGEX.findAll(textToRight).toList()
                if (matches.isNotEmpty()) {
                    val parsed = parseGreekPrice(matches.last().value)
                    if (parsed != null && parsed > 0.0) return parsed
                }
            }

            if (visionText != null) {
                val spatialAmount = findPriceToTheRightSpatially(visionText)
                if (spatialAmount != null && spatialAmount > 0.0) return spatialAmount
            }

            for (offset in 1..8) {
                if (i + offset < lines.size) {
                    val candidateLine = lines[i + offset].trim()
                    if (candidateLine.replace(Regex("[-=_*]"), "").isBlank()) continue
                    val upperCand = candidateLine.uppercase(Locale.ROOT)
                    if (upperCand.contains("ΜΕΤΡΗΤ") || upperCand.contains("METRHT") ||
                        upperCand.contains("ΡΕΣΤ") || upperCand.contains("REST")
                    ) {
                        continue
                    }
                    val nextMatches = PRICE_REGEX.findAll(candidateLine).toList()
                    if (nextMatches.isNotEmpty()) {
                        val parsed = parseGreekPrice(nextMatches.last().value)
                        if (parsed != null && parsed > 0.0) return parsed
                    }
                }
            }
        }

        if (visionText != null) {
            val spatialAmount = findPriceToTheRightSpatially(visionText)
            if (spatialAmount != null && spatialAmount > 0.0) return spatialAmount
        }

        return 0.0
    }

    private fun findPriceToTheRightSpatially(visionText: Text): Double? {
        data class PriceCandidate(val amount: Double, val rect: android.graphics.Rect)
        val allPrices = mutableListOf<PriceCandidate>()

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val upperLine = line.text.uppercase(Locale.ROOT)
                if (upperLine.contains("ΜΕΤΡΗΤ") || upperLine.contains("METRHT") ||
                    upperLine.contains("ΡΕΣΤ") || upperLine.contains("REST") ||
                    upperLine.contains("CHANGE")
                ) {
                    continue
                }

                for (element in line.elements) {
                    val box = element.boundingBox ?: continue
                    for (match in PRICE_REGEX.findAll(element.text)) {
                        val amt = parseGreekPrice(match.value)
                        if (amt != null && amt > 0.0) {
                            allPrices.add(PriceCandidate(amt, box))
                        }
                    }
                }

                val lineBox = line.boundingBox
                if (lineBox != null) {
                    for (match in PRICE_REGEX.findAll(line.text)) {
                        val amt = parseGreekPrice(match.value)
                        if (amt != null && amt > 0.0) {
                            allPrices.add(PriceCandidate(amt, lineBox))
                        }
                    }
                }
            }
        }

        if (allPrices.isEmpty()) return null

        for (block in visionText.textBlocks.reversed()) {
            for (line in block.lines.reversed()) {
                val matchEnd = findSynoloMatchEnd(line.text) ?: continue
                val lineBox = line.boundingBox ?: continue
                val lineCenterY = lineBox.centerY()

                val textToRight = line.text.substring(matchEnd).trim()
                if (textToRight.isNotBlank()) {
                    val inlineMatches = PRICE_REGEX.findAll(textToRight).toList()
                    if (inlineMatches.isNotEmpty()) {
                        val parsed = parseGreekPrice(inlineMatches.last().value)
                        if (parsed != null && parsed > 0.0) return parsed
                    }
                }

                val lineToleranceY = (lineBox.height() * 8.0).toInt().coerceIn(350, 600)
                val matchingPrices = allPrices.filter { cand ->
                    cand.rect.centerX() > lineBox.left &&
                        Math.abs(cand.rect.centerY() - lineCenterY) <= lineToleranceY
                }

                if (matchingPrices.isNotEmpty()) {
                    val best = matchingPrices.minByOrNull { Math.abs(it.rect.centerY() - lineCenterY) }
                    if (best != null && best.amount > 0.0) return best.amount
                }
            }
        }
        return null
    }

    fun parseGreekPrice(raw: String): Double? {
        val cleaned = raw
            .replace("€", "")
            .replace("EUR", "", ignoreCase = true)
            .replace("ΕΥΡΩ", "", ignoreCase = true)
            .replace(":", "")
            .replace(" ", "")
            .trim()

        if (cleaned.isBlank()) return null

        if (cleaned.contains(".") && cleaned.contains(",")) {
            return if (cleaned.lastIndexOf(",") > cleaned.lastIndexOf(".")) {
                cleaned.replace(".", "").replace(",", ".").toDoubleOrNull()
            } else {
                cleaned.replace(",", "").toDoubleOrNull()
            }
        }

        if (cleaned.contains(",")) {
            return cleaned.replace(",", ".").toDoubleOrNull()
        }

        return cleaned.toDoubleOrNull()
    }

    fun extractAfm(text: String): String {
        val afmRegex = Regex("""(?i)(?:ΑΦΜ|Α\.Φ\.Μ\.?|TIN|VAT)[:\s]*([0-9]{9})""")
        val match = afmRegex.find(text)
        return match?.groupValues?.get(1) ?: ""
    }

    fun extractDate(text: String): Long? {
        val datePatterns = listOf(
            Pattern.compile("([0-3]?[0-9])[/.\\-]([0-1]?[0-9])[/.\\-](20[2-3][0-9])"),
            Pattern.compile("([0-3]?[0-9])[/.\\-]([0-1]?[0-9])[/.\\-]([2-3][0-9])")
        )

        for (pattern in datePatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val day = matcher.group(1)?.toIntOrNull() ?: continue
                val month = matcher.group(2)?.toIntOrNull() ?: continue
                var year = matcher.group(3)?.toIntOrNull() ?: continue
                if (year < 100) year += 2000

                if (day in 1..31 && month in 1..12 && year in 2020..2030) {
                    val cal = Calendar.getInstance()
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, month - 1)
                    cal.set(Calendar.DAY_OF_MONTH, day)
                    return cal.timeInMillis
                }
            }
        }
        return null
    }

    fun parseAadeQrInternal(qrText: String): InternalQrData {
        var amount = 0.0
        var merchantAfm = ""
        var timestamp = 0L

        val amountRegex = Regex("(?:amount|poso|total|sum|val|tot|price)=([0-9]+(?:[.,][0-9]{1,2})?)", RegexOption.IGNORE_CASE)
        val amountMatch = amountRegex.find(qrText)
        if (amountMatch != null) {
            val raw = amountMatch.groupValues[1].replace(',', '.')
            amount = raw.toDoubleOrNull() ?: 0.0
        }

        val afmRegex = Regex("(?:tin|afm|vat)=([0-9]{9})", RegexOption.IGNORE_CASE)
        val afmMatch = afmRegex.find(qrText)
        if (afmMatch != null) {
            merchantAfm = afmMatch.groupValues[1]
        }

        val dateRegex = Regex("(?:date|d)=([0-9]{4}[-]?[0-9]{2}[-]?[0-9]{2})", RegexOption.IGNORE_CASE)
        val dateMatch = dateRegex.find(qrText)
        if (dateMatch != null) {
            val dStr = dateMatch.groupValues[1].replace("-", "")
            if (dStr.length == 8) {
                try {
                    val sdf = SimpleDateFormat("yyyyMMdd", Locale.ROOT)
                    timestamp = sdf.parse(dStr)?.time ?: 0L
                } catch (_: Exception) {}
            }
        }

        return InternalQrData(amount, merchantAfm, timestamp)
    }
}
