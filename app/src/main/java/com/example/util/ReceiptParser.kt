package com.example.util

import com.example.data.entity.ExpenseCategory
import com.google.mlkit.vision.text.Text
import java.util.Locale

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
        availableCategories: List<ExpenseCategory> = emptyList(),
        learnedCategorySuggester: ((merchantTitle: String, merchantAfm: String) -> String?)? = null
    ): ParsedReceiptData {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }

        // 1. Extract Store Name from the beginning of the receipt
        val (storeName, suggestedCategory) = extractStoreName(lines, rawText, availableCategories)

        // 2. Extract Total Amount (STRICT: only from the right of "ΣΥΝΟΛΟ" / "Σύνολο")
        var amount = ReceiptAmountAndQrParser.extractTotalAmount(lines, visionText)

        // 3. Fallback amount from AADE QR if and only if OCR amount was missing (0.0)
        var merchantAfm = ReceiptAmountAndQrParser.extractAfm(rawText)
        var timestamp = ReceiptAmountAndQrParser.extractDate(rawText) ?: System.currentTimeMillis()

        if (!aadeQr.isNullOrBlank()) {
            val qrData = ReceiptAmountAndQrParser.parseAadeQrInternal(aadeQr)
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

        // 4. Prefer learned category when available, otherwise resolve via heuristic rules
        val learnedCat = learnedCategorySuggester?.invoke(storeName, merchantAfm)?.takeIf { it.isNotBlank() }
        val resolvedCategory = if (learnedCat != null) {
            resolveCategory(learnedCat, availableCategories)
        } else {
            resolveCategory(suggestedCategory, availableCategories)
        }

        return ParsedReceiptData(
            title = storeName,
            amount = amount,
            timestamp = timestamp,
            suggestedCategoryName = resolvedCategory,
            merchantAfm = merchantAfm,
            rawExtractedText = rawText
        )
    }

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

    fun parseAadeQr(
        qrText: String,
        availableCategories: List<ExpenseCategory> = emptyList()
    ): ParsedReceiptData {
        val qrData = ReceiptAmountAndQrParser.parseAadeQrInternal(qrText)
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

        val afm = ReceiptAmountAndQrParser.extractAfm(fullText)
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
