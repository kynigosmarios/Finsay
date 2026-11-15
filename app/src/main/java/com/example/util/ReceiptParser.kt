package com.example.util

import com.example.data.entity.ExpenseCategory
import com.google.mlkit.vision.text.Text
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class ParsedReceiptData(
    val title: String,
    val amount: Double,
    val timestamp: Long,
    val suggestedCategoryName: String,
    val merchantAfm: String = "",
    val rawExtractedText: String = ""
)

object ReceiptParser {

    private data class KnownMerchant(
        val displayName: String,
        val defaultCategory: String
    )

    private val GREEK_KNOWN_MERCHANTS = mapOf(
        // Supermarkets & Groceries
        "ΣΚΛΑΒΕΝΙΤΗΣ" to KnownMerchant("Σκλαβενίτης", "Σούπερ Μάρκετ"),
        "ΣΚΛΑΒΕΝΙΤΗ" to KnownMerchant("Σκλαβενίτης", "Σούπερ Μάρκετ"),
        "SKLAVENITIS" to KnownMerchant("Σκλαβενίτης", "Σούπερ Μάρκετ"),
        "SKLAVENITI" to KnownMerchant("Σκλαβενίτης", "Σούπερ Μάρκετ"),
        "EKAABENITHC" to KnownMerchant("Σκλαβενίτης", "Σούπερ Μάρκετ"),
        "EKLABENITHC" to KnownMerchant("Σκλαβενίτης", "Σούπερ Μάρκετ"),
        "EKAABENITHE" to KnownMerchant("Σκλαβενίτης", "Σούπερ Μάρκετ"),
        "EKLABENITHE" to KnownMerchant("Σκλαβενίτης", "Σούπερ Μάρκετ"),
        "SKAABENITHC" to KnownMerchant("Σκλαβενίτης", "Σούπερ Μάρκετ"),
        "SKLABENITHC" to KnownMerchant("Σκλαβενίτης", "Σούπερ Μάρκετ"),
        "ZKLABENITHC" to KnownMerchant("Σκλαβενίτης", "Σούπερ Μάρκετ"),
        "ZKAABENITHC" to KnownMerchant("Σκλαβενίτης", "Σούπερ Μάρκετ"),
        "ΕΛΛΗΝΙΚΕΣ ΥΠΕΡΑΓΟΡΕΣ" to KnownMerchant("Σκλαβενίτης", "Σούπερ Μάρκετ"),
        "ELLINIKES YPERAGORES" to KnownMerchant("Σκλαβενίτης", "Σούπερ Μάρκετ"),
        "EAAHNIKEC YTEPATOPEL" to KnownMerchant("Σκλαβενίτης", "Σούπερ Μάρκετ"),
        "EAAHNIKEC" to KnownMerchant("Σκλαβενίτης", "Σούπερ Μάρκετ"),
        "YTEPATOPEL" to KnownMerchant("Σκλαβενίτης", "Σούπερ Μάρκετ"),
        "ΑΒ ΒΑΣΙΛΟΠΟΥΛΟΣ" to KnownMerchant("ΑΒ Βασιλόπουλος", "Σούπερ Μάρκετ"),
        "AB VASSILOPOULOS" to KnownMerchant("ΑΒ Βασιλόπουλος", "Σούπερ Μάρκετ"),
        "AB VASILOPOULOS" to KnownMerchant("ΑΒ Βασιλόπουλος", "Σούπερ Μάρκετ"),
        "ΒΑΣΙΛΟΠΟΥΛΟΣ" to KnownMerchant("ΑΒ Βασιλόπουλος", "Σούπερ Μάρκετ"),
        "ΒΑΣΙΛΟΠΟΥΛΟΥ" to KnownMerchant("ΑΒ Βασιλόπουλος", "Σούπερ Μάρκετ"),
        "BASIAOTOYAOC" to KnownMerchant("ΑΒ Βασιλόπουλος", "Σούπερ Μάρκετ"),
        "LIDL" to KnownMerchant("Λιντλ", "Σούπερ Μάρκετ"),
        "ΛΙΝΤΛ" to KnownMerchant("Λιντλ", "Σούπερ Μάρκετ"),
        "LGDL" to KnownMerchant("Λιντλ", "Σούπερ Μάρκετ"),
        "L1DL" to KnownMerchant("Λιντλ", "Σούπερ Μάρκετ"),
        "LCDL" to KnownMerchant("Λιντλ", "Σούπερ Μάρκετ"),
        "L!DL" to KnownMerchant("Λιντλ", "Σούπερ Μάρκετ"),
        "LODL" to KnownMerchant("Λιντλ", "Σούπερ Μάρκετ"),
        "AINTA" to KnownMerchant("Λιντλ", "Σούπερ Μάρκετ"),
        "ΛΙΔΛ" to KnownMerchant("Λιντλ", "Σούπερ Μάρκετ"),
        "LIDL PLUS" to KnownMerchant("Λιντλ", "Σούπερ Μάρκετ"),
        "LIDL HELLAS" to KnownMerchant("Λιντλ", "Σούπερ Μάρκετ"),
        "ΛΙΝΤΛ ΕΛΛΑΣ" to KnownMerchant("Λιντλ", "Σούπερ Μάρκετ"),
        "MY MARKET" to KnownMerchant("My Market", "Σούπερ Μάρκετ"),
        "MYMARKET" to KnownMerchant("My Market", "Σούπερ Μάρκετ"),
        "ΜΑΣΟΥΤΗΣ" to KnownMerchant("Μασούτης", "Σούπερ Μάρκετ"),
        "ΜΑΣΟΥΤΗ" to KnownMerchant("Μασούτης", "Σούπερ Μάρκετ"),
        "MASOUTIS" to KnownMerchant("Μασούτης", "Σούπερ Μάρκετ"),
        "MACOYTHC" to KnownMerchant("Μασούτης", "Σούπερ Μάρκετ"),
        "MASOYTHS" to KnownMerchant("Μασούτης", "Σούπερ Μάρκετ"),
        "MASOYTHC" to KnownMerchant("Μασούτης", "Σούπερ Μάρκετ"),
        "ΓΑΛΑΞΙΑΣ" to KnownMerchant("Γαλαξίας", "Σούπερ Μάρκετ"),
        "ΓΑΛΑΞΙΑ" to KnownMerchant("Γαλαξίας", "Σούπερ Μάρκετ"),
        "GALAXIAS" to KnownMerchant("Γαλαξίας", "Σούπερ Μάρκετ"),
        "TAAAEIAC" to KnownMerchant("Γαλαξίας", "Σούπερ Μάρκετ"),
        "MARKET IN" to KnownMerchant("Market In", "Σούπερ Μάρκετ"),
        "BAZAAR" to KnownMerchant("Bazaar", "Σούπερ Μάρκετ"),
        "ΚΡΗΤΙΚΟΣ" to KnownMerchant("Κρητικός", "Σούπερ Μάρκετ"),
        "ΚΡΗΤΙΚΟΥ" to KnownMerchant("Κρητικός", "Σούπερ Μάρκετ"),
        "KRITIKOS" to KnownMerchant("Κρητικός", "Σούπερ Μάρκετ"),
        "KPHTIKOC" to KnownMerchant("Κρητικός", "Σούπερ Μάρκετ"),
        "DISCOUNT MARKT" to KnownMerchant("Discount Markt", "Σούπερ Μάρκετ"),
        "OK ANYTIME" to KnownMerchant("OK Anytime Market", "Σούπερ Μάρκετ"),

        // Bakeries, Cafes & Food Delivery
        "ΒΕΝΕΤΗΣ" to KnownMerchant("Φούρνος Βενέτη", "Φαγητό & Delivery"),
        "ΒΕΝΕΤΗ" to KnownMerchant("Φούρνος Βενέτη", "Φαγητό & Delivery"),
        "VENETI" to KnownMerchant("Φούρνος Βενέτη", "Φαγητό & Delivery"),
        "ΑΠΟΛΛΩΝΙΟΝ" to KnownMerchant("Απολλώνιον Bakery", "Φαγητό & Delivery"),
        "APOLLONION" to KnownMerchant("Απολλώνιον Bakery", "Φαγητό & Delivery"),
        "ΓΡΗΓΟΡΗΣ" to KnownMerchant("Γρηγόρης", "Καφές & Έξω"),
        "ΓΡΗΓΟΡΗ" to KnownMerchant("Γρηγόρης", "Καφές & Έξω"),
        "GRIGORIS" to KnownMerchant("Γρηγόρης", "Καφές & Έξω"),
        "GREGORYS" to KnownMerchant("Γρηγόρης", "Καφές & Έξω"),
        "COFFEE ISLAND" to KnownMerchant("Coffee Island", "Καφές & Έξω"),
        "COFFEE LAB" to KnownMerchant("Coffee Lab", "Καφές & Έξω"),
        "MIKEL" to KnownMerchant("Mikel Coffee", "Καφές & Έξω"),
        "EVEREST" to KnownMerchant("Everest", "Καφές & Έξω"),
        "GOODY'S" to KnownMerchant("Goody's Burger House", "Φαγητό & Delivery"),
        "GOODYS" to KnownMerchant("Goody's Burger House", "Φαγητό & Delivery"),
        "MCDONALD" to KnownMerchant("McDonald's", "Φαγητό & Delivery"),
        "KFC" to KnownMerchant("KFC", "Φαγητό & Delivery"),
        "PIZZA FAN" to KnownMerchant("Pizza Fan", "Φαγητό & Delivery"),
        "ROMA PIZZA" to KnownMerchant("Roma Pizza", "Φαγητό & Delivery"),
        "DOMINO" to KnownMerchant("Domino's Pizza", "Φαγητό & Delivery"),
        "EFOOD" to KnownMerchant("efood", "Φαγητό & Delivery"),
        "WOLT" to KnownMerchant("Wolt", "Φαγητό & Delivery"),
        "BOX DELIVERY" to KnownMerchant("BOX", "Φαγητό & Delivery"),

        // Fuel & Gas Stations
        "SHELL" to KnownMerchant("Πρατήριο Shell", "Μετακινήσεις"),
        "BP" to KnownMerchant("Πρατήριο BP", "Μετακινήσεις"),
        "EKO" to KnownMerchant("Πρατήριο EKO", "Μετακινήσεις"),
        "AVIN" to KnownMerchant("Πρατήριο Avin", "Μετακινήσεις"),
        "REVOIL" to KnownMerchant("Πρατήριο Revoil", "Μετακινήσεις"),
        "ΕΛΙΝ" to KnownMerchant("Πρατήριο Ελίν", "Μετακινήσεις"),
        "ELIN" to KnownMerchant("Πρατήριο Ελίν", "Μετακινήσεις"),
        "CORAL GAS" to KnownMerchant("Coral Gas", "Μετακινήσεις"),

        // Tech, Electronics, Home & DIY
        "ΚΩΤΣΟΒΟΛΟΣ" to KnownMerchant("Κωτσόβολος", "Σπίτι & Ηλεκτρικά"),
        "KOTSOVOLOS" to KnownMerchant("Κωτσόβολος", "Σπίτι & Ηλεκτρικά"),
        "PUBLIC" to KnownMerchant("Public", "Αγορές & Τεχνολογία"),
        "ΠΛΑΙΣΙΟ" to KnownMerchant("Πλαίσιο", "Αγορές & Τεχνολογία"),
        "PLAISIO" to KnownMerchant("Πλαίσιο", "Αγορές & Τεχνολογία"),
        "ΓΕΡΜΑΝΟΣ" to KnownMerchant("Γερμανός", "Αγορές & Τεχνολογία"),
        "GERMANOS" to KnownMerchant("Γερμανός", "Αγορές & Τεχνολογία"),
        "IKEA" to KnownMerchant("IKEA", "Σπίτι & Εξοπλισμός"),
        "ΙKEA" to KnownMerchant("IKEA", "Σπίτι & Εξοπλισμός"),
        "LEROY MERLIN" to KnownMerchant("Leroy Merlin", "Σπίτι & Εξοπλισμός"),
        "LEROYMERLIN" to KnownMerchant("Leroy Merlin", "Σπίτι & Εξοπλισμός"),
        "DEDEMAN" to KnownMerchant("Dedeman", "Σπίτι & Εξοπλισμός"),
        "ΔΕΔΕΜΑΝ" to KnownMerchant("Dedeman", "Σπίτι & Εξοπλισμός"),
        "PRAKTIKER" to KnownMerchant("Praktiker", "Σπίτι & Εξοπλισμός"),
        "JUMBO" to KnownMerchant("Jumbo", "Παιδιά & Σπίτι"),

        // Clothing & Personal Care
        "ZARA" to KnownMerchant("Zara", "Ένδυση & Υπόδηση"),
        "H&M" to KnownMerchant("H&M", "Ένδυση & Υπόδηση"),
        "BERSHKA" to KnownMerchant("Bershka", "Ένδυση & Υπόδηση"),
        "STRADIVARIUS" to KnownMerchant("Stradivarius", "Ένδυση & Υπόδηση"),
        "PULL&BEAR" to KnownMerchant("Pull & Bear", "Ένδυση & Υπόδηση"),
        "PULL & BEAR" to KnownMerchant("Pull & Bear", "Ένδυση & Υπόδηση"),
        "MASSIMO DUTTI" to KnownMerchant("Massimo Dutti", "Ένδυση & Υπόδηση"),
        "OYSHO" to KnownMerchant("Oysho", "Ένδυση & Υπόδηση"),
        "HONDOS CENTER" to KnownMerchant("Hondos Center", "Υγεία & Ομορφιά"),
        "SEPHORA" to KnownMerchant("Sephora", "Υγεία & Ομορφιά"),

        // Telecom, Energy & Utilities
        "COSMOTE" to KnownMerchant("Cosmote", "Λογαριασμοί & Τηλεφωνία"),
        "VODAFONE" to KnownMerchant("Vodafone", "Λογαριασμοί & Τηλεφωνία"),
        "NOVA" to KnownMerchant("Nova", "Λογαριασμοί & Τηλεφωνία"),
        "ΔΕΗ" to KnownMerchant("ΔΕΗ", "Λογαριασμοί & Ρεύμα"),
        "DEI" to KnownMerchant("ΔΕΗ", "Λογαριασμοί & Ρεύμα"),
        "PROTERGIA" to KnownMerchant("Protergia", "Λογαριασμοί & Ρεύμα"),
        "ΠΡΩΤΕΡΓΙΑ" to KnownMerchant("Protergia", "Λογαριασμοί & Ρεύμα"),
        "ELPEDISON" to KnownMerchant("Elpedison", "Λογαριασμοί & Ρεύμα"),
        "ΗΡΩΝ" to KnownMerchant("Ήρων", "Λογαριασμοί & Ρεύμα"),
        "HERON" to KnownMerchant("Ήρων", "Λογαριασμοί & Ρεύμα"),
        "ΕΥΔΑΠ" to KnownMerchant("ΕΥΔΑΠ", "Λογαριασμοί & Νερό"),
        "EYDAP" to KnownMerchant("ΕΥΔΑΠ", "Λογαριασμοί & Νερό"),
        "ΕΥΑΘ" to KnownMerchant("ΕΥΑΘ", "Λογαριασμοί & Νερό"),
        "EYATH" to KnownMerchant("ΕΥΑΘ", "Λογαριασμοί & Νερό"),

        // Couriers & Services
        "ΕΛΤΑ" to KnownMerchant("ΕΛΤΑ", "Υπηρεσίες"),
        "ACS" to KnownMerchant("ACS Courier", "Υπηρεσίες"),
        "ΓΕΝΙΚΗ ΤΑΧΥΔΡΟΜΙΚΗ" to KnownMerchant("Γενική Ταχυδρομική", "Υπηρεσίες"),
        "SPEEDEX" to KnownMerchant("Speedex", "Υπηρεσίες"),
        "BOX NOW" to KnownMerchant("Box Now", "Υπηρεσίες")
    )

