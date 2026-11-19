package com.example.util

import com.example.data.entity.ExpenseCategory
import java.util.Locale
import java.util.UUID

data class PendingBankTransaction(
    val id: String = UUID.randomUUID().toString(),
    val bankName: String,
    val merchantName: String,
    val amount: Double,
    val timestamp: Long = System.currentTimeMillis(),
    val rawText: String = "",
    val householdId: String = ""
)

object BankNotificationParser {

    /**
     * Strictly whitelisted banking apps:
     * - Eurobank
     * - Alpha Bank
     * - Εθνική Τράπεζα (NBG)
     * - Τράπεζα Πειραιώς (Piraeus bank)
     * - Revolut
     * - Payzy
     * - Viber pay
     * - PayPal
     * - Snappi
     */
    private val ALLOWED_BANK_PACKAGES = mapOf(
        // Eurobank
        "com.EurobankEFG" to "Eurobank",
        "gr.eurobank.mobile.app" to "Eurobank",
        "gr.eurobank.mobile" to "Eurobank",
        "gr.eurobank.cards" to "Eurobank",
        "gr.eurobank.smartwallet" to "Eurobank",
        // Google Wallet / Google Pay
        "com.google.android.apps.walletnfcrel" to "Google Pay",
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        // Alpha Bank
        "com.mobileloft.alpha.droid" to "Alpha Bank",
        "gr.alphabank.mobilebanking" to "Alpha Bank",
        "gr.alpha.bonusextra" to "Alpha Bank",
        "gr.alpha.mobilebanking" to "Alpha Bank",
        "gr.alpha.mobile" to "Alpha Bank",
        "gr.alphabank.wallet" to "Alpha Bank",
        // NBG (Εθνική Τράπεζα)
        "mbanking.NBG" to "Εθνική Τράπεζα",
        "gr.nbg.mobilebanking" to "Εθνική Τράπεζα",
        "gr.nbg.mobile" to "Εθνική Τράπεζα",
        "gr.nbg.cards" to "Εθνική Τράπεζα",
        "gr.nbg.pay" to "Εθνική Τράπεζα",
        // Piraeus (Τράπεζα Πειραιώς)
        "gr.winbank.mobile" to "Τράπεζα Πειραιώς",
        "gr.winbank.mobile.redesign" to "Τράπεζα Πειραιώς",
        "gr.piraeusbank.winbank" to "Τράπεζα Πειραιώς",
        "gr.piraeusbank.mobile" to "Τράπεζα Πειραιώς",
        "gr.piraeusbank.pay" to "Τράπεζα Πειραιώς",
        // Revolut
        "com.revolut.revolut" to "Revolut",
        "com.revolut.business" to "Revolut",
        // Payzy
        "gr.cosmote.payzy" to "Payzy",
        "com.payzy.app" to "Payzy",
        // Viva Wallet
        "com.vivawallet.app" to "Viva Wallet",
        "gr.vivawallet.app" to "Viva Wallet",
        "com.vivawallet.merchant" to "Viva Wallet",
        // Optima Bank
        "gr.optimabank.mobile" to "Optima Bank",
        "gr.optimabank.mobilebanking" to "Optima Bank",
        // Attica Bank
        "gr.atticabank.mobile" to "Attica Bank",
        "gr.atticabank.mobilebanking" to "Attica Bank",
        // Snappi
        "eu.snappi.app" to "Snappi",
        "gr.snappi.mobile" to "Snappi",
        // PayPal
        "com.paypal.android.p2pmobile" to "PayPal",
        // Viber Pay
        "com.viber.voip" to "Viber Pay"
    )

    // Known popular brands and merchant keywords mapping to their canonical name & category domain
    data class KnownBrand(
        val canonicalName: String,
        val domain: String, // "SUPERMARKET", "HOME", "FOOD", "COFFEE", "FUEL", "CLOTHING", "BILLS", "ENTERTAINMENT", "HEALTH", "PETS"
        val aliases: List<String>
    )

