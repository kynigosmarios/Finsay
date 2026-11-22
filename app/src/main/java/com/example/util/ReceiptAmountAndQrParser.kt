package com.example.util

import android.graphics.Bitmap
import android.net.Uri
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLDecoder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

internal data class InternalQrData(
    val amount: Double = 0.0,
    val merchantAfm: String = "",
    val timestamp: Long = 0L,
    val merchantName: String = ""
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

    private val aadeHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(3500, TimeUnit.MILLISECONDS)
            .readTimeout(3500, TimeUnit.MILLISECONDS)
            .callTimeout(4500, TimeUnit.MILLISECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val ALLOWED_QR_HOSTS = listOf(
        "aade.gr",
        "epsilonnet.gr",
        "softone.gr",
        "entersoft.gr",
        "impact.gr",
        "elorus.com",
        "oxygen.gr",
        "simply.gr",
        "timologiera.gr",
        "myinvoice.gr",
        "einvoicing.gr",
        "yodiwo.com"
    )

    private fun isAllowedAadeOrInvoiceHost(host: String?): Boolean {
        val h = host?.trim()?.lowercase(Locale.ROOT) ?: return false
        return ALLOWED_QR_HOSTS.any { allowed -> h == allowed || h.endsWith(".$allowed") }
    }

    private fun isLikelyAadeQrValue(raw: String): Boolean {
        val lower = raw.lowercase(Locale.ROOT)
        return lower.contains("aade.gr") ||
            lower.contains("tameiakes") ||
            lower.contains("mydata") ||
            lower.contains("timologio") ||
            lower.contains("einvoic") ||
            lower.contains("esend") ||
            lower.startsWith("http://") ||
            lower.startsWith("https://")
    }

    /**
     * Extracts the best QR code from a receipt bitmap using QR-focused ML Kit scanning:
     * 1) full image, 2) bottom 50% crop, 3) 2x upscaled bottom 50% crop for small thermal QRs.
     */
    suspend fun extractBestQrCode(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        val scanner = BarcodeScanning.getClient(options)

        suspend fun scanBmp(bmp: Bitmap): String? {
            return try {
                val barcodes = scanner.process(InputImage.fromBitmap(bmp, 0)).await()
                val candidates = barcodes.mapNotNull { it.rawValue?.trim() }.filter { it.isNotBlank() }
                candidates.firstOrNull { isLikelyAadeQrValue(it) } ?: candidates.firstOrNull()
            } catch (_: Exception) {
                null
            }
        }

        val fullMatch = scanBmp(bitmap)
        if (!fullMatch.isNullOrBlank()) return@withContext fullMatch

        if (bitmap.height > 400) {
            try {
                val cropY = (bitmap.height * 0.50).toInt().coerceIn(0, bitmap.height - 1)
                val cropH = (bitmap.height - cropY).coerceAtLeast(1)
                val bottomCrop = Bitmap.createBitmap(bitmap, 0, cropY, bitmap.width, cropH)
                val bottomMatch = scanBmp(bottomCrop)
                if (!bottomMatch.isNullOrBlank()) return@withContext bottomMatch

                // Upscale small/low-res bottom crops so ML Kit can lock onto dense AADE QR modules
                if (bottomCrop.width < 1200 && bottomCrop.height < 1200) {
                    val scaled = Bitmap.createScaledBitmap(
                        bottomCrop,
                        (bottomCrop.width * 2).coerceAtMost(2000),
                        (bottomCrop.height * 2).coerceAtMost(2000),
                        true
                    )
                    val scaledMatch = scanBmp(scaled)
                    if (!scaledMatch.isNullOrBlank()) return@withContext scaledMatch
                }
            } catch (_: Exception) {
            }
        }
        null
    }

    private fun safeUrlDecode(value: String): String {
        return try {
            URLDecoder.decode(value, "UTF-8").trim()
        } catch (_: Exception) {
            value.trim()
        }
    }

    private fun parseQrDateString(rawDate: String): Long {
        val cleaned = rawDate.trim()
        val digitsOnly = cleaned.replace(Regex("[^0-9]"), "")
        if (digitsOnly.length >= 8) {
            val ymd = digitsOnly.take(8)
            val year = ymd.substring(0, 4).toIntOrNull()
            val month = ymd.substring(4, 6).toIntOrNull()
            val day = ymd.substring(6, 8).toIntOrNull()
            if (year != null && year in 2020..2035 && month != null && month in 1..12 && day != null && day in 1..31) {
                try {
                    val sdf = SimpleDateFormat("yyyyMMdd", Locale.ROOT)
                    return sdf.parse(ymd)?.time ?: 0L
                } catch (_: Exception) {}
            }
        }
        return extractDate(cleaned) ?: 0L
    }

    fun parseAadeQrInternal(qrText: String): InternalQrData {
        var amount = 0.0
        var merchantAfm = ""
        var timestamp = 0L
        var merchantName = ""

        val trimmed = qrText.trim()
        if (trimmed.isBlank()) return InternalQrData()

        // 1. Named query/key-value parameters
        val amountRegex = Regex(
            "(?:^|[?&;|,\\s])(?:amount|poso|total|totalValue|totalAmount|gross|grossValue|sum|val|tot|price|payable|t|a)=([0-9]+(?:[.,][0-9]{1,2})?)",
            RegexOption.IGNORE_CASE
        )
        amountRegex.find(trimmed)?.let { m ->
            val parsed = m.groupValues[1].replace(',', '.').toDoubleOrNull() ?: 0.0
            if (parsed in 0.01..99999.0) amount = parsed
        }

        val afmRegex = Regex(
            "(?:^|[?&;|,\\s])(?:tin|afm|vat|issuerVat|issuerTin|sellerVat|vatNumber)=([0-9]{9})(?:[^0-9]|$)",
            RegexOption.IGNORE_CASE
        )
        afmRegex.find(trimmed)?.let { m ->
            merchantAfm = m.groupValues[1]
        }

        val nameRegex = Regex(
            "(?:^|[?&;|])(?:issuerName|issuer_name|sellerName|seller|merchantName|merchant|companyName|company|storeName|store|brand|eponymia|vendor|name)=([^&;|]+)",
            RegexOption.IGNORE_CASE
        )
        nameRegex.find(trimmed)?.let { m ->
            val decoded = safeUrlDecode(m.groupValues[1])
            if (decoded.length >= 2 && decoded.any { it.isLetter() }) {
                merchantName = decoded.take(60)
            }
        }

        val dateRegex = Regex(
            "(?:^|[?&;|,\\s])(?:issueDate|date|dt)=([0-9]{4}[-/]?[0-9]{2}[-/]?[0-9]{2}|[0-9]{2}[-/][0-9]{2}[-/][0-9]{4})",
            RegexOption.IGNORE_CASE
        )
        dateRegex.find(trimmed)?.let { m ->
            timestamp = parseQrDateString(m.groupValues[1])
        }

        // 2. Delimited AADE ΦΗΜ / e-send payloads (e.g. ?d=SERIAL|AFM|YYYYMMDD|TOTAL or comma/semicolon-separated)
        val payloadToSplit = try {
            val uri = Uri.parse(trimmed)
            uri.getQueryParameter("d")
                ?: uri.getQueryParameter("qr")
                ?: uri.getQueryParameter("data")
                ?: trimmed
        } catch (_: Exception) {
            trimmed
        }

        val decodedPayload = safeUrlDecode(payloadToSplit)
        val delimiterParts = decodedPayload.split('|', ',', ';', '^').map { it.trim() }.filter { it.isNotBlank() }
        if (delimiterParts.size >= 3) {
            val decimalCandidates = mutableListOf<Double>()
            for (part in delimiterParts) {
                if (merchantAfm.isBlank() && part.matches(Regex("^[0-9]{9}$"))) {
                    val asYearPrefix = part.take(4).toIntOrNull()
                    if (asYearPrefix == null || asYearPrefix !in 2020..2035) {
                        merchantAfm = part
                    }
                }
                if (timestamp == 0L && (part.matches(Regex("^20[2-3][0-9][01][0-9][0-3][0-9].*$")) ||
                        part.matches(Regex("^[0-3]?[0-9][./-][01]?[0-9][./-]20[2-3][0-9].*$")) ||
                        part.matches(Regex("^20[2-3][0-9]-[01][0-9]-[0-3][0-9].*$")))
                ) {
                    val ts = parseQrDateString(part)
                    if (ts > 0L) timestamp = ts
                }
                if (part.matches(Regex("^[0-9]{1,5}[.,][0-9]{2}$"))) {
                    val v = part.replace(',', '.').toDoubleOrNull()
                    if (v != null && v in 0.01..99999.0) {
                        decimalCandidates.add(v)
                    }
                }
            }
            if (amount <= 0.0 && decimalCandidates.isNotEmpty()) {
                // On AADE delimited payloads, the gross total is the largest monetary value (vs net / VAT)
                amount = decimalCandidates.max()
            }
        }

        // Fallback plain date parameter `d=YYYYMMDD`
        if (timestamp == 0L) {
            val shortDateRegex = Regex("(?:^|[?&])d=([0-9]{4}[-]?[0-9]{2}[-]?[0-9]{2})(?:&|$)", RegexOption.IGNORE_CASE)
            shortDateRegex.find(trimmed)?.let { m ->
                timestamp = parseQrDateString(m.groupValues[1])
            }
        }

        return InternalQrData(
            amount = amount,
            merchantAfm = merchantAfm,
            timestamp = timestamp,
            merchantName = merchantName
        )
    }

    /**
     * Resolves full AADE QR metadata: first parses the QR payload locally, and if the QR is an
     * HTTPS verification URL from AADE or a certified Greek e-invoicing provider that doesn't
     * embed seller name or total directly in query params, fetches and parses the verification page
     * with a strict short timeout and silent fallback.
     */
    suspend fun resolveAadeQrData(qrText: String?): InternalQrData {
        if (qrText.isNullOrBlank()) return InternalQrData()
        val localData = parseAadeQrInternal(qrText)
        if (localData.merchantName.isNotBlank() && localData.amount > 0.0) {
            return localData
        }

        val trimmed = qrText.trim()
        val parsedUri = try {
            Uri.parse(trimmed)
        } catch (_: Exception) {
            null
        }
        val scheme = parsedUri?.scheme?.lowercase(Locale.ROOT)
        val host = parsedUri?.host?.lowercase(Locale.ROOT)
        if (scheme != "https" || !isAllowedAadeOrInvoiceHost(host)) {
            return localData
        }

        val remoteData = withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(trimmed)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) FinsayReceiptReader/1.0")
                    .header("Accept", "text/html,application/xhtml+xml,application/json;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "el-GR,el;q=0.9,en;q=0.8")
                    .get()
                    .build()

                aadeHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null
                    val body = response.body?.string()?.take(262_144) ?: return@withContext null
                    parseAadeVerificationHtmlOrJson(body)
                }
            } catch (_: Exception) {
                null
            }
        } ?: return localData

        return InternalQrData(
            amount = if (localData.amount > 0.0) localData.amount else remoteData.amount,
            merchantAfm = localData.merchantAfm.ifBlank { remoteData.merchantAfm },
            timestamp = if (localData.timestamp > 0L) localData.timestamp else remoteData.timestamp,
            merchantName = localData.merchantName.ifBlank { remoteData.merchantName }
        )
    }

    private fun decodeHtmlEntities(input: String): String {
        return input
            .replace("&nbsp;", " ", ignoreCase = true)
            .replace("&amp;", "&", ignoreCase = true)
            .replace("&quot;", "\"", ignoreCase = true)
            .replace("&#39;", "'", ignoreCase = true)
            .replace("&apos;", "'", ignoreCase = true)
            .replace("&euro;", "€", ignoreCase = true)
            .replace("&lt;", "<", ignoreCase = true)
            .replace("&gt;", ">", ignoreCase = true)
    }

    private fun parseAadeVerificationHtmlOrJson(rawBody: String): InternalQrData {
        var amount = 0.0
        var merchantAfm = ""
        var timestamp = 0L
        var merchantName = ""

        // Strip scripts/styles and convert block/cell tags into newlines for clean label:value parsing
        val cleanedText = decodeHtmlEntities(
            rawBody
                .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("</?(?:tr|td|th|div|p|li|br|h[1-6]|dt|dd|span|label)[^>]*>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("<[^>]+>"), " ")
        )
        val lines = cleanedText.lines().map { it.replace(Regex("\\s+"), " ").trim() }.filter { it.isNotBlank() }

        fun findValueAfterLabel(labelRegex: Regex, validator: (String) -> Boolean): String? {
            for (i in lines.indices) {
                val line = lines[i]
                val m = labelRegex.find(line) ?: continue
                val remainder = line.substring(m.range.last + 1).trim().trimStart(':', '-', '–', '•').trim()
                if (remainder.isNotBlank() && validator(remainder)) {
                    return remainder
                }
                if (i + 1 < lines.size) {
                    val nextLine = lines[i + 1].trim()
                    if (nextLine.isNotBlank() && validator(nextLine)) {
                        return nextLine
                    }
                }
            }
            return null
        }

        // 1. Seller / Issuer Name
        val sellerLabelRegex = Regex(
            "(?:Επωνυμ[ίι]α(?:\\s+Εκδ[όο]τη|\\s+Επιχε[ίι]ρησης)?|Διακριτικ[όο]ς\\s+Τ[ίι]τλος|Εκδ[όο]της|Πωλητ[ήη]ς|Επιχε[ίι]ρηση|Issuer\\s*Name|Seller\\s*Name|Company\\s*Name)",
            RegexOption.IGNORE_CASE
        )
        val badSellerValues = setOf("ΑΦΜ", "Α.Φ.Μ.", "ΗΜΕΡΟΜΗΝΙΑ", "ΣΥΝΟΛΟ", "ΠΟΣΟ", "ΑΞΙΑ", "ΝΑΙ", "ΟΧΙ", "ΕΓΚΥΡΗ", "VALID")
        findValueAfterLabel(sellerLabelRegex) { candidate ->
            val upper = removeGreekAccents(candidate).uppercase(Locale.ROOT)
            candidate.length in 2..80 &&
                candidate.any { it.isLetter() } &&
                !badSellerValues.contains(upper) &&
                !upper.startsWith("ΑΦΜ") &&
                !upper.startsWith("Α.Φ.Μ") &&
                !candidate.matches(Regex("^[0-9.,€\\s]+$"))
        }?.let { seller ->
            merchantName = seller.take(60)
        }

        // Also support JSON-like payloads returned by e-invoicing endpoints
        if (merchantName.isBlank()) {
            val jsonSeller = Regex(
                "\"(?:issuerName|sellerName|companyName|tradeName|eponymia)\"\\s*:\\s*\"([^\"]{2,80})\"",
                RegexOption.IGNORE_CASE
            ).find(rawBody)?.groupValues?.get(1)?.trim()
            if (!jsonSeller.isNullOrBlank() && jsonSeller.any { it.isLetter() }) {
                merchantName = jsonSeller.take(60)
            }
        }

        // 2. AFM
        merchantAfm = extractAfm(cleanedText)
        if (merchantAfm.isBlank()) {
            val jsonAfm = Regex("\"(?:issuerVat|vatNumber|afm|tin)\"\\s*:\\s*\"([0-9]{9})\"", RegexOption.IGNORE_CASE)
                .find(rawBody)?.groupValues?.get(1)
            if (!jsonAfm.isNullOrBlank()) merchantAfm = jsonAfm
        }

        // 3. Total Amount
        val totalLabelRegex = Regex(
            "(?:Συνολικ[ήη]\\s+Αξ[ίι]α|Τελικ[όο]\\s+Ποσ[όο]|Πληρωτ[έε]ο(?:\\s+Ποσ[όο])?|Γενικ[όο]\\s+Σ[ύυ]νολο|Σ[ύυ]νολο\\s*(?:Αξ[ίι]ας|Πληρωμ[ήη]ς|Απόδειξης)?|Total\\s*(?:Amount|Value|Gross)?|Gross\\s*Value)",
            RegexOption.IGNORE_CASE
        )
        findValueAfterLabel(totalLabelRegex) { candidate ->
            PRICE_REGEX.containsMatchIn(candidate)
        }?.let { amountLine ->
            val m = PRICE_REGEX.find(amountLine)
            if (m != null) {
                val parsed = parseGreekPrice(m.groupValues[1]) ?: 0.0
                if (parsed in 0.01..99999.0) amount = parsed
            }
        }

        if (amount <= 0.0) {
            val jsonAmt = Regex(
                "\"(?:totalGrossValue|totalAmount|grossValue|payableAmount|total)\"\\s*:\\s*\"?([0-9]+(?:[.,][0-9]{1,2})?)\"?",
                RegexOption.IGNORE_CASE
            ).find(rawBody)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
            if (jsonAmt != null && jsonAmt in 0.01..99999.0) {
                amount = jsonAmt
            }
        }

        // 4. Date
        val dateLabelRegex = Regex("(?:Ημερομην[ίι]α(?:\\s+Έκδοσης)?|Ημ/ν[ίι]α|Issue\\s*Date|Date)", RegexOption.IGNORE_CASE)
        findValueAfterLabel(dateLabelRegex) { candidate ->
            parseQrDateString(candidate) > 0L
        }?.let { dateStr ->
            timestamp = parseQrDateString(dateStr)
        }
        if (timestamp == 0L) {
            timestamp = extractDate(cleanedText) ?: 0L
        }

        return InternalQrData(
            amount = amount,
            merchantAfm = merchantAfm,
            timestamp = timestamp,
            merchantName = merchantName
        )
    }
}