    private val GREEK_AFM_MERCHANTS = mapOf(
        "800764388" to KnownMerchant("Σκλαβενίτης", "Σούπερ Μάρκετ"),
        "094317955" to KnownMerchant("Praktiker", "Σπίτι & Εξοπλισμός"),
        "093683423" to KnownMerchant("Λιντλ", "Σούπερ Μάρκετ"),
        "094049871" to KnownMerchant("ΑΒ Βασιλόπουλος", "Σούπερ Μάρκετ"),
        "094056294" to KnownMerchant("Μασούτης", "Σούπερ Μάρκετ"),
        "094149182" to KnownMerchant("Κωτσόβολος", "Σπίτι & Ηλεκτρικά"),
        "094405330" to KnownMerchant("Public", "Αγορές & Τεχνολογία"),
        "094086392" to KnownMerchant("Πλαίσιο", "Αγορές & Τεχνολογία"),
        "094474775" to KnownMerchant("Jumbo", "Παιδιά & Σπίτι"),
        "094191316" to KnownMerchant("Leroy Merlin", "Σπίτι & Εξοπλισμός"),
        "099762147" to KnownMerchant("IKEA", "Σπίτι & Εξοπλισμός")
    )

    private val GREEK_STORE_TYPE_CATEGORIES = mapOf(
        "ΦΑΡΜΑΚΕΙΟ" to "Υγεία & Φαρμακείο",
        "PHARMACY" to "Υγεία & Φαρμακείο",
        "FARMAKEIO" to "Υγεία & Φαρμακείο",
        "ΑΡΤΟΠΟΙΕΙΟ" to "Φαγητό & Delivery",
        "ΑΡΤΟΠΟΙΙΑ" to "Φαγητό & Delivery",
        "ΦΟΥΡΝΟΣ" to "Φαγητό & Delivery",
        "BAKERY" to "Φαγητό & Delivery",
        "FOURNOS" to "Φαγητό & Delivery",
        "FOYRNOS" to "Φαγητό & Delivery",
        "ΖΑΧΑΡΟΠΛΑΣΤΕΙΟ" to "Φαγητό & Delivery",
        "PASTRY" to "Φαγητό & Delivery",
        "ΚΑΦΕΚΟΠΤΕΙΟ" to "Καφές & Έξω",
        "ΚΑΦΕΤΕΡΙΑ" to "Καφές & Έξω",
        "ΚΑΦΕ" to "Καφές & Έξω",
        "CAFE" to "Καφές & Έξω",
        "COFFEE" to "Καφές & Έξω",
        "ΕΣΤΙΑΤΟΡΙΟ" to "Φαγητό & Delivery",
        "RESTAURANT" to "Φαγητό & Delivery",
        "ΤΑΒΕΡΝΑ" to "Φαγητό & Delivery",
        "TAVERNA" to "Φαγητό & Delivery",
        "ΨΗΤΟΠΩΛΕΙΟ" to "Φαγητό & Delivery",
        "ΟΒΕΛΙΣΤΗΡΙΟ" to "Φαγητό & Delivery",
        "ΨΗΣΤΑΡΙΑ" to "Φαγητό & Delivery",
        "PIZZERIA" to "Φαγητό & Delivery",
        "BURGER" to "Φαγητό & Delivery",
        "ΚΡΕΟΠΩΛΕΙΟ" to "Σούπερ Μάρκετ",
        "ΙΧΘΥΟΠΩΛΕΙΟ" to "Σούπερ Μάρκετ",
        "ΟΠΩΡΟΠΩΛΕΙΟ" to "Σούπερ Μάρκετ",
        "ΜΑΝΑΒΙΚΟ" to "Σούπερ Μάρκετ",
        "SUPER MARKET" to "Σούπερ Μάρκετ",
        "SUPERMARKET" to "Σούπερ Μάρκετ",
        "SUPER MARKETS" to "Σούπερ Μάρκετ",
        "ΣΟΥΠΕΡ ΜΑΡΚΕΤ" to "Σούπερ Μάρκετ",
        "ΥΠΕΡΑΓΟΡΑ" to "Σούπερ Μάρκετ",
        "ΥΠΕΡΑΓΟΡΕΣ" to "Σούπερ Μάρκετ",
        "YPERAGORA" to "Σούπερ Μάρκετ",
        "YTEPATOPA" to "Σούπερ Μάρκετ",
        "MINI MARKET" to "Σούπερ Μάρκετ",
        "ΜΙΝΙ ΜΑΡΚΕΤ" to "Σούπερ Μάρκετ",
        "ΠΑΝΤΟΠΩΛΕΙΟ" to "Σούπερ Μάρκετ",
        "PANTOPOLEIO" to "Σούπερ Μάρκετ",
        "ΒΙΒΛΙΟΠΩΛΕΙΟ" to "Αγορές & Τεχνολογία",
        "ΚΟΜΜΩΤΗΡΙΟ" to "Προσωπική Φροντίδα",
        "BARBERSHOP" to "Προσωπική Φροντίδα",
        "BARBER" to "Προσωπική Φροντίδα",
        "ΠΡΑΤΗΡΙΟ" to "Μετακινήσεις",
        "ΒΕΝΖΙΝΑΔΙΚΟ" to "Μετακινήσεις"
    )