    private val KNOWN_BRANDS = listOf(
        // Supermarket / Groceries
        KnownBrand("Lidl", "SUPERMARKET", listOf("lidl", "λιντλ")),
        KnownBrand("Σκλαβενίτης", "SUPERMARKET", listOf("σκλαβενιτης", "σκλαβενίτης", "sklavenitis")),
        KnownBrand("ΑΒ Βασιλόπουλος", "SUPERMARKET", listOf("βασιλοπουλος", "βασιλόπουλος", "ab vassilopoulos", "ab basilopoulos", "ab market")),
        KnownBrand("Μασούτης", "SUPERMARKET", listOf("μασουτης", "μασούτης", "masoutis")),
        KnownBrand("My Market", "SUPERMARKET", listOf("my market", "mymarket")),
        KnownBrand("Γαλαξίας", "SUPERMARKET", listOf("γαλαξιας", "γαλαξίας", "galaxias")),
        KnownBrand("Market In", "SUPERMARKET", listOf("market in", "marketin")),
        KnownBrand("Κρητικός", "SUPERMARKET", listOf("κρητικος", "κρητικός", "kritikos")),
        KnownBrand("Bazaar", "SUPERMARKET", listOf("bazaar")),

        // Home / Furniture / Electronics
        KnownBrand("IKEA", "HOME", listOf("ikea", "ικεα")),
        KnownBrand("Leroy Merlin", "HOME", listOf("leroy merlin", "leroymerlin", "λεροι μερλιν")),
        KnownBrand("Praktiker", "HOME", listOf("praktiker", "πρακτικερ")),
        KnownBrand("Jysk", "HOME", listOf("jysk", "γιουσκ")),
        KnownBrand("Zara Home", "HOME", listOf("zara home")),
        KnownBrand("Vicko", "HOME", listOf("vicko", "βικο")),
        KnownBrand("Κωτσόβολος", "HOME", listOf("κωτσοβολος", "κωτσόβολος", "kotsovolos")),
        KnownBrand("Πλαίσιο", "HOME", listOf("πλαισιο", "πλαίσιο", "plaisio")),
        KnownBrand("Public", "HOME", listOf("public", "παμπλικ")),
        KnownBrand("Media Markt", "HOME", listOf("media markt", "mediamarkt")),

        // Food / Dining / Delivery
        KnownBrand("Wolt", "FOOD", listOf("wolt", "βολτ")),
        KnownBrand("e-food", "FOOD", listOf("e-food", "efood", "ιφουντ")),
        KnownBrand("Box", "FOOD", listOf("box delivery")),
        KnownBrand("McDonald's", "FOOD", listOf("mcdonalds", "mcdonald's", "μακντοναλντς")),
        KnownBrand("Goody's", "FOOD", listOf("goodys", "goody's", "γκουντις")),
        KnownBrand("Everest", "FOOD", listOf("everest", "εβερεστ")),
        KnownBrand("Γρηγόρης", "FOOD", listOf("γρηγορης", "γρηγόρης", "gregorys")),
        KnownBrand("Pizza Fan", "FOOD", listOf("pizza fan", "pizzafan")),
        KnownBrand("Domino's", "FOOD", listOf("dominos", "domino's")),
        KnownBrand("KFC", "FOOD", listOf("kfc")),

        // Coffee
        KnownBrand("Coffee Island", "COFFEE", listOf("coffee island")),
        KnownBrand("Mikel", "COFFEE", listOf("mikel", "μαικελ")),
        KnownBrand("Starbucks", "COFFEE", listOf("starbucks", "σταρμπακς")),

        // Fuel & Transport
        KnownBrand("Shell", "FUEL", listOf("shell", "σελ")),
        KnownBrand("EKO", "FUEL", listOf("eko", "εκο")),
        KnownBrand("BP", "FUEL", listOf("bp")),
        KnownBrand("Avin", "FUEL", listOf("avin", "αβιν")),
        KnownBrand("Revoil", "FUEL", listOf("revoil")),
        KnownBrand("Elin", "FUEL", listOf("elin", "ελιν")),
        KnownBrand("Αττική Οδός", "FUEL", listOf("αττικη οδος", "αττική οδός", "attiki odos")),

        // Clothing & Shopping
        KnownBrand("Zara", "CLOTHING", listOf("zara", "ζαρα")),
        KnownBrand("H&M", "CLOTHING", listOf("h&m", "hm", "η&μ")),
        KnownBrand("Pull&Bear", "CLOTHING", listOf("pull&bear", "pull and bear")),
        KnownBrand("Bershka", "CLOTHING", listOf("bershka")),
        KnownBrand("Stradivarius", "CLOTHING", listOf("stradivarius")),
        KnownBrand("Massimo Dutti", "CLOTHING", listOf("massimo dutti")),
        KnownBrand("Nike", "CLOTHING", listOf("nike", "ναικι")),
        KnownBrand("Adidas", "CLOTHING", listOf("adidas", "αντιντας")),
        KnownBrand("Intersport", "CLOTHING", listOf("intersport")),
        KnownBrand("Cosmos Sport", "CLOTHING", listOf("cosmos sport", "cosmossport")),
        KnownBrand("Sephora", "CLOTHING", listOf("sephora")),
        KnownBrand("Hondos Center", "CLOTHING", listOf("hondos center", "hondos")),

        // Entertainment & Streaming
        KnownBrand("Netflix", "ENTERTAINMENT", listOf("netflix", "νετφλιξ")),
        KnownBrand("Spotify", "ENTERTAINMENT", listOf("spotify")),
        KnownBrand("Disney+", "ENTERTAINMENT", listOf("disney+", "disney plus", "disney")),
        KnownBrand("Steam", "ENTERTAINMENT", listOf("steam")),
        KnownBrand("PlayStation", "ENTERTAINMENT", listOf("playstation", "psn")),

        // Utilities & Subscriptions
        KnownBrand("Cosmote", "BILLS", listOf("cosmote", "κοσμοτε")),
        KnownBrand("Vodafone", "BILLS", listOf("vodafone", "βονταφον")),
        KnownBrand("Nova", "BILLS", listOf("nova", "νοβα")),
        KnownBrand("ΔΕΗ", "BILLS", listOf("δεη", "ppc", "dei")),
        KnownBrand("Protergia", "BILLS", listOf("protergia", "προτεργια")),
        KnownBrand("Elpedison", "BILLS", listOf("elpedison", "ελπεντισον")),
        KnownBrand("Ήρων", "BILLS", listOf("heron", "ηρων", "ήρων")),
        KnownBrand("ΕΥΔΑΠ", "BILLS", listOf("ευδαπ", "eydap")),
        KnownBrand("ΕΥΑΘ", "BILLS", listOf("ευαθ", "eyath")),
        KnownBrand("Apple", "BILLS", listOf("apple.com", "apple", "itunes")),
        KnownBrand("Google", "BILLS", listOf("google play", "google")),

        // Pets
        KnownBrand("Pet City", "PETS", listOf("pet city", "petcity"))
    )

