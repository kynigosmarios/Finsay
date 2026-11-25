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
    val householdId: String = "",
    val isIncome: Boolean = false
)

object BankNotificationParser {

    /**
     * Explicit blacklist of non-bank apps (messengers, social media, email, shopping, delivery,
     * browsers, SMS, travel, rideshare) so marketing/promo/chat notifications with currency symbols
     * (e.g. Temu promos on Viber, Gmail newsletters, Skroutz offers) are NEVER treated as bank alerts.
     */
    private val BLOCKED_EXACT_PACKAGES = setOf(
        "com.viber.voip",
        "com.whatsapp",
        "com.whatsapp.w4b",
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "com.facebook.orca",
        "com.facebook.katana",
        "com.facebook.lite",
        "com.instagram.android",
        "com.snapchat.android",
        "com.zhiliaoapp.musically",
        "com.ss.android.ugc.trill",
        "com.twitter.android",
        "com.linkedin.android",
        "com.discord",
        "org.thoughtcrime.securesms",
        "com.skype.raider",
        "com.slack",
        "com.microsoft.teams",
        "com.google.android.gm",
        "com.microsoft.office.outlook",
        "com.yahoo.mobile.client.android.mail",
        "com.samsung.android.email.provider",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "com.android.mms",
        "com.einnovation.temu",
        "com.zzkko", // Shein
        "com.alibaba.aliexpresshd",
        "com.amazon.mShop.android.shopping",
        "com.ebay.mobile",
        "gr.skroutz.app",
        "com.efood.android",
        "com.wolt.android",
        "com.booking",
        "com.airbnb.android",
        "com.ubercab",
        "ee.mtakso.client",
        "com.android.chrome",
        "org.mozilla.firefox",
        "com.opera.browser",
        "com.microsoft.emmx",
        "com.brave.browser",
        "com.google.android.youtube",
        "com.spotify.music",
        "com.netflix.mediaclient",
        "com.android.vending"
    )

    private val BLOCKED_PACKAGE_TOKENS = listOf(
        "viber", "whatsapp", "telegram", "messenger", "facebook", "instagram", "tiktok",
        "snapchat", "discord", "twitter", "linkedin", "reddit", "pinterest", "skype", "slack",
        "gmail", "outlook", "email", "mail", "messaging", ".mms", "sms",
        "temu", "shein", "aliexpress", "amazon", "ebay", "skroutz", "efood", "wolt",
        "booking", "airbnb", "uber", "bolt", "vinted", "zalando", "shein", "lidl",
        "chrome", "firefox", "opera", "browser", "youtube", "netflix", "spotify", "news",
        "weather", "game", "shopping", "delivery", "marketplace", "coupons", "deals"
    )