    private val RECEIPT_DISQUALIFIED_KEYWORDS = listOf(
        "ΑΠΟΔΕΙΞΗ", "ΛΙΑΝΙΚΗΣ", "ΛΙΑΝΙΚΩΝ", "ΠΩΛΗΣΗΣ", "ΠΩΛΗΣΕΩΝ", "ΣΥΝΑΛΛΑΓΗΣ", "ΣΥΝΑΛΛΑΓΩΝ",
        "ΠΑΡΟΧΗΣ", "ΥΠΗΡΕΣΙΩΝ", "ΕΙΣΠΡΑΞΗΣ", "ΤΙΜΟΛΟΓΙΟ", "ΠΙΣΤΩΤΙΚΟ", "ΔΕΛΤΙΟ", "ΑΠΥ", "ΑΛΠ",
        "ΝΟΜΙΜΗ", "RECEIPT", "INVOICE", "TAX INVOICE", "CASH RECEIPT", "BILL",
        "ΤΑΜΕΙΑΚΗ", "ΜΗΧΑΝΗ", "ΦΟΡΟΛΟΓΙΚΟΣ", "ΜΗΧΑΝΙΣΜΟΣ", "ΦΗΜ", "ΑΔΗΜΕ", "ΑΡ. ΕΓΚΡΙΣΗΣ",
        "ΕΓΚΡΙΣΗ", "ΑΡ. ΣΕΙΡΑΣ", "SERIAL", "S/N", "REGISTER", "ΤΑΜΕΙΟ", "ΤΑΜΙΑΣ", "ΧΕΙΡΙΣΤΗΣ",
        "POS", "TERMINAL", "ΠΑΡΑΣΤΑΤΙΚΟ", "ΑΡ. ΠΑΡΑΣΤΑΤΙΚΟΥ",
        "ΚΑΛΩΣ ΗΡΘΑΤΕ", "ΚΑΛΩΣΗΡΘΑΤΕ", "ΕΥΧΑΡΙΣΤΟΥΜΕ", "WELCOME", "THANK YOU", "ΓΙΑ ΤΗΝ ΠΡΟΤΙΜΗΣΗ",
        "ΑΦΜ", "Α.Φ.Μ", "ΔΟΥ", "Δ.Ο.Υ", "ΓΕΜΗ", "Γ.Ε.Μ.Η", "TIN", "VAT", "IBAN", "TAX ID",
        "ΤΗΛ", "TEL", "PHONE", "FAX", "EMAIL", "WWW.", "HTTP", ".GR", ".COM", "INFO@",
        "ΟΔΟΣ", "ΟΔ.", "ΛΕΩΦ", "Λ.", "ΠΛΑΤΕΙΑ", "ΠΛ.", "Τ.Κ.", "TK",
        // Transliterated & Latin cashier / register / till tokens:
        "TAMEIO", "TAMEIA", "TAMIO", "TAMIAS", "TAM.", "ΤΑΜ.",
        "KAT:", "KAT.", "KAT ", "KATASTHMA", "KATASTHMATOS",
        "XEIRISTIS", "CHEIRISTIS", "XEIR.", "CLERK", "CASHIER",
        "TERMATIKO", "TERMATIKOY",
        "APODEIXI", "APODIXI", "LIANIKIS", "PWLHSHS", "POLISIS",
        "PARASΤATIKO", "PARASТATIKOY", "ATM30", "ATM", "TRX", "TXN",
        "ALLAGES", "ΑΛΛΑΓΕΣ", "ENTOS", "ΕΝΤΟΣ", "HMERWN", "ΗΜΕΡΩΝ",
        "YP/MA", "ΥΠ/ΜΑ", "EDRA", "ΕΔΡΑ"
    )