    fun getBankName(packageName: String): String? {
        ALLOWED_BANK_PACKAGES[packageName]?.let { return it }
        val pkg = packageName.lowercase(Locale.ROOT)
        return when {
            pkg.contains("eurobank") -> "Eurobank"
            pkg.contains("walletnfcrel") || pkg.contains("paisa.user") -> "Google Pay"
            pkg.contains("winbank") || pkg.contains("piraeus") -> "Τράπεζα Πειραιώς"
            pkg.contains("alphabank") || pkg.startsWith("gr.alpha.") || pkg.contains("mobileloft.alpha") -> "Alpha Bank"
            pkg.contains("nbg") -> "Εθνική Τράπεζα"
            pkg.contains("revolut") -> "Revolut"
            pkg.contains("payzy") -> "Payzy"
            pkg.contains("vivawallet") || pkg.contains("viva.wallet") -> "Viva Wallet"
            pkg.contains("optimabank") || pkg.contains("optima") -> "Optima Bank"
            pkg.contains("atticabank") || pkg.contains("attica") -> "Attica Bank"
            pkg.contains("snappi") -> "Snappi"
            pkg.contains("paypal") -> "PayPal"
            pkg == "com.viber.voip" -> "Viber Pay"
            else -> null
        }
    }

    fun isBankPackage(packageName: String): Boolean {
        return getBankName(packageName) != null || packageName == "com.google.android.gms"
    }

