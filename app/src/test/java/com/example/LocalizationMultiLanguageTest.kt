package com.example

import com.example.data.entity.Expense
import com.example.data.entity.FamilyMember
import com.example.data.entity.resolveReconciledPayers
import com.example.util.CurrencyFormatter
import com.example.util.Localization
import com.example.util.RecurringScheduleHelper
import com.example.util.SettleUpCalculator
import com.example.util.i18n.TranslationCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale

class LocalizationMultiLanguageTest {

    private val nonBaseLanguages = listOf(
        Localization.Language.SPANISH,
        Localization.Language.FRENCH,
        Localization.Language.GERMAN,
        Localization.Language.ITALIAN,
        Localization.Language.PORTUGUESE,
        Localization.Language.DUTCH,
        Localization.Language.POLISH,
        Localization.Language.ROMANIAN
    )

    private val canonicalCategoryKeys = listOf(
        "Supermarket",
        "Food & Coffee",
        "Food",
        "Bills & Home",
        "Bills",
        "Rent",
        "Transport & Fuel",
        "Transportation",
        "Health & Pharmacy",
        "Health & Care",
        "Entertainment",
        "Children",
        "Home & Maintenance",
        "Clothing",
        "Education",
        "Salary",
        "Business / Freelance",
        "Subsidy / Allowance",
        "Bonus / Gift",
        "Investments",
        "Other",
        "Savings & Goals"
    )