    /**
     * Primary parsing method supporting ML Kit Text (with spatial bounding boxes)
     * and raw string fallback, plus optional AADE QR data.
     */
    fun parseReceipt(
        visionText: Text?,
        rawText: String,
        aadeQr: String? = null,
        availableCategories: List<ExpenseCategory> = emptyList()
    ): ParsedReceiptData {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 1. Extract Store Name from the beginning of the receipt
        val (storeName, suggestedCategory) = extractStoreName(lines, rawText, availableCategories)

        // 2. Extract Total Amount (STRICT: only from the right of "ΣΥΝΟΛΟ" / "Σύνολο")
        var amount = extractTotalAmount(lines, visionText)

        // 3. Fallback amount from AADE QR if and only if OCR amount was missing (0.0)
        var merchantAfm = extractAfm(rawText)
        var timestamp = extractDate(rawText) ?: System.currentTimeMillis()

        if (!aadeQr.isNullOrBlank()) {
            val qrData = parseAadeQrInternal(aadeQr)
            if (amount <= 0.0 && qrData.amount > 0.0) {
                amount = qrData.amount
            }
            if (merchantAfm.isBlank() && qrData.merchantAfm.isNotBlank()) {
                merchantAfm = qrData.merchantAfm
            }
            if (qrData.timestamp > 0) {
                timestamp = qrData.timestamp
            }
        }

        // 4. Resolve Category against availableCategories
        val resolvedCategory = resolveCategory(suggestedCategory, availableCategories)

        return ParsedReceiptData(
            title = storeName,
            amount = amount,
            timestamp = timestamp,
            suggestedCategoryName = resolvedCategory,
            merchantAfm = merchantAfm,
            rawExtractedText = rawText
        )
    }

    /**
     * Backward-compatible parseOcrText method.
     */
    fun parseOcrText(
        text: String,
        availableCategories: List<ExpenseCategory> = emptyList()
    ): ParsedReceiptData {
        return parseReceipt(
            visionText = null,
            rawText = text,
            aadeQr = null,
            availableCategories = availableCategories
        )
    }

    /**
     * Backward-compatible parseAadeQr method.
     */
    fun parseAadeQr(
        qrText: String,
        availableCategories: List<ExpenseCategory> = emptyList()
    ): ParsedReceiptData {
        val qrData = parseAadeQrInternal(qrText)
        val ocrParsed = parseOcrText(qrText, availableCategories)
        val finalAmount = if (qrData.amount > 0.0) qrData.amount else ocrParsed.amount
        val finalAfm = if (qrData.merchantAfm.isNotBlank()) qrData.merchantAfm else ocrParsed.merchantAfm

        return ParsedReceiptData(
            title = if (ocrParsed.title.isNotBlank() && ocrParsed.title != "Αγορά απόδειξης") ocrParsed.title else "Απόδειξη",
            amount = finalAmount,
            timestamp = if (qrData.timestamp > 0) qrData.timestamp else System.currentTimeMillis(),
            suggestedCategoryName = ocrParsed.suggestedCategoryName.ifBlank { defaultCategory(availableCategories) },
            merchantAfm = finalAfm,
            rawExtractedText = qrText
        )
    }

    private val LEGAL_COMPANY_SUFFIXES = listOf(
        "ΚΑΙ ΣΙΑ ΟΕ", "ΚΑΙ ΣΙΑ Ο.Ε.", "ΚΑΙ ΣΙΑ ΕΕ", "ΚΑΙ ΣΙΑ Ε.Ε.", "& ΣΙΑ ΟΕ", "& ΣΙΑ Ο.Ε.",
        "& ΣΙΑ ΕΕ", "& ΣΙΑ Ε.Ε.", "ΚΑΙ ΣΙΑ", "& ΣΙΑ", "KAI SIA OE", "KAI SIA EE", "KAI SIA",
        "ΜΟΝΟΠΡΟΣΩΠΗ Α.Ε.", "ΜΟΝΟΠΡΟΣΩΠΗ ΑΕ", "ΜΟΝΟΠΡΟΣΩΠΗ ΙΚΕ", "ΜΟΝΟΠΡΟΣΩΠΗ Ι.Κ.Ε.",
        "MONOPROSWPH AE", "MONOPROSWPH IKE", "ΜΟΝ. ΙΚΕ", "Μ.Α.Ε.",
        "Α.Β.Ε.Ε.", "ΑΒΕΕ", "A.B.E.E.", "ABEE",
        "Α.Ε.Ε.", "ΑΕΕ", "A.E.E.", "AEE",
        "Α.Ε.", "ΑΕ", "A.E.", "AE",
        "Ο.Ε.", "ΟΕ", "O.E.", "OE",
        "Ε.Ε.", "ΕΕ", "E.E.", "EE",
        "Ε.Π.Ε.", "ΕΠΕ", "E.P.E.", "EPE",
        "Ι.Κ.Ε.", "ΙΚΕ", "I.K.E.", "IKE",
        "ΥΙΟΙ", "ΑΦΟΙ", "AFOI", "YIOI", "LTD", "SA", "LLC", "INC"
    )