    fun parse(
        packageName: String,
        title: String,
        text: String,
        timestamp: Long = System.currentTimeMillis()
    ): PendingBankTransaction? {
        val combined = "$title $text".trim()
        if (combined.isBlank()) return null

        val lower = combined.lowercase(Locale.ROOT)

        // Google Play Services ("com.google.android.gms") sometimes posts Google Wallet tap-to-pay receipts
        val bankName = if (packageName == "com.google.android.gms") {
            val isWalletTx = (lower.contains("με την κάρτα") || lower.contains("με την καρτα") ||
                lower.contains("with card") || lower.contains("with your") ||
                lower.contains("google pay") || lower.contains("google wallet") ||
                lower.contains("πορτοφόλι google") || lower.contains("mastercard") || lower.contains("visa")) &&
                (lower.contains("€") || lower.contains("eur") || lower.contains("ευρώ") || lower.contains("ευρω"))
            if (!isWalletTx) return null
            "Google Pay"
        } else {
            getBankName(packageName) ?: return null
        }

        // Reject incoming funds/credits so only actual expenses/debits are tracked
        val incomingKeywords = listOf(
            "λάβατε", "λαβατε", "κατατέθηκαν", "κατατεθηκαν", "κατάθεση", "καταθεση",
            "πίστωση", "πιστωση", "πιστώθηκε", "πιστωθηκε", "πληρωθήκατε", "πληρωθηκατε",
            "received", "credited", "deposit", "incoming payment", "salary"
        )
        if (incomingKeywords.any { lower.contains(it) }) {
            return null
        }

        // For Viber Pay, verify it's an actual payment/financial transaction notification and not a normal chat
        if (packageName == "com.viber.voip") {
            val hasViberPayKeywords = listOf(
                "viber pay", "pay", "πληρωμή", "πληρωμη", "συναλλαγή", "συναλλαγη",
                "χρέωση", "χρεωση", "μεταφορά", "μεταφορα", "payment", "money", "ευρώ", "ευρω", "€"
            ).any { lower.contains(it) }
            if (!hasViberPayKeywords) return null
        }

        // Must have transaction-related keywords or financial indicators
        val hasTransactionKeywords = hasTransactionKeywords(combined)
        if (!hasTransactionKeywords && !packageName.contains("revolut") && !packageName.contains("paypal") && bankName != "Google Pay") {
            return null
        }

        // Extract transaction amount (ignoring any number after "υπόλοιπο" or "balance")
        val amount = extractAmount(combined) ?: return null
        if (amount <= 0.05) return null

        // Extract merchant / recipient / brand (prioritize body over title so titles like "Αγορά/Πληρωμή σε POS" don't hijack "σε POS")
        val merchant = extractMerchant(body = text, title = title, combined = combined, bankName = bankName)

        return PendingBankTransaction(
            bankName = bankName,
            merchantName = merchant.ifBlank { "Συναλλαγή $bankName" },
            amount = amount,
            timestamp = timestamp,
            rawText = combined
        )
    }

    private fun hasTransactionKeywords(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        val keywords = listOf(
            "αγορά", "αγορα", "συναλλαγή", "συναλλαγη", "χρέωση", "χρεωση",
            "πληρωμή", "πληρωμη", "εγκρίθηκε", "εγκριθηκε", "κάρτα", "καρτα",
            "έξοδο", "εξοδο", "μεταφορά", "μεταφορα",
            "e-commerce", "ecommerce", "ηλεκτρονικά", "ηλεκτρονικα", "online",
            "purchase", "spent", "transaction", "payment", "card charged", "paid", "sent",
            "money", "debit", "approved", "€", "eur", "ευρώ", "ευρω"
        )
        return keywords.any { lower.contains(it) }
    }