    /**
     * Global banking & payment wallet apps (Greece, Europe, UK, USA, International).
     * Never includes general chat apps like Viber/WhatsApp/Telegram.
     */
    private val ALLOWED_BANK_PACKAGES = mapOf(
        // Google Wallet / Google Pay
        "com.google.android.apps.walletnfcrel" to "Google Pay",
        "com.google.android.apps.nbu.paisa.user" to "Google Pay",
        "com.samsung.android.spay" to "Samsung Wallet",

        // Greece
        "com.EurobankEFG" to "Eurobank",
        "gr.eurobank.mobile.app" to "Eurobank",
        "gr.eurobank.mobile" to "Eurobank",
        "gr.eurobank.cards" to "Eurobank",
        "gr.eurobank.smartwallet" to "Eurobank",
        "com.mobileloft.alpha.droid" to "Alpha Bank",
        "gr.alphabank.mobilebanking" to "Alpha Bank",
        "gr.alpha.bonusextra" to "Alpha Bank",
        "gr.alpha.mobilebanking" to "Alpha Bank",
        "gr.alpha.mobile" to "Alpha Bank",
        "gr.alphabank.wallet" to "Alpha Bank",
        "mbanking.NBG" to "Εθνική Τράπεζα",
        "gr.nbg.mobilebanking" to "Εθνική Τράπεζα",
        "gr.nbg.mobile" to "Εθνική Τράπεζα",
        "gr.nbg.cards" to "Εθνική Τράπεζα",
        "gr.nbg.pay" to "Εθνική Τράπεζα",
        "gr.winbank.mobile" to "Τράπεζα Πειραιώς",
        "gr.winbank.mobile.redesign" to "Τράπεζα Πειραιώς",
        "gr.piraeusbank.winbank" to "Τράπεζα Πειραιώς",
        "gr.piraeusbank.mobile" to "Τράπεζα Πειραιώς",
        "gr.piraeusbank.pay" to "Τράπεζα Πειραιώς",
        "gr.cosmote.payzy" to "Payzy",
        "com.payzy.app" to "Payzy",
        "com.vivawallet.app" to "Viva Wallet",
        "gr.vivawallet.app" to "Viva Wallet",
        "com.vivawallet.merchant" to "Viva Wallet",
        "gr.optimabank.mobile" to "Optima Bank",
        "gr.optimabank.mobilebanking" to "Optima Bank",
        "gr.atticabank.mobile" to "Attica Bank",
        "gr.atticabank.mobilebanking" to "Attica Bank",
        "eu.snappi.app" to "Snappi",
        "gr.snappi.mobile" to "Snappi",

        // Pan-European / Global Neobanks & Digital Wallets
        "com.revolut.revolut" to "Revolut",
        "com.revolut.business" to "Revolut",
        "com.transferwise.android" to "Wise",
        "de.number26.android" to "N26",
        "co.uk.getmondo" to "Monzo",
        "com.starlingbank.android" to "Starling Bank",
        "com.bunq.android" to "bunq",
        "com.imaginecurve.curve.prd" to "Curve",
        "com.paypal.android.p2pmobile" to "PayPal",
        "com.myklarnamobile" to "Klarna",
        "com.moneybookers.skrillpayments" to "Skrill",
        "com.payoneer.android" to "Payoneer",
        "com.monese.monese.live" to "Monese",
        "com.vivid.money" to "Vivid Money",
        "com.trade.republic.app" to "Trade Republic",

        // UK & Ireland
        "com.barclays.android.barclaysmobilebanking" to "Barclays",
        "uk.co.hsbc.hsbcukmobilebanking" to "HSBC",
        "com.grppl.android.shell.CMBlloydsTSB73" to "Lloyds Bank",
        "com.grppl.android.shell.halifax" to "Halifax",
        "com.rbs.mobile.android.natwest" to "NatWest",
        "uk.co.santander.santanderUK" to "Santander",
        "com.aib.mobilebanking" to "AIB",
        "com.bankofireland.mobilebanking" to "Bank of Ireland",

        // USA & Canada
        "com.chase.sig.android" to "Chase",
        "com.infonow.bofa" to "Bank of America",
        "com.wf.wellsfargomobile" to "Wells Fargo",
        "com.citi.citimobile" to "Citi",
        "com.konylabs.capitalone" to "Capital One",
        "com.americanexpress.android.acctsvcs.us" to "American Express",
        "com.venmo" to "Venmo",
        "com.squareup.cash" to "Cash App",
        "com.onedebit.chime" to "Chime",
        "com.usbank.mobilebanking" to "U.S. Bank",
        "com.rbc.mobile.android" to "RBC",
        "com.td" to "TD Bank",
        "com.scotiabank.banking" to "Scotiabank",
        "com.bmo.mobile" to "BMO",

        // Continental Europe
        "com.ing.diba.mbbr2" to "ING",
        "com.ing.mobile" to "ING",
        "com.db.pwcc.dbmobile" to "Deutsche Bank",
        "de.commerzbanking.mobil" to "Commerzbank",
        "com.starfinanz.smob.android.sfinanzstatus" to "Sparkasse",
        "com.dkbcodefactory.banking" to "DKB",
        "net.bnpparibas.mescomptes" to "BNP Paribas",
        "mobi.societegenerale.mobile.lappli" to "Société Générale",
        "fr.creditagricole.androidapp" to "Crédit Agricole",
        "com.boursorama.android.clients" to "BoursoBank",
        "com.bbva.bbvacontigo" to "BBVA",
        "es.lacaixa.mobile.android.newwapicon" to "CaixaBank",
        "es.bancosantander.apps" to "Santander",
        "com.unicredit.mobile" to "UniCredit",
        "com.intesasanpaolo.mobile" to "Intesa Sanpaolo",
        "com.abnamro.nl.mobile.payments" to "ABN AMRO",
        "nl.rabomobiel" to "Rabobank",
        "at.erstebank.george" to "George / Erste",
        "at.raiffeisen.meinelba" to "Raiffeisen",
        "com.nordea.mobilebank" to "Nordea",
        "se.swedbank.mobil" to "Swedbank",
        "cy.com.bankofcyprus.mobile" to "Bank of Cyprus",
        "com.hellenicbank.mobile" to "Hellenic Bank",

        // LatAm / APAC / Global
        "com.nu.production" to "Nubank",
        "com.dbs.sg.dbsmbanking" to "DBS",
        "com.commbank.netbank" to "CommBank",
        "com.anz.android.gomoney" to "ANZ",
        "org.westpac.bank" to "Westpac",
        "au.com.nab.mobile" to "NAB",
        "com.snapwork.hdfc" to "HDFC Bank",
        "com.csam.icici.bank.imobile" to "ICICI Bank",
        "com.sbi.lotusintouch" to "SBI"
    )