    /**
     * Extracts store/merchant name from the top/beginning of the receipt lines.
     * Uses a generalized scoring algorithm to distinguish genuine commercial business titles
     * from graphic logo OCR artifacts, document headers, machine info, and addresses.
     */
    private fun extractStoreName(
        lines: List<String>,
        fullText: String,
        availableCategories: List<ExpenseCategory>
    ): Pair<String, String> {
        val topLines = lines.take(12)
        val upperTopText = topLines.joinToString("\n").uppercase(Locale.ROOT)
        val upperFullText = fullText.uppercase(Locale.ROOT)

        val afm = extractAfm(fullText)
        if (afm.isNotBlank()) {
            val merchantByAfm = GREEK_AFM_MERCHANTS[afm]
            if (merchantByAfm != null) {
                return Pair(merchantByAfm.displayName, detectCategoryFromContent(merchantByAfm.displayName, upperFullText, availableCategories))
            }
        }

        // 1. Direct Known Chain Match (check top lines first, then full receipt)
        for ((knownKey, merchant) in GREEK_KNOWN_MERCHANTS) {
            if (upperTopText.contains(knownKey)) {
                return Pair(merchant.displayName, detectCategoryFromContent(merchant.displayName, upperFullText, availableCategories))
            }
        }
        for ((knownKey, merchant) in GREEK_KNOWN_MERCHANTS) {
            if (upperFullText.contains(knownKey)) {
                return Pair(merchant.displayName, detectCategoryFromContent(merchant.displayName, upperFullText, availableCategories))
            }
        }

        // 2. Score candidate lines from top of the receipt
        data class CandidateScore(
            val line: String,
            val cleaned: String,
            val score: Int,
            val lineIndex: Int
        )

        val candidates = mutableListOf<CandidateScore>()

        for (i in topLines.indices) {
            val raw = topLines[i]
            val cleaned = cleanStoreNameLine(raw)
            if (!isAcceptableStoreCandidate(cleaned)) continue

            var score = 25 - (i * 2) // Strong preference for earlier lines

            val upper = cleaned.uppercase(Locale.ROOT)

            // Positive Indicator 1: Contains legal corporate suffix (ΟΕ, ΑΕ, ΙΚΕ, ΚΑΙ ΣΙΑ, etc.)
            val hasLegalSuffix = LEGAL_COMPANY_SUFFIXES.any { upper.contains(it) }
            if (hasLegalSuffix) score += 60

            // Positive Indicator 2: Contains store/trade descriptor (ΥΠΕΡΑΓΟΡΑ, ΦΟΥΡΝΟΣ, ΨΗΤΟΠΩΛΕΙΟ, κλπ.)
            val hasStoreType = GREEK_STORE_TYPE_CATEGORIES.keys.any { upper.contains(it) }
            if (hasStoreType) score += 60

            // Positive Indicator 3: Multi-word business title (2 to 5 words)
            val wordCount = cleaned.split(Regex("\\s+")).count { it.isNotBlank() }
            if (wordCount in 2..5) score += 30

            // Negative Indicator: Isolated short word (<= 4 chars with no vowels or strange consonant clusters)
            if (wordCount == 1 && cleaned.length <= 4) {
                score -= 50
            }

            candidates.add(CandidateScore(raw, cleaned, score, i))
        }

        if (candidates.isNotEmpty()) {
            val winner = candidates.maxByOrNull { it.score }!!
            val cleanedTitle = cleanStoreDisplayName(winner.cleaned)
            val formatted = toGreekTitleCase(cleanedTitle)
            val category = detectCategoryFromContent(formatted, upperFullText, availableCategories)
            return Pair(formatted, category)
        }

        // Fallback: If no candidate scored, take first acceptable candidate from lines
        for (l in lines.take(8)) {
            val cl = cleanStoreNameLine(l)
            if (isAcceptableStoreCandidate(cl) && cl.length in 3..40) {
                return Pair(toGreekTitleCase(cl), defaultCategory(availableCategories))
            }
        }
        return Pair("Απόδειξη", defaultCategory(availableCategories))
    }

    /**
     * Cleans legal suffixes and trading noise from the display store title.
     * e.g. "ΛΙΝΤΛ ΕΛΛΑΣ ΚΑΙ ΣΙΑ ΟΕ, ΥΠΕΡΑΓΟΡΑ" -> "ΛΙΝΤΛ ΕΛΛΑΣ"
     * e.g. "ΨΗΤΟΠΩΛΕΙΟ Ο ΜΠΑΜΠΗΣ Ο.Ε." -> "ΨΗΤΟΠΩΛΕΙΟ Ο ΜΠΑΜΠΗΣ"
     */
    private fun cleanStoreDisplayName(raw: String): String {
        var result = raw

        // Remove trailing commas and descriptors like ", ΥΠΕΡΑΓΟΡΑ", ", ΠΑΝΤΟΠΩΛΕΙΟ"
        val commaIdx = result.indexOf(',')
        if (commaIdx > 3) {
            val beforeComma = result.substring(0, commaIdx).trim()
            val afterComma = result.substring(commaIdx + 1).trim().uppercase(Locale.ROOT)
            val isDescriptorAfter = GREEK_STORE_TYPE_CATEGORIES.keys.any { afterComma.contains(it) } ||
                    LEGAL_COMPANY_SUFFIXES.any { afterComma.contains(it) }
            if (isDescriptorAfter && beforeComma.length >= 3) {
                result = beforeComma
            }
        }

        // Remove common legal suffixes from end of name
        for (suffix in LEGAL_COMPANY_SUFFIXES) {
            val regex = Regex("""(?i)\s+${Regex.escape(suffix)}\s*$""")
            if (regex.containsMatchIn(result)) {
                val stripped = regex.replace(result, "").trim()
                if (stripped.length >= 3) {
                    result = stripped
                }
            }
        }

        return result
    }