    /**
     * Requirement 3: Do NOT consider as expense any number that is after the word
     * "υπόλοιπο", "Υπόλοιπο", or "balance".
     * Any number that appears after "υπόλοιπο" or "balance" (up to end of sentence or clause)
     * is completely eliminated so it can never be parsed as an expense.
     */
    private fun removeBalanceClauses(text: String): String {
        // Matches "υπόλοιπο" or "balance" followed by any characters until a number, and that number
        // Examples: "υπόλοιπο της κάρτας σας είναι 350.00 EUR", "Balance: €450.00", "διαθέσιμο υπόλοιπο: 120.50€"
        var clean = text.replace(
            Regex("(?i)(?:νέο\\s+|διαθέσιμο\\s+|λογιστικό\\s+)?(?:υπόλοιπο|υπολοιπο|υπολοίπου|υπολοιπου|balance|bal\\.)[^.,;!\\n]*?(?:[0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]+)(?:[.,][0-9]{1,2})?\\s*(?:€|EUR|eur|ευρώ|ευρω)?"),
            " "
        )
        // Also if text contains balance and after it there is a number
        clean = clean.replace(
            Regex("(?i)(?:υπόλοιπο|υπολοιπο|balance)[^0-9]*(?:[0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]+)(?:[.,][0-9]{1,2})?\\s*(?:€|EUR|eur|ευρώ|ευρω)?"),
            " "
        )
        return clean
    }

    private fun parseAmountNumber(raw: String): Double? {
        val s = raw.trim()
        val normalized = when {
            s.matches(Regex("\\d{1,3}(\\.\\d{3})+,\\d{1,2}")) -> s.replace(".", "").replace(",", ".")
            s.matches(Regex("\\d{1,3}(,\\d{3})+\\.\\d{1,2}")) -> s.replace(",", "")
            else -> s.replace(',', '.')
        }
        return normalized.toDoubleOrNull()
    }

    private fun extractAmount(text: String): Double? {
        val cleaned = removeBalanceClauses(text)
        val numGroup = "((?:[0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]+)(?:[.,][0-9]{1,2})?)"

        val amountPatterns = listOf(
            Regex("(?:€|EUR|eur)\\s*$numGroup"),
            Regex("$numGroup\\s*(?:€|EUR|eur|ευρώ|ευρω)"),
            Regex("(?i)(?:ποσό|ποσο|amount|sum)[:\\s]+$numGroup"),
            Regex("((?:[0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]+)[.,][0-9]{2})")
        )

        for (pattern in amountPatterns) {
            val match = pattern.find(cleaned)
            if (match != null) {
                val parsed = parseAmountNumber(match.groupValues[1])
                if (parsed != null && parsed > 0.05 && parsed < 50000.0) {
                    return parsed
                }
            }
        }
        return null
    }