    private val dynamicHelperKeys = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
        "Only Creators or Editors can manage savings goals",
        "Only Creators or Editors can save savings goals",
        "Only Creators or Editors can delete savings goals",
        "Only Creators or Editors can deposit to savings goals",
        "Only Creators or Editors can access reminders",
        "Only Creators or Editors can log expenses from reminders",
        "Only Creators or Editors can manage standing orders",
        "Only Creators or Editors can save standing orders",
        "Only Creators or Editors can delete standing orders",
        "Only Creators or Editors can toggle standing orders",
        "Only Creators or Editors can execute standing orders"
    )

    private val placeholderRegex = Regex("%(?:\\d+\\$)(?:[-+#0,(]*\\d*(?:\\.\\d+)?[sdifSDIF])|%(?:[-+#0,(]*\\d*(?:\\.\\d+)?[sdif])")

    @Test
    fun allTenLanguagesSupportedAndRoundTrip() {
        assertEquals(10, Localization.Language.entries.size)
        val expectedCodes = listOf("EN", "EL", "ES", "FR", "DE", "IT", "PT", "NL", "PL", "RO")
        assertEquals(expectedCodes, Localization.Language.entries.map { it.code })

        Localization.Language.entries.forEach { lang ->
            assertEquals(lang, Localization.Language.fromCode(lang.code))
            assertEquals(lang, Localization.Language.fromCode(lang.code.lowercase(Locale.ROOT)))
            assertEquals(lang, Localization.Language.fromCode(lang.firebaseCode))
            assertEquals(lang, Localization.Language.fromCode(lang.bcp47Tag))
            assertTrue(lang.displayName.isNotBlank())
            assertTrue(lang.flag.isNotBlank())
            assertNotNull(Localization.getDateLocale(lang))
        }
    }

    @Test
    fun canonicalRoleAndCategoryAndHouseholdRoundTripAcrossAllLanguages() {
        val sampleRoles = listOf("Parent", "Child", "Partner", "Roommate", "Colleague", "Friend", "Individual", "Other")
        Localization.Language.entries.forEach { lang ->
            // Default household name round-trip
            val defaultHouse = Localization.myExodologio(lang)
            assertTrue("Default household blank for $lang", defaultHouse.isNotBlank())
            assertEquals(
                "Household translation round-trip failed for $lang ($defaultHouse)",
                Localization.myExodologio(Localization.Language.ENGLISH),
                Localization.translateHouseholdName(defaultHouse, Localization.Language.ENGLISH)
            )

            // All-members label recognition
            assertTrue(
                "allMembersWord not recognized for $lang: ${Localization.allMembersWord(lang)}",
                Localization.isAllMembersLabel(Localization.allMembersWord(lang))
            )
            assertTrue(
                "sharedFamily not recognized for $lang: ${Localization.sharedFamily(lang)}",
                Localization.isAllMembersLabel(Localization.sharedFamily(lang))
            )

            // Roles singular & plural round-trip
            sampleRoles.forEach { enRole ->
                val expectedKey = Localization.canonicalRoleKey(enRole)
                val localizedSingular = Localization.translateRole(enRole, lang)
                val localizedPlural = Localization.pluralRoleName(enRole, lang)
                assertEquals(
                    "Singular role canonical mismatch for $lang: $enRole -> $localizedSingular",
                    expectedKey,
                    Localization.canonicalRoleKey(localizedSingular)
                )
                assertEquals(
                    "Plural role canonical mismatch for $lang: $enRole -> $localizedPlural",
                    expectedKey,
                    Localization.canonicalRoleKey(localizedPlural)
                )
                assertNotNull(
                    "Plural role not in knownPluralRoleLabels for $lang: $localizedPlural",
                    Localization.translateCollectiveRoleLabel(localizedPlural, Localization.Language.ENGLISH)
                )
            }

            // Categories round-trip
            canonicalCategoryKeys.forEach { enCat ->
                val localizedCat = Localization.translateCategory(enCat, lang)
                assertEquals(
                    "Category round-trip failed for $lang: $enCat -> $localizedCat",
                    enCat,
                    Localization.translateCategory(localizedCat, Localization.Language.ENGLISH)
                )
            }
        }
    }

    @Test
    fun crossLanguageExpensePayerReconciliationAndSettleUp() {
        val alex = FamilyMember(id = 1L, name = "Alex", role = "Parent")
        val maria = FamilyMember(id = 2L, name = "Maria", role = "Parent")
        val leo = FamilyMember(id = 3L, name = "Leo", role = "Child")
        val members = listOf(alex, maria, leo)

        Localization.Language.entries.forEach { lang ->
            val parentsLabel = Localization.pluralRoleName("Parent", lang)
            val allLabel = Localization.allMembersWord(lang)

            val expense = Expense(
                id = 10L,
                title = "Groceries",
                amount = 100.0,
                categoryId = 1L,
                categoryName = "Supermarket",
                splitType = "ALL_FAMILY",
                memberIdsCsv = "",
                memberNamesDisplay = allLabel,
                payerMemberId = 1L,
                payerMemberName = parentsLabel,
                payerMemberIdsCsv = ""
            )
            val payers = expense.resolveReconciledPayers(members)
            assertEquals("Expected both parents for $lang ($parentsLabel)", setOf(1L, 2L), payers.map { it.id }.toSet())

            val settleResult = com.example.util.SettleUpResult(
                memberSummaries = emptyList(),
                transfers = listOf(
                    com.example.util.SettlementTransfer(
                        fromMember = alex,
                        toMember = maria,
                        amount = 25.0
                    )
                ),
                totalHouseholdExpenses = 100.0,
                isBalanced = false
            )
            val summary = SettleUpCalculator.formatSummaryText(
                result = settleResult,
                householdTitle = Localization.myExodologio(lang),
                lang = lang,
                currencySymbol = "€"
            )
            assertTrue("Summary missing transfer details for $lang", summary.contains("Alex") && summary.contains("Maria"))

            val freqLabel = RecurringScheduleHelper.frequencyLabel("MONTHLY", lang)
            assertTrue("Recurring label blank for $lang", freqLabel.isNotBlank())
        }
    }

    @Test
    fun extractAllKeysAndVerifyCompleteMultiLanguageCoverage() {
        val srcRoot = resolveSourceRoot()
        assertTrue("Could not find src/main/java/com/example at ${srcRoot.absolutePath}", srcRoot.exists())

        val extractedKeys = LinkedHashMap<String, MutableSet<String>>()
        val dynamicOrInterpolated = mutableListOf<String>()

        fun addKey(key: String, source: String) {
            extractedKeys.getOrPut(key) { LinkedHashSet() }.add(source)
        }

        // 1. Canonical categories, dynamic helper keys & currency names
        canonicalCategoryKeys.forEach { addKey(it, "Localization.kt(category)") }
        dynamicHelperKeys.forEach { addKey(it, "DynamicHelpers") }
        CurrencyFormatter.supportedCurrencies.forEach { addKey(it.nameEn, "CurrencyFormatter.kt") }

        // 2. Scan all Kotlin files
        val ktFiles = srcRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" && !it.path.contains("${File.separator}i18n${File.separator}") }
            .sortedBy { it.path }
            .toList()

        for (file in ktFiles) {
            val content = file.readText(Charsets.UTF_8)
            val relName = file.relativeTo(srcRoot).path
            val scanResult = scanKotlinFileForKeys(content, relName)
            scanResult.keys.forEach { addKey(it, relName) }
            dynamicOrInterpolated.addAll(scanResult.warnings)
        }

        // Write extracted keys report for inspection in build/
        val moduleRoot = srcRoot.parentFile?.parentFile?.parentFile?.parentFile?.parentFile ?: File(".")
        val reportFile = File(moduleRoot, "build/i18n_extracted_keys.txt")
        reportFile.parentFile?.mkdirs()
        val sb = StringBuilder()
        sb.appendLine("# Total unique EN keys: ${extractedKeys.size}")
        sb.appendLine("# Dynamic/interpolated warnings: ${dynamicOrInterpolated.size}")
        dynamicOrInterpolated.forEach { sb.appendLine("# WARN: $it") }
        var idx = 1
        extractedKeys.forEach { (key, files) ->
            val escaped = key
                .replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\"", "\\\"")
            sb.appendLine("$idx\t${files.first()}\t$escaped")
            idx++
        }
        reportFile.writeText(sb.toString(), Charsets.UTF_8)

        assertTrue(
            "Found dynamic or interpolated strings passed to Localization.tr/trFormat:\n" +
                dynamicOrInterpolated.joinToString("\n"),
            dynamicOrInterpolated.isEmpty()
        )

        // 3. Verify 100% key coverage & placeholder parity across all 8 non-EN/EL languages
        val missingReport = StringBuilder()
        val placeholderErrors = mutableListOf<String>()
        var totalMissing = 0

        for (lang in nonBaseLanguages) {
            val catalog = TranslationCatalog.mapFor(lang)
            val missingForLang = mutableListOf<String>()

            for (enKey in extractedKeys.keys) {
                val translated = catalog[enKey]
                if (translated.isNullOrBlank()) {
                    missingForLang.add(enKey)
                } else {
                    val enPlaceholders = extractPlaceholders(enKey)
                    val trPlaceholders = extractPlaceholders(translated)
                    if (enPlaceholders != trPlaceholders) {
                        placeholderErrors.add(
                            "[${lang.code}] Placeholder mismatch for \"$enKey\" -> \"$translated\" " +
                                "(expected $enPlaceholders, got $trPlaceholders)"
                        )
                    } else if (enPlaceholders.isNotEmpty()) {
                        // Also test actual String.format execution
                        val sampleArgs = buildSampleArgs(enPlaceholders)
                        try {
                            String.format(Localization.getDateLocale(lang), translated, *sampleArgs)
                        } catch (t: Throwable) {
                            placeholderErrors.add(
                                "[${lang.code}] String.format crashed for \"$enKey\" -> \"$translated\": ${t.message}"
                            )
                        }
                    }
                }
            }

            if (missingForLang.isNotEmpty()) {
                totalMissing += missingForLang.size
                missingReport.appendLine("[${lang.code}] Missing ${missingForLang.size}/${extractedKeys.size} keys (first 10):")
                missingForLang.take(10).forEach { k ->
                    missingReport.appendLine("  - ${k.replace("\n", "\\n")}")
                }
            }
        }

        assertTrue(
            "Placeholder/format errors found:\n${placeholderErrors.joinToString("\n")}",
            placeholderErrors.isEmpty()
        )
        assertEquals(
            "Missing translations across 8 languages (total=$totalMissing). Extracted key list written to ${reportFile.absolutePath}:\n$missingReport",
            0,
            totalMissing
        )
    }

    private fun extractPlaceholders(text: String): List<String> {
        return placeholderRegex.findAll(text)
            .map { it.value }
            .filter { it != "%%" }
            .sorted()
            .toList()
    }

    private fun buildSampleArgs(placeholders: List<String>): Array<Any> {
        var maxPos = 0
        val typesByPos = mutableMapOf<Int, Char>()
        var implicitIdx = 1
        for (ph in placeholders) {
            val typeChar = ph.last()
            val posMatch = Regex("%(\\d+)\\$").find(ph)
            val pos = posMatch?.groupValues?.get(1)?.toInt() ?: implicitIdx++
            if (pos > maxPos) maxPos = pos
            typesByPos[pos] = typeChar
        }
        return Array(maxPos) { i ->
            when (typesByPos[i + 1]?.lowercaseChar()) {
                'd' -> 42
                'f' -> 12.5
                else -> "Sample"
            }
        }
    }

    private fun resolveSourceRoot(): File {
        val cwd = File(System.getProperty("user.dir") ?: ".")
        val candidates = listOf(
            File(cwd, "src/main/java/com/example"),
            File(cwd, "app/src/main/java/com/example")
        )
        return candidates.firstOrNull { it.exists() } ?: candidates.first()
    }

    private data class ScanResult(val keys: List<String>, val warnings: List<String>)

    private fun scanKotlinFileForKeys(src: String, relName: String): ScanResult {
        val keys = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val n = src.length
        var i = 0

        fun skipWsAndComments() {
            while (i < n) {
                when {
                    src[i].isWhitespace() -> i++
                    i + 1 < n && src[i] == '/' && src[i + 1] == '/' -> {
                        i += 2
                        while (i < n && src[i] != '\n') i++
                    }
                    i + 1 < n && src[i] == '/' && src[i + 1] == '*' -> {
                        i += 2
                        var depth = 1
                        while (i + 1 < n && depth > 0) {
                            if (src[i] == '/' && src[i + 1] == '*') {
                                depth++; i += 2
                            } else if (src[i] == '*' && src[i + 1] == '/') {
                                depth--; i += 2
                            } else {
                                i++
                            }
                        }
                    }
                    else -> return
                }
            }
        }

        fun readSingleStringLiteral(): Pair<String, Boolean>? {
            // returns Pair(decodedString, hasUnescapedInterpolation)
            if (i >= n || src[i] != '"') return null
            if (i + 2 < n && src[i] == '"' && src[i + 1] == '"' && src[i + 2] == '"') {
                // Triple quoted string
                i += 3
                val start = i
                while (i + 2 < n && !(src[i] == '"' && src[i + 1] == '"' && src[i + 2] == '"')) {
                    i++
                }
                val raw = src.substring(start, i)
                if (i + 2 < n) i += 3
                val hasInterp = Regex("\\$[a-zA-Z_{]").containsMatchIn(raw)
                return raw to hasInterp
            } else {
                i++ // skip opening "
                val sb = StringBuilder()
                var hasInterp = false
                while (i < n && src[i] != '"') {
                    if (src[i] == '\\' && i + 1 < n) {
                        i++
                        when (src[i]) {
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '$' -> sb.append('$')
                            '\'' -> sb.append('\'')
                            'u' -> {
                                if (i + 4 < n) {
                                    val hex = src.substring(i + 1, i + 5)
                                    val code = hex.toIntOrNull(16)
                                    if (code != null) {
                                        sb.append(code.toChar())
                                        i += 4
                                    } else {
                                        sb.append("\\u")
                                    }
                                }
                            }
                            else -> sb.append(src[i])
                        }
                        i++
                    } else {
                        if (src[i] == '$' && i + 1 < n && (src[i + 1].isLetter() || src[i + 1] == '_' || src[i + 1] == '{')) {
                            hasInterp = true
                        }
                        sb.append(src[i])
                        i++
                    }
                }
                if (i < n && src[i] == '"') i++
                return sb.toString() to hasInterp
            }
        }

        fun skipExpressionToCommaOrClose(): String {
            val start = i
            var paren = 0
            var bracket = 0
            var brace = 0
            while (i < n) {
                val c = src[i]
                when {
                    c == '/' && i + 1 < n && src[i + 1] == '/' -> {
                        i += 2; while (i < n && src[i] != '\n') i++
                    }
                    c == '/' && i + 1 < n && src[i + 1] == '*' -> {
                        i += 2
                        var depth = 1
                        while (i + 1 < n && depth > 0) {
                            if (src[i] == '/' && src[i + 1] == '*') { depth++; i += 2 }
                            else if (src[i] == '*' && src[i + 1] == '/') { depth--; i += 2 }
                            else i++
                        }
                    }
                    c == '"' -> { readSingleStringLiteral() }
                    c == '\'' -> {
                        i++
                        if (i < n && src[i] == '\\') i += 2 else i++
                        if (i < n && src[i] == '\'') i++
                    }
                    c == '(' -> { paren++; i++ }
                    c == ')' -> {
                        if (paren == 0 && bracket == 0 && brace == 0) return src.substring(start, i).trim()
                        paren--; i++
                    }
                    c == '[' -> { bracket++; i++ }
                    c == ']' -> { bracket--; i++ }
                    c == '{' -> { brace++; i++ }
                    c == '}' -> { brace--; i++ }
                    c == ',' && paren == 0 && bracket == 0 && brace == 0 -> return src.substring(start, i).trim()
                    else -> i++
                }
            }
            return src.substring(start, i).trim()
        }

        fun parseStringArgExpression(): Pair<String?, String> {
            // returns Pair(resolvedStringOrNull, warningOrEmpty)
            val saved = i
            skipWsAndComments()
            // Handle optional named argument like `en = ` or `enTemplate = `
            val namedMatch = Regex("^([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*").find(src.substring(i, minOf(n, i + 30)))
            if (namedMatch != null) {
                i += namedMatch.value.length
                skipWsAndComments()
            }
            if (i < n && src[i] == '"') {
                val sb = StringBuilder()
                var anyInterp = false
                while (true) {
                    val lit = readSingleStringLiteral() ?: break
                    sb.append(lit.first)
                    if (lit.second) anyInterp = true
                    skipWsAndComments()
                    if (i < n && src[i] == '+') {
                        i++
                        skipWsAndComments()
                        if (i < n && src[i] == '"') {
                            continue
                        } else {
                            // Concatenating with a non-literal expression!
                            val rest = skipExpressionToCommaOrClose()
                            return null to "Concatenation with non-literal: \"${sb.toString().take(40)}\" + $rest"
                        }
                    } else {
                        break
                    }
                }
                skipWsAndComments()
                if (i < n && (src[i] == ',' || src[i] == ')')) {
                    if (anyInterp) {
                        return sb.toString() to "String interpolation inside tr/trFormat: \"${sb.toString().take(80)}\""
                    }
                    return sb.toString() to ""
                }
            }
            // Fallback: non-literal expression
            i = saved
            val expr = skipExpressionToCommaOrClose()
            return null to "Non-literal en argument: $expr"
        }

        val allowedNonLiterals = setOf(
            "canonicalEnEl.first",
            "nameEn",
            "en",
            "enTemplate",
            "actionEn",
            "en[idx]",
            "DEFAULT_SAVINGS_CATEGORY_NAME_EN"
        )

        val isGuideOrLegal = relName.endsWith("GuideContentData.kt") || relName.endsWith("LegalDocumentsDialog.kt")

        while (i < n) {
            val c = src[i]
            // Skip comments and string literals outside calls
            if (c == '/' && i + 1 < n && src[i + 1] == '/') {
                i += 2; while (i < n && src[i] != '\n') i++; continue
            }
            if (c == '/' && i + 1 < n && src[i + 1] == '*') {
                i += 2
                var depth = 1
                while (i + 1 < n && depth > 0) {
                    if (src[i] == '/' && src[i + 1] == '*') { depth++; i += 2 }
                    else if (src[i] == '*' && src[i + 1] == '/') { depth--; i += 2 }
                    else i++
                }
                continue
            }
            if (c == '"') {
                readSingleStringLiteral(); continue
            }

            if (c.isLetter() || c == '_') {
                val idStart = i
                while (i < n && (src[i].isLetterOrDigit() || src[i] == '_')) i++
                val ident = src.substring(idStart, i)

                // Check previous non-whitespace token to avoid matching `fun tr(` or `other.tr(`
                var p = idStart - 1
                while (p >= 0 && src[p].isWhitespace()) p--
                val prevChar = if (p >= 0) src[p] else ' '
                val prevWord = if (p >= 0 && (src[p].isLetterOrDigit() || src[p] == '_')) {
                    var ws = p
                    while (ws >= 0 && (src[ws].isLetterOrDigit() || src[ws] == '_')) ws--
                    src.substring(ws + 1, p + 1)
                } else ""

                val afterId = i
                skipWsAndComments()
                val isCall = i < n && src[i] == '('

                val isTrCall = isCall && (ident == "tr" || ident == "trFormat") && prevWord != "fun" &&
                    (prevChar != '.' || (idStart >= 13 && src.substring(maxOf(0, idStart - 13), idStart).endsWith("Localization.")))
                val isLocalTCall = isCall && isGuideOrLegal && ident == "t" && prevWord != "fun" && prevChar != '.'

                if (isTrCall || isLocalTCall) {
                    val lineNum = src.substring(0, idStart).count { it == '\n' } + 1
                    i++ // skip '('
                    if (isTrCall) {
                        // Skip first argument (lang / langCode)
                        skipExpressionToCommaOrClose()
                        if (i < n && src[i] == ',') {
                            i++ // skip ','
                            val (enStr, warn) = parseStringArgExpression()
                            if (warn.isNotEmpty()) {
                                val expr = warn.removePrefix("Non-literal en argument: ").trim()
                                if (expr !in allowedNonLiterals) {
                                    warnings.add("$relName:$lineNum ($ident): $warn")
                                }
                            } else if (enStr != null) {
                                keys.add(enStr)
                            }
                        }
                    } else {
                        // Local t(en, el)
                        val (enStr, warn) = parseStringArgExpression()
                        if (warn.isNotEmpty()) {
                            val expr = warn.removePrefix("Non-literal en argument: ").trim()
                            if (expr !in allowedNonLiterals) {
                                warnings.add("$relName:$lineNum (t): $warn")
                            }
                        } else if (enStr != null) {
                            keys.add(enStr)
                        }
                    }
                    continue
                } else {
                    i = afterId
                }
            } else {
                i++
            }
        }

        return ScanResult(keys, warnings)
    }
}