    private fun cleanStoreNameLine(line: String): String {
        return line
            .replace(Regex("[^a-zA-Zα-ωΑ-Ωά-ώΆ-Ώ0-9\\s&.'\"-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isAcceptableStoreCandidate(line: String): Boolean {
        if (line.length < 3) return false
        val upper = line.uppercase(Locale.ROOT)

        // Must have at least 2 letters
        val letterCount = line.count { it.isLetter() }
        if (letterCount < 2) return false

        // Digits shouldn't dominate (e.g. not a register code like Tameio 02 Kat 3624)
        val digitCount = line.count { it.isDigit() }
        if (digitCount >= 3 && digitCount.toDouble() / line.length > 0.25) return false
        if (digitCount > letterCount) return false

        // Disqualify cashier / till / machine patterns
        if (upper.contains("ΤΑΜΕΙΟ") || upper.contains("TAMEIO") ||
            upper.contains("ΧΕΙΡΙΣΤ") || upper.contains("XEIRIST") || upper.contains("CHEIRIST") ||
            upper.contains("ΤΕΡΜΑΤΙΚ") || upper.contains("TERMATIK") ||
            upper.contains("ΠΑΡΑΣΤΑΤΙΚ") || upper.contains("PARASTATIK") ||
            upper.contains("ΑΠΟΔΕΙΞΗ") || upper.contains("APODEIXH") ||
            upper.contains("ΕΙΔΟΣ") || upper.contains("EIDOS")
        ) {
            return false
        }

        val cleanWords = upper.replace(Regex("[^a-zA-Zα-ωΑ-Ω0-9]"), " ").split(Regex("\\s+")).filter { it.isNotBlank() }
        val cashierTokens = setOf("KAT", "ΚΑΤ", "TAM", "ΤΑΜ", "ATM", "POS", "CLERK", "STORE")
        if (cleanWords.any { cashierTokens.contains(it) } && cleanWords.any { it.all { ch -> ch.isDigit() } }) {
            return false
        }

        // Disqualify known receipt non-merchant headers
        for (kw in RECEIPT_DISQUALIFIED_KEYWORDS) {
            if (upper.contains(kw)) {
                return false
            }
        }

        // Disqualify postal codes or address patterns
        if (upper.contains(Regex("""\b\d{5}\b""")) || upper.contains(Regex("""\b\d{3}\s\d{2}\b"""))) {
            return false
        }

        return true
    }

    private val ACCENTED_GREEK_WORDS = mapOf(
        "ΨΗΤΟΠΩΛΕΙΟ" to "Ψητοπωλείο",
        "ΨΗΤΟΠΩΛΕΙΑ" to "Ψητοπωλεία",
        "ΦΑΡΜΑΚΕΙΟ" to "Φαρμακείο",
        "ΑΡΤΟΠΟΙΕΙΟ" to "Αρτοποιείο",
        "ΑΡΤΟΠΟΙΙΑ" to "Αρτοποιία",
        "ΦΟΥΡΝΟΣ" to "Φούρνος",
        "ΖΑΧΑΡΟΠΛΑΣΤΕΙΟ" to "Ζαχαροπλαστείο",
        "ΚΑΦΕΚΟΠΤΕΙΟ" to "Καφεκοπτείο",
        "ΚΑΦΕΤΕΡΙΑ" to "Καφετέρια",
        "ΕΣΤΙΑΤΟΡΙΟ" to "Εστιατόριο",
        "ΤΑΒΕΡΝΑ" to "Ταβέρνα",
        "ΟΒΕΛΙΣΤΗΡΙΟ" to "Οβελιστήριο",
        "ΨΗΣΤΑΡΙΑ" to "Ψησταριά",
        "ΚΡΕΟΠΩΛΕΙΟ" to "Κρεοπωλείο",
        "ΙΧΘΥΟΠΩΛΕΙΟ" to "Ιχθυοπωλείο",
        "ΟΠΩΡΟΠΩΛΕΙΟ" to "Οπωροπωλείο",
        "ΜΑΝΑΒΙΚΟ" to "Μανάβικο",
        "ΒΙΒΛΙΟΠΩΛΕΙΟ" to "Βιβλιοπωλείο",
        "ΚΟΜΜΩΤΗΡΙΟ" to "Κομμωτήριο",
        "ΠΡΑΤΗΡΙΟ" to "Πρατήριο",
        "ΒΕΝΖΙΝΑΔΙΚΟ" to "Βενζινάδικο",
        "ΣΟΥΠΕΡ" to "Σούπερ",
        "ΜΑΡΚΕΤ" to "Μάρκετ",
        "ΜΙΝΙ" to "Μίνι",
        "ΠΑΝΤΟΠΩΛΕΙΟ" to "Παντοπωλείο",
        "ΠΑΝΤΟΠΩΛΕΙΑ" to "Παντοπωλεία",
        "ΥΠΕΡΑΓΟΡΑ" to "Υπεραγορά",
        "ΕΜΠΟΡΙΟ" to "Εμπόριο",
        "ΚΑΤΑΣΤΗΜΑ" to "Κατάστημα",
        "ΓΩΝΙΑ" to "Γωνία"
    )

    /**
     * Converts Greek / Latin words to Title Case while preserving uppercase corporate forms
     * and accented dictionary store types.
     */
    private fun toGreekTitleCase(text: String): String {
        val uppercaseForms = setOf(
            "ΑΕ", "ΟΕ", "ΕΕ", "ΕΠΕ", "ΙΚΕ", "ΑΕΕ", "ΑΒΕΕ", "LTD", "SA", "PC", "INC", "LLC", "AB", "ΑΒ"
        )
        return text.split(Regex("\\s+")).filter { it.isNotBlank() }.joinToString(" ") { word ->
            val stripped = word.replace(Regex("[^a-zA-Zα-ωΑ-Ω0-9]"), "").uppercase(Locale.ROOT)
            if (stripped in uppercaseForms) {
                word.uppercase(Locale.ROOT)
            } else if (ACCENTED_GREEK_WORDS.containsKey(stripped)) {
                ACCENTED_GREEK_WORDS[stripped]!!
            } else {
                word.lowercase(Locale.ROOT).replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.ROOT) else ch.toString()
                }
            }
        }
    }

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

    private fun findSynoloMatchEnd(line: String): Int? {
        val normLine = removeGreekAccents(line).uppercase(Locale.ROOT)
        // Disqualify discount / savings / loyalty points lines
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
            if (isIgnoredModifier) {
                return null
            }
            return match.range.last + 1
        }
        // Also check individual words with unaccented Levenshtein distance <= 1
        val words = line.split(Regex("\\s+"))
        var currIdx = 0
        for (w in words) {
            val start = line.indexOf(w, currIdx)
            val unaccented = removeGreekAccents(w)
            val cleaned = unaccented.replace(Regex("[^a-zA-Zα-ωΑ-Ω0-9]"), "").uppercase(Locale.ROOT)
            if (cleaned.length in 5..8) {
                if (isFuzzySynolo(cleaned)) {
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
            }
            if (start >= 0) {
                currIdx = start + w.length
            }
        }
        return null
    }

    private fun isFuzzySynolo(word: String): Boolean {
        val targets = listOf(
            "ΣΥΝΟΛΟ", "ΣΥΝΟΛΟΝ", "EYNOAO", "ZYNOAO", "SYNOAO", "SYNOLO", "TOTAL", "ΣΥΝΟΑΟ",
            "EVVOLO", "ZVVOLO", "EVVOIO", "ZVVOIO", "SVVOLO", "EUVODO", "EVNOLO", "ZVNOLO",
            "ZVVOTO", "EVVOTO", "ZUVOTO", "EUVOTO", "ZUVODO", "ΣΥΝΟΛΙΚΗ", "SYNOLIKH"
        )
        for (target in targets) {
            if (levenshteinDistance(word, target) <= 1) {
                return true
            }
        }
        return false
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
    }