    private fun extractMerchant(
        body: String,
        title: String = "",
        combined: String = "$title $body".trim(),
        bankName: String = ""
    ): String {
        // 0. Google Pay / Google Wallet notifications put the merchant directly in the notification title
        // e.g. Title: "SHELL MYRTEA THESSALON" | Text: "18,00 € με την κάρτα Eurobank Classic Ma..."
        if (bankName == "Google Pay") {
            val cleanTitle = title.trim()
                .replace(Regex("^(?:PAYPAL\\s*\\*|PP\\*|GOOGLE\\s*\\*|AMZN\\s*\\*|AMAZON\\s*\\*)\\s*", RegexOption.IGNORE_CASE), "")
                .trim()
            val titleLower = cleanTitle.lowercase(Locale.ROOT)
            val isGenericWalletTitle = titleLower in setOf(
                "google pay", "google wallet", "πορτοφόλι google", "πορτοφολι google",
                "πληρωμή", "πληρωμη", "συναλλαγή", "συναλλαγη", "αγορά", "αγορα",
                "payment", "purchase", "notification"
            )
            val looksLikeAmount = Regex("^[0-9.,]+\\s*(?:€|eur|ευρώ)?$", RegexOption.IGNORE_CASE).matches(cleanTitle)
            if (cleanTitle.length in 2..60 && !isGenericWalletTitle && !looksLikeAmount) {
                return cleanTitle
            }
        }

        // 1. Pattern-based extraction from notification phrasing
        // Search body first, then combined text so generic headers like "Αγορά/Πληρωμή σε POS" don't override "στην εταιρεία ..."
        val terminator = "(?=\\s+(?:με|την|στις|on|with|\\d{1,2}/\\d{1,2}|\\d{1,2}:\\d{2})|[,;!\\n]|(?<!\\b[A-ZΑ-ΩΆ-Ώ])\\.(?!\\s*[A-ZΑ-ΩΆ-Ώ]\\.)|\\z)"
        val merchantPatterns = listOf(
            Regex("(?:στο κατάστημα|στο|στην|στη|σε(?!\\s+POS\\b)|προς|at|in|to)\\s+(?:την\\s+|τον\\s+|το\\s+)?(?:εταιρεία|εταιρία|επιχείρηση|επιχειρηση|επιχ/ση|έμπορο|εμπορο|κατάστημα|καταστημα|merchant)?\\s*[:\\s]*([a-zA-Zα-ωΑ-Ωά-ώΆ-Ώ0-9\\s&.*#_/-]+?)$terminator", RegexOption.IGNORE_CASE),
            Regex("(?:επιχείρηση|επιχειρηση|έμπορος|εμπορος|merchant|προς)[:\\s]+([a-zA-Zα-ωΑ-Ωά-ώΆ-Ώ0-9\\s&.*#_/-]+?)$terminator", RegexOption.IGNORE_CASE),
            Regex("(?:αγορά|αγορα|πληρωμή|πληρωμη|purchase|spent|payment)\\s+(?:ποσού|ποσου|of)?\\s*[0-9.,€eurEUR\\s]+\\s*(?:σε|στο|στην|στη|at|to)?\\s*(?:εταιρεία|εταιρία|επιχείρηση|επιχειρηση)?\\s*([a-zA-Zα-ωΑ-Ωά-ώΆ-Ώ0-9\\s&.*#_/-]+?)$terminator", RegexOption.IGNORE_CASE)
        )

        val textsToSearch = listOf(body, combined).filter { it.isNotBlank() }.distinct()
        for (sourceText in textsToSearch) {
            for (pattern in merchantPatterns) {
                val match = pattern.find(sourceText)
                if (match != null) {
                    var candidate = match.groupValues[1].trim()

                    // Strip leading prefixes like "εταιρεία", "επιχείρηση"
                    candidate = candidate.replace(Regex("^(?:εταιρεία|εταιρία|επιχείρηση|επιχειρηση|έμπορο|εμπορο|κατάστημα|καταστημα)\\s+", RegexOption.IGNORE_CASE), "").trim()

                    // Handle payment aggregators (e.g. PAYPAL *NETFLIX -> NETFLIX, GOOGLE *XYZ -> XYZ)
                    candidate = candidate.replace(Regex("^(?:PAYPAL\\s*\\*|PP\\*|GOOGLE\\s*\\*|AMZN\\s*\\*|AMAZON\\s*\\*)\\s*", RegexOption.IGNORE_CASE), "").trim()

                    // Strip corporate/domain suffixes (e.g. "COM DE", "COM GR", "COM", "LTD", "SA", "ΑΕ")
                    candidate = candidate.replace(Regex("\\s+(?:COM\\s+[A-Z]{2}|COM|GR|LTD|SA|LLC|INC|IKE|ΕΠΕ|ΑΕ)$", RegexOption.IGNORE_CASE), "").trim()

                    if (candidate.length in 2..45 &&
                        !candidate.contains("κάρτα", ignoreCase = true) &&
                        !candidate.contains("καρτα", ignoreCase = true) &&
                        !candidate.contains("card", ignoreCase = true) &&
                        !candidate.contains("υπόλοιπο", ignoreCase = true) &&
                        !candidate.contains("balance", ignoreCase = true) &&
                        !candidate.contains("κάνατε", ignoreCase = true) &&
                        !candidate.equals("POS", ignoreCase = true) &&
                        !candidate.equals("ετ", ignoreCase = true) &&
                        !candidate.equals("ετ.", ignoreCase = true)
                    ) {
                        return candidate
                    }
                }
            }
        }

        // 2. Fallback: Check known popular brands directly across full notification text
        val lower = combined.lowercase(Locale.ROOT)
        for (brand in KNOWN_BRANDS) {
            for (alias in brand.aliases) {
                val pattern = "(?:^|[^\\p{L}\\p{Nd}])" + Regex.escape(alias) + "(?:$|[^\\p{L}\\p{Nd}])"
                if (Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(lower)) {
                    return brand.canonicalName
                }
            }
        }

        return ""
    }