    // Known popular brands and merchant keywords mapping to their canonical name & category domain
    data class KnownBrand(
        val canonicalName: String,
        val domain: String,
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
        KnownBrand("Aldi", "SUPERMARKET", listOf("aldi")),
        KnownBrand("Carrefour", "SUPERMARKET", listOf("carrefour")),
        KnownBrand("Tesco", "SUPERMARKET", listOf("tesco")),
        KnownBrand("Sainsbury's", "SUPERMARKET", listOf("sainsbury")),
        KnownBrand("Walmart", "SUPERMARKET", listOf("walmart")),
        KnownBrand("Costco", "SUPERMARKET", listOf("costco")),

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
        KnownBrand("Uber Eats", "FOOD", listOf("uber eats", "ubereats")),
        KnownBrand("Deliveroo", "FOOD", listOf("deliveroo")),
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
        KnownBrand("Uber", "FUEL", listOf("uber")),
        KnownBrand("Bolt", "FUEL", listOf("bolt")),

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
        KnownBrand("Amazon", "CLOTHING", listOf("amazon", "amzn")),

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

    private fun isBlockedNonBankPackage(packageName: String): Boolean {
        val pkg = packageName.trim().lowercase(Locale.ROOT)
        if (pkg.isBlank()) return true
        if (pkg in BLOCKED_EXACT_PACKAGES) return true
        return BLOCKED_PACKAGE_TOKENS.any { token ->
            pkg.contains(".$token") || pkg.startsWith("$token.") || pkg.endsWith(".$token") || pkg.contains(token)
        }
    }

    fun getBankName(packageName: String): String? {
        if (packageName.isBlank()) return null
        if (isBlockedNonBankPackage(packageName)) return null
        ALLOWED_BANK_PACKAGES[packageName]?.let { return it }

        val pkg = packageName.lowercase(Locale.ROOT)
        return when {
            pkg.contains("eurobank") -> "Eurobank"
            pkg.contains("walletnfcrel") || pkg.contains("paisa.user") -> "Google Pay"
            pkg.contains("samsung.android.spay") -> "Samsung Wallet"
            pkg.contains("winbank") || pkg.contains("piraeus") -> "Τράπεζα Πειραιώς"
            pkg.contains("alphabank") || pkg.startsWith("gr.alpha.") || pkg.contains("mobileloft.alpha") -> "Alpha Bank"
            pkg.contains(".nbg") || pkg.startsWith("mbanking.nbg") || pkg.contains("nbg.") -> "Εθνική Τράπεζα"
            pkg.contains("revolut") -> "Revolut"
            pkg.contains("transferwise") || pkg.contains(".wise.") -> "Wise"
            pkg.contains("number26") || pkg.contains(".n26.") -> "N26"
            pkg.contains("getmondo") || pkg.contains("monzo") -> "Monzo"
            pkg.contains("starlingbank") -> "Starling Bank"
            pkg.contains("bunq") -> "bunq"
            pkg.contains("imaginecurve") -> "Curve"
            pkg.contains("payzy") -> "Payzy"
            pkg.contains("vivawallet") || pkg.contains("viva.wallet") -> "Viva Wallet"
            pkg.contains("optimabank") -> "Optima Bank"
            pkg.contains("atticabank") -> "Attica Bank"
            pkg.contains("snappi") -> "Snappi"
            pkg.contains("paypal") -> "PayPal"
            pkg.contains("klarna") -> "Klarna"
            pkg.contains("skrill") -> "Skrill"
            pkg.contains("payoneer") -> "Payoneer"
            pkg.contains("barclays") -> "Barclays"
            pkg.contains("hsbc") -> "HSBC"
            pkg.contains("lloyds") -> "Lloyds Bank"
            pkg.contains("natwest") -> "NatWest"
            pkg.contains("santander") -> "Santander"
            pkg.contains("chase.sig") -> "Chase"
            pkg.contains("wellsfargo") -> "Wells Fargo"
            pkg.contains("citimobile") -> "Citi"
            pkg.contains("capitalone") -> "Capital One"
            pkg.contains("americanexpress") -> "American Express"
            pkg.contains("venmo") -> "Venmo"
            pkg.contains("squareup.cash") -> "Cash App"
            pkg.contains("onedebit.chime") -> "Chime"
            pkg.contains("sparkasse") || pkg.contains("sfinanzstatus") -> "Sparkasse"
            pkg.contains("commerzbank") -> "Commerzbank"
            pkg.contains("bnpparibas") -> "BNP Paribas"
            pkg.contains("bbva") -> "BBVA"
            pkg.contains("lacaixa") || pkg.contains("caixabank") -> "CaixaBank"
            pkg.contains("unicredit") -> "UniCredit"
            pkg.contains("intesasanpaolo") -> "Intesa Sanpaolo"
            pkg.contains("abnamro") -> "ABN AMRO"
            pkg.contains("rabomobiel") || pkg.contains("rabobank") -> "Rabobank"
            pkg.contains("raiffeisen") -> "Raiffeisen"
            pkg.contains("erstebank") -> "Erste Bank"
            pkg.contains("nordea") -> "Nordea"
            pkg.contains("swedbank") -> "Swedbank"
            pkg.contains("bankofcyprus") -> "Bank of Cyprus"
            pkg.contains("hellenicbank") -> "Hellenic Bank"
            // Worldwide banking app heuristic (only for non-blacklisted packages with explicit banking tokens)
            Regex("(?:^|\\.)(?:[a-z0-9_-]*bank[a-z0-9_-]*|ebanking|mbanking|mobilebanking|mobilebank|netbank|neobank|creditunion)(?:$|\\.)").containsMatchIn(pkg) -> {
                deriveFriendlyBankNameFromPackage(pkg)
            }
            else -> null
        }
    }

    private fun deriveFriendlyBankNameFromPackage(pkg: String): String {
        val ignoredSegments = setOf(
            "com", "gr", "eu", "uk", "co", "de", "fr", "es", "it", "nl", "at", "ch", "us", "au", "ca", "cy", "org", "net", "io",
            "android", "mobile", "app", "apps", "mbanking", "ebanking", "mobilebanking", "banking", "client", "prod", "prd", "live", "digital"
        )
        val segment = pkg.split(".")
            .firstOrNull { it.length >= 3 && it !in ignoredSegments }
            ?: "Bank"
        return segment.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
    }

    fun isBankPackage(packageName: String): Boolean {
        if (isBlockedNonBankPackage(packageName)) return false
        return getBankName(packageName) != null || packageName == "com.google.android.gms"
    }

    fun parse(
        packageName: String,
        title: String,
        text: String,
        timestamp: Long = System.currentTimeMillis()
    ): PendingBankTransaction? {
        if (isBlockedNonBankPackage(packageName)) return null
        val combined = "$title $text".trim()
        if (combined.isBlank()) return null

        val lower = combined.lowercase(Locale.ROOT)

        // 1. Strict source verification: Google Play Services ("com.google.android.gms") only for Wallet tap-to-pay receipts
        val bankName = if (packageName == "com.google.android.gms") {
            val isWalletTx = (lower.contains("με την κάρτα") || lower.contains("με την καρτα") ||
                lower.contains("with card") || lower.contains("with your") ||
                lower.contains("google pay") || lower.contains("google wallet") ||
                lower.contains("πορτοφόλι google") || lower.contains("mastercard") || lower.contains("visa") ||
                lower.contains("amex") || lower.contains("maestro")) &&
                hasCurrencyIndicator(combined)
            if (!isWalletTx) return null
            "Google Pay"
        } else {
            getBankName(packageName) ?: return null
        }

        // 2. Hard rejection of URLs / promo links / marketing / spam / ads (e.g. Temu offers)
        if (containsPromoOrSpamSignals(lower)) return null

        // 3. Hard rejection of OTP / 2FA / login / authorization approval requests (not completed transactions)
        if (containsOtpOrAuthRequestSignals(lower)) return null

        // 4. Hard rejection of declined / failed / cancelled / insufficient funds alerts
        if (containsDeclinedOrFailedSignals(lower)) return null

        // 5. Hard rejection of statement-ready / promotional balance alerts
        if (containsStatementOrInfoOnlySignals(lower)) return null

        // 6. Classify transaction direction (Income vs Expense) & require positive transaction evidence
        val isIncome = isIncomingCreditNotification(combined, lower)
        val isExpense = isOutgoingExpenseNotification(lower, bankName)
        if (!isIncome && !isExpense) return null

        // 7. Extract transaction amount (strictly ignoring any number after "υπόλοιπο" or "balance")
        val amount = extractAmount(combined) ?: return null
        if (amount <= 0.05) return null

        // 8. Extract merchant / sender / description
        val merchant = extractMerchant(
            body = text,
            title = title,
            combined = combined,
            bankName = bankName,
            isIncome = isIncome
        )

        val fallbackLabel = if (isIncome) "Πίστωση $bankName" else "Συναλλαγή $bankName"

        return PendingBankTransaction(
            bankName = bankName,
            merchantName = merchant.ifBlank { fallbackLabel },
            amount = amount,
            timestamp = timestamp,
            rawText = combined,
            isIncome = isIncome
        )
    }

    private fun hasCurrencyIndicator(text: String): Boolean {
        return Regex("(?:€|\\$|£|¥|₺|\\b(?:EUR|USD|GBP|CHF|PLN|SEK|NOK|DKK|CAD|AUD|RON|BGN|TRY|ευρώ|ευρω)\\b)", RegexOption.IGNORE_CASE)
            .containsMatchIn(text)
    }

    private fun containsPromoOrSpamSignals(lower: String): Boolean {
        // Any URL or web link inside a notification is virtually always a promo, ad, newsletter, or phishing/marketing push
        if (lower.contains("http://") || lower.contains("https://") || lower.contains("www.") ||
            lower.contains("bit.ly/") || lower.contains("tinyurl") ||
            Regex("\\b[a-z0-9-]+\\.(?:com|gr|eu|net|org|co\\.uk|io|app)/").containsMatchIn(lower)
        ) {
            return true
        }
        val promoPhrases = listOf(
            // Greek promos & ads
            "απίστευτο", "απιστευτο", "αγοράστε", "αγοραστε", "μόνο με", "μονο με",
            "προσφορά", "προσφορα", "προσφορές", "προσφορες", "έκπτωση", "εκπτωση",
            "κατάργηση εγγραφής", "καταργηση εγγραφης", "κερδίστε", "κερδιστε",
            "διαγωνισμός", "διαγωνισμος", "κουπόνι", "κουπονι", "πρόσθετες χρεώσεις",
            "ολοκλήρωση της αγοράς", "ανακαλύψτε", "ανακαλυψτε", "προεγκρίθηκε", "προεγκριθηκε",
            "δάνειο", "δανειο", "αίτηση τώρα", "αιτηση τωρα", "προσκάλεσε", "προσκαλεσε",
            // English promos & ads
            "unsubscribe", "special offer", "limited offer", "limited time", "promo code",
            "discount", "% off", "coupon", "voucher", "free shipping", "flash sale",
            "buy now", "shop now", "order now", "win up to", "earn up to", "get up to",
            "invite a friend", "refer a friend", "ready for effortless", "apply now",
            "pre-approved", "loan offer", "credit limit", "upgrade your plan", "try pro",
            "discover our", "new feature", "black friday", "cyber monday"
        )
        return promoPhrases.any { lower.contains(it) }
    }

    private fun containsOtpOrAuthRequestSignals(lower: String): Boolean {
        val otpPhrases = listOf(
            "otp", "one-time", "one time password", "verification code", "security code",
            "confirm your", "approve this", "approve payment", "authorize", "authorise",
            "3d secure", "3ds", "tap to approve", "confirm transaction", "login attempt",
            "sign-in", "sign in attempt", "passcode", "pin code",
            "κωδικός επιβεβαίωσης", "κωδικος επιβεβαιωσης", "κωδικός ασφαλείας", "κωδικος ασφαλειας",
            "κωδικός μίας χρήσης", "κωδικος μιας χρησης", "μίας χρήσης", "μιας χρησης",
            "εγκρίνετε τη συναλλαγή", "εγκρινετε τη συναλλαγη", "επιβεβαιώστε τη συναλλαγή",
            "επιβεβαιωστε τη συναλλαγη", "εξουσιοδότηση", "εξουσιοδοτηση", "έγκριση συναλλαγής",
            "εγκριση συναλλαγης", "μην κοινοποιείτε", "μην μοιράζεστε", "do not share"
        )
        return otpPhrases.any { lower.contains(it) }
    }

    private fun containsDeclinedOrFailedSignals(lower: String): Boolean {
        val declinedPhrases = listOf(
            "απορρίφθηκε", "απορριφθηκε", "ανεπαρκές", "ανεπαρκες", "απέτυχε", "απετυχε",
            "ακυρώθηκε", "ακυρωθηκε", "δεν ολοκληρώθηκε", "δεν ολοκληρωθηκε", "αδυναμία χρέωσης",
            "declined", "failed", "rejected", "insufficient funds", "insufficient balance",
            "cancelled", "canceled", "unsuccessful", "could not be completed", "not enough",
            "abgelehnt", "refusé", "rechazado", "rifiutato"
        )
        return declinedPhrases.any { lower.contains(it) }
    }

    private fun containsStatementOrInfoOnlySignals(lower: String): Boolean {
        val infoPhrases = listOf(
            "monthly statement", "statement is ready", "your statement", "account statement",
            "εκδόθηκε ο λογαριασμός", "εκδοθηκε ο λογαριασμος", "νέο αντίγραφο", "νεο αντιγραφο",
            "υπενθύμιση πληρωμής", "υπενθυμιση πληρωμης", "payment due", "due on",
            "minimum payment", "ελάχιστη καταβολή", "ελαχιστη καταβολη"
        )
        return infoPhrases.any { lower.contains(it) }
    }

    private fun isIncomingCreditNotification(combined: String, lower: String): Boolean {
        // Explicit outgoing-only markers override ambiguous matches (e.g. "You sent to..." or "Αγορά με κάρτα")
        val strictOutgoingPhrases = listOf(
            "αγορά", "αγορα", "χρέωση", "χρεωση", "χρεώθηκε", "χρεωθηκε",
            "πληρώσατε", "πληρωσατε", "στείλατε", "στειλατε", "ανάληψη", "αναληψη",
            "με την κάρτα", "με την καρτα", "you paid", "you spent", "you sent",
            "card charged", "debited", "purchase", "bought", "withdrew", "withdrawal"
        )
        val hasStrictOutgoing = strictOutgoingPhrases.any { lower.contains(it) }

        val incomingKeywords = listOf(
            // Greek
            "λάβατε", "λαβατε", "κατατέθηκαν", "κατατεθηκαν", "κατάθεση", "καταθεση",
            "πίστωση", "πιστωση", "πιστώθηκε", "πιστωθηκε", "πληρωθήκατε", "πληρωθηκατε",
            "εισερχόμενη", "εισερχομενη", "εισερχόμενο", "εισερχομενο",
            "έμβασμα από", "εμβασμα απο", "μεταφορά από", "μεταφορα απο",
            "σας έστειλε", "σας εστειλε", "επιστροφή χρημάτων", "επιστροφη χρηματων",
            "επιστροφή ποσού", "επιστροφη ποσου", "μισθοδοσία", "μισθοδοσια",
            "μισθός", "μισθος", "επίδομα", "επιδομα", "μέρισμα", "μερισμα",
            // English & EU
            "you received", "received ", "credited", "credit of", "direct deposit",
            "deposit of", "deposited", "incoming payment", "incoming transfer",
            "salary", "payroll", "sent you", "paid you", "refund", "refunded",
            "money received", "added to your", "top-up", "topped up", "cashback credited",
            "gutschrift", "geldeingang", "vous avez reçu", "virement reçu",
            "accredito", "bonifico ricevuto", "has recibido", "ingreso"
        )
        val hasIncomingKeyword = incomingKeywords.any { lower.contains(it) }
        val hasExplicitPlusAmount = Regex("(?<!\\d)\\+\\s*(?:€|\\$|£)?\\s*\\d").containsMatchIn(combined)

        if (hasIncomingKeyword && !hasStrictOutgoing) return true
        // Refunds sometimes mention "purchase" or "card" alongside "refund"/"επιστροφή", so still count explicit refund as income
        val isExplicitRefund = lower.contains("refund") || lower.contains("επιστροφή χρημάτων") ||
            lower.contains("επιστροφη χρηματων") || lower.contains("επιστροφή ποσού") ||
            lower.contains("επιστροφη ποσου") || lower.contains("πιστώθηκε") || lower.contains("πιστωθηκε")
        if (isExplicitRefund) return true

        return hasExplicitPlusAmount && !hasStrictOutgoing && hasCurrencyIndicator(combined)
    }

    private fun isOutgoingExpenseNotification(lower: String, bankName: String): Boolean {
        // Google Pay / Samsung Wallet card tap receipts (e.g. "2,10 € με την κάρτα Revolut Mastercard")
        if (bankName == "Google Pay" || bankName == "Samsung Wallet") {
            val walletCardHint = listOf(
                "με την κάρτα", "με την καρτα", "with card", "with your", "mastercard",
                "visa", "amex", "maestro", "paid", "spent", "πληρωμή", "πληρωμη", "αγορά", "αγορα"
            ).any { lower.contains(it) }
            if (walletCardHint && hasCurrencyIndicator(lower)) return true
        }

        // Require an actual completed transaction/payment/debit keyword (NOT just a currency symbol!)
        val expenseKeywords = listOf(
            // Greek
            "αγορά", "αγορα", "συναλλαγή", "συναλλαγη", "χρέωση", "χρεωση", "χρεώθηκε", "χρεωθηκε",
            "πληρωμή", "πληρωμη", "πληρώσατε", "πληρωσατε", "εγκρίθηκε", "εγκριθηκε",
            "με την κάρτα", "με την καρτα", "κάρτα σας", "καρτα σας",
            "έξοδο", "εξοδο", "μεταφορά προς", "μεταφορα προς", "στείλατε", "στειλατε",
            "ανάληψη", "αναληψη", "σε pos", "e-commerce", "ecommerce", "επιχείρηση", "επιχειρηση",
            // English & International
            "purchase", "spent", "you spent", "you paid", "paid ", "payment of", "payment to",
            "transaction", "card charged", "charged", "debited", "debit of", "you sent",
            "sent to", "transfer to", "withdrew", "withdrawal", "pos ", "at pos", "approved",
            "bezahlt", "zahlung", "abbuchung", "kartenzahlung", "paiement", "achat", "débit",
            "pagamento", "acquisto", "addebito", "pago", "compra", "cargo"
        )
        return expenseKeywords.any { lower.contains(it) }
    }

    /**
     * Eliminate any numbers after "υπόλοιπο" or "balance" so available balance is never parsed as the transaction amount.
     */
    private fun removeBalanceClauses(text: String): String {
        var clean = text.replace(
            Regex("(?i)(?:νέο\\s+|διαθέσιμο\\s+|λογιστικό\\s+|new\\s+|available\\s+|remaining\\s+|current\\s+)?(?:υπόλοιπο|υπολοιπο|υπολοίπου|υπολοιπου|balance|bal\\.|saldo|solde)[^.,;!\\n]*?(?:[€$£¥₺]|EUR|USD|GBP|CHF)?\\s*(?:[0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]+)(?:[.,][0-9]{1,2})?\\s*(?:[€$£¥₺]|EUR|USD|GBP|CHF|ευρώ|ευρω)?"),
            " "
        )
        clean = clean.replace(
            Regex("(?i)(?:υπόλοιπο|υπολοιπο|balance|saldo|solde)[^0-9]*(?:[0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]+)(?:[.,][0-9]{1,2})?\\s*(?:[€$£¥₺]|EUR|USD|GBP|CHF|ευρώ|ευρω)?"),
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
        val currencySymbolOrCode = "(?:€|\\$|£|¥|₺|EUR|USD|GBP|CHF|PLN|SEK|NOK|DKK|CAD|AUD|RON|BGN|TRY|ευρώ|ευρω)"

        val amountPatterns = listOf(
            Regex("(?i)[+-]?\\s*$currencySymbolOrCode\\s*$numGroup"),
            Regex("(?i)[+-]?\\s*$numGroup\\s*$currencySymbolOrCode"),
            Regex("(?i)(?:ποσό|ποσο|amount|sum|total)[:\\s]+[+-]?\\s*$numGroup"),
            Regex("((?:[0-9]{1,3}(?:[.,][0-9]{3})+|[0-9]+)[.,][0-9]{2})")
        )

        for (pattern in amountPatterns) {
            val match = pattern.find(cleaned)
            if (match != null) {
                val parsed = parseAmountNumber(match.groupValues[1])
                if (parsed != null && parsed > 0.05 && parsed < 500000.0) {
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
        bankName: String = "",
        isIncome: Boolean = false
    ): String {
        // 0. Google Pay / Samsung Wallet notifications put the merchant directly in the notification title
        if (bankName == "Google Pay" || bankName == "Samsung Wallet") {
            val cleanTitle = title.trim()
                .replace(Regex("^(?:PAYPAL\\s*\\*|PP\\*|GOOGLE\\s*\\*|AMZN\\s*\\*|AMAZON\\s*\\*)\\s*", RegexOption.IGNORE_CASE), "")
                .trim()
            val titleLower = cleanTitle.lowercase(Locale.ROOT)
            val isGenericWalletTitle = titleLower in setOf(
                "google pay", "google wallet", "samsung wallet", "samsung pay",
                "πορτοφόλι google", "πορτοφολι google",
                "πληρωμή", "πληρωμη", "συναλλαγή", "συναλλαγη", "αγορά", "αγορα",
                "payment", "purchase", "notification"
            )
            val looksLikeAmount = Regex("^[+-]?\\s*[€$£]?\\s*[0-9.,]+\\s*(?:€|\\$|£|eur|usd|gbp|ευρώ)?$", RegexOption.IGNORE_CASE).matches(cleanTitle)
            if (cleanTitle.length in 2..60 && !isGenericWalletTitle && !looksLikeAmount) {
                return cleanTitle
            }
        }

        // 1. Incoming credit / sender extraction patterns
        val terminator = "(?=\\s+(?:με|την|στις|on|with|\\d{1,2}/\\d{1,2}|\\d{1,2}:\\d{2})|[,;!\\n]|(?<!\\b[A-ZΑ-ΩΆ-Ώ])\\.(?!\\s*[A-ZΑ-ΩΆ-Ώ]\\.)|\\z)"
        if (isIncome) {
            val incomePatterns = listOf(
                Regex("(?:από|απο|from|by)\\s+(?:την\\s+|τον\\s+|το\\s+)?(?:εταιρεία|εταιρία|επιχείρηση|επιχειρηση)?\\s*[:\\s]*([a-zA-Zα-ωΑ-Ωά-ώΆ-Ώ0-9\\s&.*#_/-]+?)$terminator", RegexOption.IGNORE_CASE),
                Regex("^([a-zA-Zα-ωΑ-Ωά-ώΆ-Ώ0-9\\s&.-]{2,40})\\s+(?:sent you|paid you|σας έστειλε|σας εστειλε)", RegexOption.IGNORE_CASE)
            )
            val textsToSearch = listOf(body, combined).filter { it.isNotBlank() }.distinct()
            for (sourceText in textsToSearch) {
                for (pattern in incomePatterns) {
                    val match = pattern.find(sourceText)
                    if (match != null) {
                        val candidate = match.groupValues[1].trim()
                        if (candidate.length in 2..45 &&
                            !candidate.contains("υπόλοιπο", ignoreCase = true) &&
                            !candidate.contains("balance", ignoreCase = true) &&
                            !candidate.contains("λογαριασμ", ignoreCase = true) &&
                            !candidate.contains("account", ignoreCase = true)
                        ) {
                            return candidate
                        }
                    }
                }
            }
            if (combined.contains("μισθ", ignoreCase = true) || combined.contains("salary", ignoreCase = true) || combined.contains("payroll", ignoreCase = true)) {
                return "Μισθοδοσία"
            }
            if (combined.contains("επιστροφ", ignoreCase = true) || combined.contains("refund", ignoreCase = true)) {
                return "Επιστροφή χρημάτων ($bankName)"
            }
            return ""
        }

        // 2. Expense merchant pattern extraction
        val merchantPatterns = listOf(
            Regex("(?:στο κατάστημα|στο|στην|στη|σε(?!\\s+POS\\b)|προς|at|in|to)\\s+(?:την\\s+|τον\\s+|το\\s+)?(?:εταιρεία|εταιρία|επιχείρηση|επιχειρηση|επιχ/ση|έμπορο|εμπορο|κατάστημα|καταστημα|merchant)?\\s*[:\\s]*([a-zA-Zα-ωΑ-Ωά-ώΆ-Ώ0-9\\s&.*#_/-]+?)$terminator", RegexOption.IGNORE_CASE),
            Regex("(?:επιχείρηση|επιχειρηση|έμπορος|εμπορος|merchant|προς)[:\\s]+([a-zA-Zα-ωΑ-Ωά-ώΆ-Ώ0-9\\s&.*#_/-]+?)$terminator", RegexOption.IGNORE_CASE),
            Regex("(?:αγορά|αγορα|πληρωμή|πληρωμη|purchase|spent|payment)\\s+(?:ποσού|ποσου|of)?\\s*[0-9.,€$£eurusdgbpEURUSDGBP\\s]+\\s*(?:σε|στο|στην|στη|at|to)?\\s*(?:εταιρεία|εταιρία|επιχείρηση|επιχειρηση)?\\s*([a-zA-Zα-ωΑ-Ωά-ώΆ-Ώ0-9\\s&.*#_/-]+?)$terminator", RegexOption.IGNORE_CASE)
        )

        val textsToSearch = listOf(body, combined).filter { it.isNotBlank() }.distinct()
        for (sourceText in textsToSearch) {
            for (pattern in merchantPatterns) {
                val match = pattern.find(sourceText)
                if (match != null) {
                    var candidate = match.groupValues[1].trim()
                    candidate = candidate.replace(Regex("^(?:εταιρεία|εταιρία|επιχείρηση|επιχειρηση|έμπορο|εμπορο|κατάστημα|καταστημα)\\s+", RegexOption.IGNORE_CASE), "").trim()
                    candidate = candidate.replace(Regex("^(?:PAYPAL\\s*\\*|PP\\*|GOOGLE\\s*\\*|AMZN\\s*\\*|AMAZON\\s*\\*)\\s*", RegexOption.IGNORE_CASE), "").trim()
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

        // 3. Fallback: Check known popular brands directly across full notification text
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
     * Categorize recognized expense merchant / brand into matching ExpenseCategory.
     */
    fun categorizeMerchant(
        merchant: String,
        rawText: String,
        categories: List<ExpenseCategory>
    ): ExpenseCategory? {
        val search = "$merchant $rawText".lowercase(Locale.ROOT)

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

        return null
    }

    /**
     * Categorize an incoming credit/deposit bank notification into a matching Income Category.
     */
    fun categorizeIncomeMerchant(
        merchant: String,
        rawText: String,
        incomeCategories: List<ExpenseCategory>
    ): ExpenseCategory? {
        if (incomeCategories.isEmpty()) return null
        val search = "$merchant $rawText".lowercase(Locale.ROOT)

        // 1. Check custom keywords & category names first
        for (cat in incomeCategories) {
            val kwList = cat.voiceKeywords.split(",").map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotBlank() }
            val catName = cat.name.trim().lowercase(Locale.ROOT)
            for (kw in (kwList + catName)) {
                if (kw.length >= 3) {
                    val pattern = "(?:^|[^\\p{L}\\p{Nd}])" + Regex.escape(kw) + "(?:$|[^\\p{L}\\p{Nd}])"
                    if (Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(search)) {
                        return cat
                    }
                }
            }
        }

        // 2. Domain heuristics for common income types
        return when {
            search.contains("μισθ") || search.contains("salary") || search.contains("payroll") ->
                incomeCategories.find { it.name.contains("Μισθ", ignoreCase = true) || it.name.contains("Salary", ignoreCase = true) }
            search.contains("ενοίκ") || search.contains("ενοικ") || search.contains("rent") ->
                incomeCategories.find { it.name.contains("Ενοίκ", ignoreCase = true) || it.name.contains("Rent", ignoreCase = true) }
            search.contains("επιδότ") || search.contains("επιδοτ") || search.contains("επίδομ") || search.contains("επιδομ") || search.contains("οπεκα") || search.contains("δηυπα") || search.contains("benefit") || search.contains("grant") ->
                incomeCategories.find { it.name.contains("Επιδότ", ignoreCase = true) || it.name.contains("Benefit", ignoreCase = true) }
            search.contains("bonus") || search.contains("μπόνους") || search.contains("δώρο") || search.contains("δωρο") || search.contains("cashback") ->
                incomeCategories.find { it.name.contains("Bonus", ignoreCase = true) || it.name.contains("Δώρο", ignoreCase = true) }
            search.contains("μέρισμ") || search.contains("μερισμ") || search.contains("dividend") || search.contains("interest") || search.contains("τόκοι") ->
                incomeCategories.find { it.name.contains("Επενδ", ignoreCase = true) || it.name.contains("Invest", ignoreCase = true) }
            else -> incomeCategories.find { it.name.contains("Άλλο", ignoreCase = true) || it.name.contains("Other", ignoreCase = true) }
                ?: incomeCategories.lastOrNull()
        }
    }
}