    /**
     * Extracts the total amount STRICTLY from the right of the word "ΣΥΝΟΛΟ" or "Σύνολο".
     * No other numbers from the receipt are ever used.
     */
    private fun extractTotalAmount(lines: List<String>, visionText: Text? = null): Double {
        // 1. Scan lines from bottom to top for same-line total
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            val matchEnd = findSynoloMatchEnd(line) ?: continue

            // Substring immediately to the right of "ΣΥΝΟΛΟ" / "Σύνολο"
            val textToRight = line.substring(matchEnd).trim()
            if (textToRight.isNotBlank()) {
                val matches = PRICE_REGEX.findAll(textToRight).toList()
                if (matches.isNotEmpty()) {
                    // Take the last/rightmost price (handles VAT tables where total/gross is the rightmost column)
                    val decimalMatch = matches.last()
                    val parsed = parseGreekPrice(decimalMatch.value)
                    if (parsed != null && parsed > 0.0) {
                        return parsed
                    }
                }
            }

            // 2. Spatial check with visionText if available (2-column layout)
            if (visionText != null) {
                val spatialAmount = findPriceToTheRightSpatially(visionText)
                if (spatialAmount != null && spatialAmount > 0.0) {
                    return spatialAmount
                }
            }

            // 3. Check subsequent lines (within 8 lines, skipping divider lines like "----" or "=====")
            for (offset in 1..8) {
                if (i + offset < lines.size) {
                    val candidateLine = lines[i + offset].trim()
                    // Skip divider lines
                    if (candidateLine.replace(Regex("[-=_*]"), "").isBlank()) continue

                    val upperCand = candidateLine.uppercase(Locale.ROOT)
                    // Skip cash and change lines if looking for grand total
                    if (upperCand.contains("ΜΕΤΡΗΤ") || upperCand.contains("METRHT") ||
                        upperCand.contains("ΡΕΣΤ") || upperCand.contains("REST")
                    ) {
                        continue
                    }

                    val nextMatches = PRICE_REGEX.findAll(candidateLine).toList()
                    if (nextMatches.isNotEmpty()) {
                        val parsed = parseGreekPrice(nextMatches.last().value)
                        if (parsed != null && parsed > 0.0) {
                            return parsed
                        }
                    }
                }
            }
        }

        // If visionText was provided, try spatial search across all blocks as fallback
        if (visionText != null) {
            val spatialAmount = findPriceToTheRightSpatially(visionText)
            if (spatialAmount != null && spatialAmount > 0.0) {
                return spatialAmount
            }
        }