    /**
     * Requirement 5: Categorize recognized notification merchant / brand into matching ExpenseCategory.
     * If the merchant cannot be categorized, returns null (so description field remains blank).
     */
    fun categorizeMerchant(
        merchant: String,
        rawText: String,
        categories: List<ExpenseCategory>
    ): ExpenseCategory? {
        val search = "$merchant $rawText".lowercase(Locale.ROOT)

        // 1. Check known brand domains first
        val matchedBrand = KNOWN_BRANDS.firstOrNull { brand ->
            search.contains(brand.canonicalName.lowercase(Locale.ROOT)) ||
            brand.aliases.any { alias ->
                val p = "(?:^|[^\\p{L}\\p{Nd}])" + Regex.escape(alias) + "(?:$|[^\\p{L}\\p{Nd}])"
                Regex(p, RegexOption.IGNORE_CASE).containsMatchIn(search)
            }
        }

        if (matchedBrand != null) {
            val targetDomain = matchedBrand.domain
            val found = when (targetDomain) {
                "SUPERMARKET" -> categories.find { it.name.contains("Σούπερ", ignoreCase = true) || it.name.contains("Super", ignoreCase = true) || it.name.contains("Τρόφιμα", ignoreCase = true) }
                "HOME" -> categories.find { it.name.contains("Σπίτι", ignoreCase = true) || it.name.contains("Έπιπλα", ignoreCase = true) || it.name.contains("Home", ignoreCase = true) || it.name.contains("Σπιτιού", ignoreCase = true) }
                "FOOD" -> categories.find { it.name.contains("Φαγητό", ignoreCase = true) || it.name.contains("Food", ignoreCase = true) || it.name.contains("Εστίαση", ignoreCase = true) }
                "COFFEE" -> categories.find { it.name.contains("Καφέ", ignoreCase = true) || it.name.contains("Coffee", ignoreCase = true) }
                    ?: categories.find { it.name.contains("Φαγητό", ignoreCase = true) }
                "FUEL" -> categories.find { it.name.contains("Καύσιμα", ignoreCase = true) || it.name.contains("Μετακινήσεις", ignoreCase = true) || it.name.contains("Βενζίνη", ignoreCase = true) || it.name.contains("Αυτοκίνητο", ignoreCase = true) }
                "CLOTHING" -> categories.find { it.name.contains("Ρούχα", ignoreCase = true) || it.name.contains("Ένδυση", ignoreCase = true) || it.name.contains("Shopping", ignoreCase = true) }
                "ENTERTAINMENT" -> categories.find { it.name.contains("Διασκέδαση", ignoreCase = true) || it.name.contains("Ψυχαγωγία", ignoreCase = true) || it.name.contains("Entertainment", ignoreCase = true) }
                    ?: categories.find { it.name.contains("Συνδρομές", ignoreCase = true) || it.name.contains("Λογαριασμ", ignoreCase = true) || it.name.contains("Bills", ignoreCase = true) }
                "BILLS" -> categories.find { it.name.contains("Συνδρομές", ignoreCase = true) || it.name.contains("Τηλέφων", ignoreCase = true) || it.name.contains("Ρεύμα", ignoreCase = true) || it.name.contains("Bills", ignoreCase = true) || it.name.contains("Λογαριασμ", ignoreCase = true) }
                    ?: categories.find { it.name.contains("Διασκέδαση", ignoreCase = true) || it.name.contains("Ψυχαγωγία", ignoreCase = true) }
                "PETS" -> categories.find { it.name.contains("Γατι", ignoreCase = true) || it.name.contains("Κατοικίδια", ignoreCase = true) || it.name.contains("Pets", ignoreCase = true) }
                else -> null
            }
            if (found != null) return found
        }

        // 2. Check against custom categories and their voiceKeywords
        for (cat in categories) {
            val kwList = cat.voiceKeywords.split(",").map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() }
            val catName = cat.name.trim().lowercase(Locale.ROOT)
            val allKeywords = kwList + catName

            for (kw in allKeywords) {
                if (kw.length >= 3) {
                    val pattern = "(?:^|[^\\p{L}\\p{Nd}])" + Regex.escape(kw) + "(?:$|[^\\p{L}\\p{Nd}])"
                    if (Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(search)) {
                        return cat
                    }
                }
            }
        }

        // Not recognized or cannot be categorized -> null
        return null
    }
}