        // STRICT REQUIREMENT: If not found to the right of "ΣΥΝΟΛΟ" / "Σύνολο", return 0.0
        return 0.0
    }

    /**
     * Inspects ML Kit bounding boxes to find the price printed to the right of "ΣΥΝΟΛΟ"
     * on the same vertical baseline (handling 2-column printed receipts, wrinkles, folds, and slanted photos).
     */
    private fun findPriceToTheRightSpatially(visionText: Text): Double? {
        data class PriceCandidate(
            val amount: Double,
            val rect: android.graphics.Rect
        )
        val allPrices = mutableListOf<PriceCandidate>()

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val upperLine = line.text.uppercase(Locale.ROOT)
                // Exclude cash tendered and change lines
                if (upperLine.contains("ΜΕΤΡΗΤ") || upperLine.contains("METRHT") ||
                    upperLine.contains("ΡΕΣΤ") || upperLine.contains("REST") ||
                    upperLine.contains("CHANGE")
                ) {
                    continue
                }

                // First check line-level elements for fine-grained bounding boxes
                for (element in line.elements) {
                    val box = element.boundingBox ?: continue
                    for (match in PRICE_REGEX.findAll(element.text)) {
                        val amt = parseGreekPrice(match.value)
                        if (amt != null && amt > 0.0) {
                            allPrices.add(PriceCandidate(amt, box))
                        }
                    }
                }

                // Also record line-level bounding box
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

                // Check if price is in line.text to the right of matchEnd
                val textToRight = line.text.substring(matchEnd).trim()
                if (textToRight.isNotBlank()) {
                    val inlineMatches = PRICE_REGEX.findAll(textToRight).toList()
                    if (inlineMatches.isNotEmpty()) {
                        val parsed = parseGreekPrice(inlineMatches.last().value)
                        if (parsed != null && parsed > 0.0) {
                            return parsed
                        }
                    }
                }

                // Generous vertical tolerance to handle paper wrinkles and photo tilt on high-resolution cameras (up to 550px)
                val lineToleranceY = (lineBox.height() * 8.0).toInt().coerceIn(350, 600)

                // Find prices whose center is to the right of the synolo text start
                val matchingPrices = allPrices.filter { cand ->
                    cand.rect.centerX() > lineBox.left &&
                    Math.abs(cand.rect.centerY() - lineCenterY) <= lineToleranceY
                }

                if (matchingPrices.isNotEmpty()) {
                    // Pick the candidate closest vertically to the synolo line
                    val best = matchingPrices.minByOrNull { Math.abs(it.rect.centerY() - lineCenterY) }
                    if (best != null && best.amount > 0.0) {
                        return best.amount
                    }
                }
            }
        }
        return null
    }

    /**
     * Parses Greek numbers handling decimal commas and dots.
     */
    private fun parseGreekPrice(raw: String): Double? {
        val cleaned = raw
            .replace("€", "")
            .replace("EUR", "", ignoreCase = true)
            .replace("ΕΥΡΩ", "", ignoreCase = true)
            .replace(":", "")
            .replace(" ", "")
            .trim()

        if (cleaned.isBlank()) return null

        // Format 1.250,50 or 1,250.50
        if (cleaned.contains(".") && cleaned.contains(",")) {
            return if (cleaned.lastIndexOf(",") > cleaned.lastIndexOf(".")) {
                cleaned.replace(".", "").replace(",", ".").toDoubleOrNull()
            } else {
                cleaned.replace(",", "").toDoubleOrNull()
            }
        }

        // Single comma separator (Greek standard)
        if (cleaned.contains(",")) {
            return cleaned.replace(",", ".").toDoubleOrNull()
        }

        return cleaned.toDoubleOrNull()
    }

    private fun extractAfm(text: String): String {
        val afmRegex = Regex("""(?i)(?:ΑΦΜ|Α\.Φ\.Μ\.?|TIN|VAT)[:\s]*([0-9]{9})""")
        val match = afmRegex.find(text)
        return match?.groupValues?.get(1) ?: ""
    }

    private fun extractDate(text: String): Long? {
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

    private data class InternalQrData(
        val amount: Double = 0.0,
        val merchantAfm: String = "",
        val timestamp: Long = 0L
    )

    private fun parseAadeQrInternal(qrText: String): InternalQrData {
        var amount = 0.0
        var merchantAfm = ""
        var timestamp = 0L

        // Match amount in QR string (e.g. amount=14.50 or val=14.50 or poso=14,50 or tot=14.50)
        val amountRegex = Regex("(?:amount|poso|total|sum|val|tot|price)=([0-9]+(?:[.,][0-9]{1,2})?)", RegexOption.IGNORE_CASE)
        val amountMatch = amountRegex.find(qrText)
        if (amountMatch != null) {
            val raw = amountMatch.groupValues[1].replace(',', '.')
            amount = raw.toDoubleOrNull() ?: 0.0
        }

        // Match AFM in QR string (e.g. tin=094123456 or afm=094123456)
        val afmRegex = Regex("(?:tin|afm|vat)=([0-9]{9})", RegexOption.IGNORE_CASE)
        val afmMatch = afmRegex.find(qrText)
        if (afmMatch != null) {
            merchantAfm = afmMatch.groupValues[1]
        }

        // Match date in QR string
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

    private fun resolveCategory(
        suggested: String,
        availableCategories: List<ExpenseCategory>
    ): String {
        if (availableCategories.isEmpty()) {
            return suggested.ifBlank { "Σούπερ Μάρκετ" }
        }
        if (suggested.isNotBlank()) {
            val matched = availableCategories.find {
                it.name.contains(suggested, ignoreCase = true) ||
                suggested.contains(it.name, ignoreCase = true)
            }
            if (matched != null) {
                return matched.name
            }
            return suggested
        }
        return defaultCategory(availableCategories)
    }

    private fun matchCategoryByName(
        storeName: String,
        categories: List<ExpenseCategory>
    ): String {
        val norm = storeName.lowercase(Locale.ROOT)
        for (cat in categories) {
            val catNorm = cat.name.lowercase(Locale.ROOT)
            if (norm.contains(catNorm) || catNorm.contains(norm)) {
                return cat.name
            }
        }
        return ""
    }

    private fun detectCategoryFromContent(
        storeName: String,
        fullText: String,
        categories: List<ExpenseCategory>
    ): String {
        val combined = "$storeName $fullText".uppercase(Locale.ROOT)

        // 1. Pharmacy / Health
        if (combined.contains("ΦΑΡΜΑΚ") || combined.contains("PHARM")) {
            return findCategory(listOf("Υγεία", "Φαρμακείο", "Health"), "Υγεία & Φαρμακείο", categories)
        }

        // 2. Supermarket / Grocery
        if (combined.contains("ΣΟΥΠΕΡ") || combined.contains("SUPER") || combined.contains("MARKET") ||
            combined.contains("ΜΑΡΚΕΤ") || combined.contains("ΠΑΝΤΟΠΩΛ") || combined.contains("ΜΑΝΑΒ") ||
            combined.contains("ΚΡΕΟΠ") || combined.contains("ΙΧΘΥ") || combined.contains("ΥΠΕΡΑΓΟΡ") ||
            combined.contains("ΤΡΟΦΙΜ") || combined.contains("LIDL") || combined.contains("ΛΙΝΤΛ") ||
            combined.contains("ΣΚΛΑΒΕΝΙΤ") || combined.contains("ΜΑΣΟΥΤ") || combined.contains("ΒΑΣΙΛΟΠ")
        ) {
            return findCategory(listOf("Σούπερ", "Super", "Τρόφιμα", "Market"), "Σούπερ Μάρκετ", categories)
        }

        // 3. Cafe & Drinks
        if (combined.contains("ΚΑΦΕ") || combined.contains("CAFE") || combined.contains("COFFEE") ||
            combined.contains("ΜΠΑΡ") || combined.contains("BAR") || combined.contains("EVEREST") ||
            combined.contains("ΓΡΗΓΟΡ") || combined.contains("GREGORY") || combined.contains("MIKEL")
        ) {
            return findCategory(listOf("Καφές", "Καφέδες", "Coffee", "Έξω", "Ποτά"), "Καφές & Έξω", categories)
        }

        // 4. Food & Bakery & Delivery
        if (combined.contains("ΦΟΥΡΝ") || combined.contains("ΑΡΤΟΠ") || combined.contains("BAKERY") ||
            combined.contains("ΖΑΧΑΡ") || combined.contains("ΕΣΤΙΑΤ") || combined.contains("ΤΑΒΕΡΝ") ||
            combined.contains("ΨΗΤΟΠ") || combined.contains("ΟΒΕΛ") || combined.contains("PIZZ") ||
            combined.contains("BURGER") || combined.contains("DELIVERY") || combined.contains("EFOOD") ||
            combined.contains("WOLT") || combined.contains("BOX") || combined.contains("GOODY") ||
            combined.contains("MCDONALD") || combined.contains("KFC") || combined.contains("VENETI") ||
            combined.contains("ΒΕΝΕΤ")
        ) {
            return findCategory(listOf("Φαγητό", "Delivery", "Εστιατόριο", "Food"), "Φαγητό & Delivery", categories)
        }

        // 5. Fuel & Transit
        if (combined.contains("ΠΡΑΤΗΡ") || combined.contains("ΒΕΝΖΙΝ") || combined.contains("SHELL") ||
            combined.contains("BP") || combined.contains("EKO") || combined.contains("AVIN") ||
            combined.contains("REVOIL") || combined.contains("ELIN") || combined.contains("ΕΛΙΝ") ||
            combined.contains("ΚΑΥΣΙΜ") || combined.contains("GAS")
        ) {
            return findCategory(listOf("Μετακινήσεις", "Καύσιμα", "Βενζίνη", "Transit", "Fuel"), "Μετακινήσεις", categories)
        }

        // 6. Technology & Retail
        if (combined.contains("PUBLIC") || combined.contains("ΠΛΑΙΣΙΟ") || combined.contains("PLAISIO") ||
            combined.contains("ΚΩΤΣΟΒ") || combined.contains("KOTSOV") || combined.contains("ΓΕΡΜΑΝ") ||
            combined.contains("GERMAN") || combined.contains("TECH") || combined.contains("IKEA") ||
            combined.contains("JUMBO") || combined.contains("ΒΙΒΛΙΟ")
        ) {
            return findCategory(listOf("Αγορές", "Τεχνολογία", "Ηλεκτρικά", "Shopping"), "Αγορές & Τεχνολογία", categories)
        }

        // 7. Clothing & Fashion
        if (combined.contains("ZARA") || combined.contains("H&M") || combined.contains("BERSHKA") ||
            combined.contains("STRADIVARIUS") || combined.contains("PULL") || combined.contains("OYSHO") ||
            combined.contains("MASSIMO") || combined.contains("ΕΝΔΥΣ") || combined.contains("ΥΠΟΔΗΣ")
        ) {
            return findCategory(listOf("Ένδυση", "Υπόδηση", "Ρούχα", "Fashion"), "Ένδυση & Υπόδηση", categories)
        }

        // 8. Personal Care / Barber
        if (combined.contains("ΚΟΜΜΩΤ") || combined.contains("BARBER") || combined.contains("BEAUTY") ||
            combined.contains("HAIR") || combined.contains("HONDOS") || combined.contains("SEPHORA")
        ) {
            return findCategory(listOf("Προσωπική", "Φροντίδα", "Ομορφιά", "Beauty"), "Προσωπική Φροντίδα", categories)
        }

        // 9. Utilities & Bills
        if (combined.contains("ΔΕΗ") || combined.contains("DEI") || combined.contains("ΕΥΔΑΠ") ||
            combined.contains("EYDAP") || combined.contains("ΕΥΑΘ") || combined.contains("COSMOTE") ||
            combined.contains("VODAFONE") || combined.contains("NOVA") || combined.contains("PROTERGIA")
        ) {
            return findCategory(listOf("Λογαριασμοί", "Ρεύμα", "Νερό", "Τηλεφωνία", "Bills"), "Λογαριασμοί", categories)
        }

        return defaultCategory(categories)
    }

    private fun findCategory(keywords: List<String>, defaultName: String, categories: List<ExpenseCategory>): String {
        if (categories.isNotEmpty()) {
            for (kw in keywords) {
                val matched = categories.find { it.name.contains(kw, ignoreCase = true) }
                if (matched != null) return matched.name
            }
        }
        return defaultName
    }

    private fun defaultCategory(categories: List<ExpenseCategory>): String {
        return categories.firstOrNull {
            it.name.contains("Σούπερ", ignoreCase = true) ||
            it.name.contains("Super", ignoreCase = true)
        }?.name ?: categories.firstOrNull()?.name ?: "Σούπερ Μάρκετ"
    }
}
