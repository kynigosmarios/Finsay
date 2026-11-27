package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Emerald700
import com.example.util.Localization

object LegalConstants {
    const val CURRENT_LEGAL_VERSION = "2026-11"
}

private data class LegalSectionData(
    val title: String,
    val body: String
)

@Composable
fun LegalDocumentsDialog(
    initialTab: String = "TERMS", // "TERMS" or "PRIVACY"
    appLanguage: String = "EL",
    onDismiss: () -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)
    var selectedTab by remember { mutableIntStateOf(if (initialTab.equals("PRIVACY", ignoreCase = true)) 1 else 0) }
    val listState = rememberLazyListState()

    LaunchedEffect(initialTab) {
        selectedTab = if (initialTab.equals("PRIVACY", ignoreCase = true)) 1 else 0
    }

    LaunchedEffect(selectedTab, appLanguage) {
        listState.scrollToItem(0)
    }

    val sections = remember(selectedTab, lang) {
        if (selectedTab == 0) {
            termsOfServiceSections(lang)
        } else {
            privacyPolicySections(lang)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("legal_documents_dialog")
    ) {
        // Page Header Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
            tonalElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("legal_dialog_close_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = Localization.tr(lang, "Back", "Επιστροφή"),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Emerald700.copy(alpha = 0.12f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Default.Description else Icons.Default.Security,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedTab == 0) {
                            Localization.tr(lang, "Terms & Conditions", "Όροι και προϋποθέσεις")
                        } else {
                            Localization.tr(lang, "Privacy Policy", "Πολιτική απορρήτου")
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tabs: Terms & Conditions / Privacy Policy
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = Emerald700,
                    height = 3.dp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .heightIn(min = 48.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                modifier = Modifier.heightIn(min = 48.dp),
                text = {
                    Text(
                        text = Localization.tr(lang, "Terms & Conditions", "Όροι και προϋποθέσεις"),
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 0) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                modifier = Modifier.heightIn(min = 48.dp),
                text = {
                    Text(
                        text = Localization.tr(lang, "Privacy Policy", "Πολιτική απορρήτου"),
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedTab == 1) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scrollable Legal Text Content (LazyColumn ensures complete, smooth scrolling through all sections)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("legal_documents_list"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(
                items = sections,
                key = { "${selectedTab}_${lang.name}_${it.title}" }
            ) { section ->
                LegalSection(title = section.title, body = section.body)
            }
        }
    }
}

private fun privacyPolicySections(lang: Localization.Language): List<LegalSectionData> {
    fun t(en: String, el: String) = Localization.tr(lang, en, el)
    return listOf(
        LegalSectionData(
            title = t(
                "1. Scope & Data Controller (Updated: November 2026)",
                "1. Πεδίο Εφαρμογής & Υπεύθυνος Επεξεργασίας (Ενημέρωση: Νοέμβριος 2026)"
            ),
            body = t(
                "This Privacy Policy explains how Finsay (finsayapp.com, package com.kynigos.oikonomos) collects, uses, processes, stores, and protects your personal and financial tracking data. It is designed to comply with the EU General Data Protection Regulation (GDPR 2016/679), Greek Law 4624/2019, the UK GDPR, applicable United States privacy statutes (including the California CCPA/CPRA, Virginia VCDPA, Colorado CPA, Connecticut CTDPA, and Texas TDPSA), and Google Play Developer Program Policies. Data Controller contact: support@finsayapp.com.",
                "Η παρούσα Πολιτική Απορρήτου περιγράφει πώς η εφαρμογή Finsay (finsayapp.com, πακέτο com.kynigos.oikonomos) συλλέγει, χρησιμοποιεί, αποθηκεύει και προστατεύει τα προσωπικά και οικονομικά δεδομένα σας. Είναι σχεδιασμένη σε συμμόρφωση με τον Γενικό Κανονισμό Προστασίας Δεδομένων της ΕΕ (GDPR 2016/679), τον ελληνικό Νόμο 4624/2019, το UK GDPR, την εφαρμοστέα νομοθεσία ιδιωτικότητας των ΗΠΑ (συμπεριλαμβανομένων των CCPA/CPRA Καλιφόρνιας, VCDPA, CPA, CTDPA, TDPSA) και τις Πολιτικές Προγραμματιστών του Google Play. Επικοινωνία Υπεύθυνου Επεξεργασίας: support@finsayapp.com."
            )
        ),
        LegalSectionData(
            title = t(
                "2. Categories of Data Collected & Profile Isolation",
                "2. Κατηγορίες Δεδομένων & Απομόνωση Ιδιωτικού Προφίλ"
            ),
            body = t(
                "• Account & Authentication Data: Name, email address, unique user identifier (UID), and profile photo obtained via Google Sign-In (Android Credential Manager) or Email/Password through Firebase Authentication.\n• Private Profile Details (Strictly Owner-Only): Optional fields such as phone number, date of birth, occupation, and city are isolated in a private sub-document readable and writable exclusively by your authenticated account—never exposed to other users during ledger-sharing lookups.\n• Financial & Budget Records: Amounts, titles/descriptions, categories, dates, payment sources, selected ledger currency, incomes, monthly budgets, recurring bills, savings goals, and member split/settle-up entries voluntarily logged by you.\n• Shared Household Data: Member nicknames, roles (Creator, Editor, Registar), shared ledger links, and an activity log of ledger edits visible to authorized members of that ledger.\n• Preferences & Local Settings: Language (EL/EN), theme mode, reminder schedules, and local merchant-to-category memory.",
                "• Στοιχεία Λογαριασμού & Ταυτοποίησης: Όνομα, διεύθυνση email, μοναδικό αναγνωριστικό χρήστη (UID) και φωτογραφία προφίλ μέσω Google Sign-In (Android Credential Manager) ή Email/Κωδικού μέσω Firebase Authentication.\n• Ευαίσθητα Στοιχεία Προφίλ (Αποκλειστικά Ιδιωτικά): Προαιρετικά πεδία όπως τηλέφωνο, ημερομηνία γέννησης, επάγγελμα και πόλη αποθηκεύονται απομονωμένα σε ιδιωτικό υπο-έγγραφο προσβάσιμο αποκλειστικά από τον ίδιο τον χρήστη και δεν εκτίθενται ποτέ σε άλλους χρήστες κατά την αναζήτηση διαμοιρασμού.\n• Οικονομικές Εγγραφές & Προϋπολογισμός: Ποσά, τίτλοι/περιγραφές, κατηγορίες, ημερομηνίες, τρόποι πληρωμής, νόμισμα εξοδολογίου, έσοδα, μηνιαία όρια, πάγιοι λογαριασμοί, στόχοι αποταμίευσης και επιμερισμοί/εκκαθαρίσεις μελών που καταχωρείτε εθελοντικά.\n• Κοινά Εξοδολόγια: Ψευδώνυμα μελών, ρόλοι (Δημιουργός, Συντάκτης, Καταχωρητής) και ιστορικό δραστηριότητας ορατό μόνο στα εξουσιοδοτημένα μέλη του εκάστοτε εξοδολογίου.\n• Προτιμήσεις Συσκευής: Γλώσσα (ΕΛ/EN), θέμα εμφάνισης, υπενθυμίσεις και τοπική μνήμη κατηγορίας εμπόρων."
            )
        ),
        LegalSectionData(
            title = t(
                "3. Bank & Wallet Push Notification Reader (Optional, 100% On-Device)",
                "3. Ανάγνωση Τραπεζικών Ειδοποιήσεων Push (Προαιρετική, 100% Τοπικά)"
            ),
            body = t(
                "• Strictly Opt-In: The Bank Notification Auto-Catcher (Android NotificationListenerService) is completely disabled by default. It activates ONLY after you review our in-app Prominent Disclosure and manually grant Notification Access in Android System Settings.\n• Strict Filtering & On-Device Parsing: Finsay filters solely for recognized banking and digital wallet notification packages (ignoring personal messages, chats, emails, and OTP/2FA codes). Merchant name and transaction amount are parsed 100% locally in device memory.\n• Zero Transmission & Explicit Save: Raw notification text is NEVER uploaded to Finsay servers, logged externally, or shared with third parties. Detected alerts create a local draft only; an expense is saved and synced to the cloud ONLY if you actively tap Save/Confirm.\n• No Bank Credentials / No Open Banking: Finsay never collects banking logins, passwords, card numbers, PINs, or PSD2/Open Banking tokens.\n• Revocation: You may disable this feature anytime in App Settings or Android System Settings.",
                "• Αυστηρά Προαιρετική (Opt-In): Η λειτουργία αυτόματης ανίχνευσης τραπεζικών ειδοποιήσεων (Android NotificationListenerService) είναι απενεργοποιημένη από προεπιλογή. Ενεργοποιείται ΜΟΝΟ αφού διαβάσετε την εμφανή γνωστοποίηση (Prominent Disclosure) εντός εφαρμογής και δώσετε ρητά άδεια Πρόσβασης Ειδοποιήσεων στις Ρυθμίσεις Android.\n• Αυστηρό Φιλτράρισμα & Τοπική Επεξεργασία: Το Finsay φιλτράρει αποκλειστικά αναγνωρισμένες τραπεζικές εφαρμογές και ψηφιακά πορτοφόλια (αγνοεί προσωπικά μηνύματα, συνομιλίες, email και κωδικούς OTP/2FA). Η εξαγωγή εμπόρου και ποσού εκτελείται 100% τοπικά στη μνήμη της συσκευής.\n• Μηδενική Μετάδοση & Ρητή Αποθήκευση: Το ακατέργαστο κείμενο των ειδοποιήσεων ΔΕΝ αποστέλλεται ποτέ σε διακομιστές του Finsay ούτε σε τρίτους. Δημιουργείται μόνο τοπικό προσχέδιο· ένα έξοδο αποθηκεύεται και συγχρονίζεται στο cloud ΜΟΝΟ αν πατήσετε εσείς Αποθήκευση/Επιβεβαίωση.\n• Καμία Πρόσβαση σε e-Banking / PSD2: Το Finsay δεν ζητά και δεν αποθηκεύει ποτέ κωδικούς e-Banking, PIN, αριθμούς καρτών ή διαπιστευτήρια Open Banking/PSD2.\n• Ανάκληση: Μπορείτε να την απενεργοποιήσετε ανά πάσα στιγμή από τις Ρυθμίσεις Εφαρμογής ή τις Ρυθμίσεις Android."
            )
        ),
        LegalSectionData(
            title = t(
                "4. Camera, Receipt OCR, AADE/e-Invoicing QR Lookup & Voice Entry",
                "4. Κάμερα, Σάρωση Αποδείξεων (OCR), QR ΑΑΔΕ & Φωνητική Καταχώρηση"
            ),
            body = t(
                "• Camera & Photo Picker: Camera access (CAMERA) or the zero-permission Android Photo Picker is used solely when you choose to scan a receipt or QR code. OCR text recognition and barcode decoding run locally on-device via Google ML Kit; receipt images are not uploaded to Finsay servers.\n• Greek AADE / e-Invoicing QR Verification: When you scan a Greek tax/e-invoicing QR code, the app may perform a direct, read-only HTTPS lookup strictly to allowlisted official tax/e-invoicing domains (e.g., aade.gr and certified Greek e-invoicing providers) solely to retrieve merchant name, VAT/AFM, date, and total amount.\n• Voice Expense Entry (RECORD_AUDIO): Used only when you press the microphone button. Speech-to-text relies on Android's native SpeechRecognizer service (subject to your device/Google speech service terms), and the resulting text is parsed locally into a draft expense. Finsay does NOT record, save, or upload audio files.",
                "• Κάμερα & Επιλογέας Φωτογραφιών: Η άδεια κάμερας (CAMERA) ή ο επιλογέας Android Photo Picker (χωρίς πρόσβαση σε όλη τη συλλογή) χρησιμοποιούνται αποκλειστικά όταν σαρώνετε απόδειξη ή QR. Η αναγνώριση κειμένου (OCR) και QR εκτελείται τοπικά στη συσκευή μέσω Google ML Kit· οι φωτογραφίες αποδείξεων δεν ανεβαίνουν σε servers του Finsay.\n• Επαλήθευση QR ΑΑΔΕ / Παρόχων Ηλεκτρονικής Τιμολόγησης: Κατά τη σάρωση ελληνικού φορολογικού QR, η εφαρμογή ενδέχεται να πραγματοποιήσει απευθείας κλήση ανάγνωσης (HTTPS) αυστηρά σε εγκεκριμένα domains (π.χ. aade.gr και πιστοποιημένους παρόχους ηλεκτρονικής τιμολόγησης) αποκλειστικά για ανάκτηση επωνυμίας εμπόρου, ΑΦΜ, ημερομηνίας και συνολικού ποσού.\n• Φωνητική Καταχώρηση (RECORD_AUDIO): Ενεργοποιείται μόνο όταν πατάτε το κουμπί μικροφώνου. Χρησιμοποιεί την ενσωματωμένη υπηρεσία SpeechRecognizer του Android (υπό τους όρους της συσκευής/Google) και το κείμενο αναλύεται τοπικά σε προσχέδιο εξόδου. Το Finsay ΔΕΝ ηχογραφεί, ΔΕΝ αποθηκεύει και ΔΕΝ αποστέλλει αρχεία ήχου."
            )
        ),
        LegalSectionData(
            title = t(
                "5. Biometric App Lock, Local Merchant Memory & Device Features",
                "5. Βιομετρικό Κλείδωμα, Τοπική Μνήμη Εμπόρων & Λειτουργίες Συσκευής"
            ),
            body = t(
                "• Optional Biometric & PIN App Lock: Fingerprint/Face Unlock uses Android BiometricPrompt handled exclusively by your device's secure OS hardware. Finsay NEVER accesses, collects, stores, or transmits biometric identifiers or templates (compliant with GDPR Art. 9 and US state biometric laws such as BIPA/CUBI). Any optional 4-digit PIN is stored strictly on-device as a salted SHA-256 hash.\n• Smart Merchant Category Memory: Category corrections for merchants/VAT IDs are stored locally on-device (SharedPreferences) solely to improve auto-suggestions.\n• Reminders, Widgets, Quick Tile & App Shortcuts: Local notifications (POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED, WAKE_LOCK, VIBRATE) deliver user-configured reminders and bank-draft prompts. Widgets, Quick Settings tiles, and launcher App Shortcuts operate locally.",
                "• Κλείδωμα Εφαρμογής (Δακτυλικό Αποτύπωμα / PIN): Η βιομετρική ταυτοποίηση εκτελείται αποκλειστικά από το ασφαλές υλικό/λογισμικό του Android (BiometricPrompt). Το Finsay ΔΕΝ έχει πρόσβαση, ΔΕΝ συλλέγει και ΔΕΝ αποθηκεύει ποτέ βιομετρικά δεδομένα ή αποτυπώματα (συμμόρφωση με GDPR Άρθρο 9 & νόμους ΗΠΑ όπως BIPA/CUBI). Ο προαιρετικός 4-ψήφιος κωδικός PIN αποθηκεύεται μόνο τοπικά ως salted SHA-256 hash.\n• Έξυπνη Μνήμη Κατηγορίας Εμπόρων: Οι αντιστοιχίσεις εμπόρου/ΑΦΜ με κατηγορία αποθηκεύονται αποκλειστικά τοπικά (SharedPreferences) για ταχύτερη προεπιλογή.\n• Υπενθυμίσεις, Widget, Quick Tile & App Shortcuts: Οι τοπικές ειδοποιήσεις (POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED, WAKE_LOCK, VIBRATE) εξυπηρετούν μόνο τις δικές σας υπενθυμίσεις και ειδοποιήσεις προσχεδίων."
            )
        ),
        LegalSectionData(
            title = t(
                "6. Legal Bases (GDPR / UK GDPR), Zero Ads & No Data Selling",
                "6. Νομικές Βάσεις (GDPR), Μηδενικές Διαφημίσεις & Μη Πώληση Δεδομένων"
            ),
            body = t(
                "• Performance of Contract (Art. 6(1)(b)): Delivering budgeting, multi-currency tracking, analytics, cloud sync, shared ledgers, and data exports.\n• Consent (Art. 6(1)(a)): Mandatory acceptance of these Terms & Privacy Policy (version and timestamp recorded in your private owner-only profile), plus optional bank notification parsing, camera/OCR, microphone voice input, and reminder notifications (withdrawable anytime).\n• Legitimate Interests (Art. 6(1)(f)): Ensuring security, preventing unauthorized access via Firebase Authentication and Zero-Trust Firestore Security Rules, and maintaining service reliability.\n• Zero Ads, No Data Brokers & No Third-Party AI Uploads: Finsay displays no ads, does NOT sell or rent personal data, does NOT share personal data for cross-context behavioral advertising, and does NOT transmit your financial data to third-party cloud LLM/AI services.",
                "• Εκτέλεση Σύμβασης (Άρθρο 6(1)(β)): Παροχή καταγραφής εξόδων, πολλαπλών νομισμάτων, αναλυτικών γραφημάτων, συγχρονισμού cloud, κοινών εξοδολογίων και εξαγωγών.\n• Συγκατάθεση (Άρθρο 6(1)(α)): Υποχρεωτική αποδοχή των Όρων Χρήσης και της Πολιτικής Απορρήτου (με καταγραφή έκδοσης και ημερομηνίας στο ιδιωτικό σας προφίλ), καθώς και προαιρετική ανάγνωση τραπεζικών ειδοποιήσεων, κάμερα/OCR, φωνητική καταχώρηση και υπενθυμίσεις (ανακαλείται οποτεδήποτε).\n• Έννομο Συμφέρον (Άρθρο 6(1)(στ)): Ασφάλεια συστημάτων, αποτροπή μη εξουσιοδοτημένης πρόσβασης μέσω Firebase Authentication και κανόνων Zero-Trust, και αξιοπιστία υπηρεσίας.\n• Μηδενικές Διαφημίσεις & Καμία Πώληση/Κοινοποίηση: Το Finsay δεν περιέχει διαφημίσεις, ΔΕΝ πωλεί, ΔΕΝ ενοικιάζει και ΔΕΝ διαβιβάζει δεδομένα σε διαφημιστικά δίκτυα ή data brokers, ούτε αποστέλλει τα οικονομικά σας σε εξωτερικές cloud υπηρεσίες τεχνητής νοημοσύνης (LLM)."
            )
        ),
        LegalSectionData(
            title = t(
                "7. Cloud Infrastructure, Security & International Transfers",
                "7. Υποδομή Cloud, Ασφάλεια & Διεθνείς Διαβιβάσεις Δεδομένων"
            ),
            body = t(
                "• Storage & Security: Cloud data is hosted on Google Cloud / Firebase Firestore with TLS encryption in transit, AES encryption at rest, Firebase Authentication, and Zero-Trust Security Rules restricting reads/writes to authorized ledger members.\n• International Data Transfers: Because Google Cloud / Firebase operates global infrastructure (including EU and US servers), any transfers outside the EEA or UK are safeguarded by EU Standard Contractual Clauses (SCCs), the UK International Data Transfer Addendum, and Google LLC's certification under the EU-US / UK-US Data Privacy Framework.\n• Google Play Services: Finsay Pro purchases are processed exclusively by Google Play Billing (we never see or store payment card details). Native Google Play In-App Updates and In-App Review APIs may run discreetly under Google's Privacy Policy.",
                "• Αποθήκευση & Ασφάλεια: Τα δεδομένα cloud φιλοξενούνται σε Google Cloud / Firebase Firestore με κρυπτογράφηση TLS κατά τη μεταφορά, AES κατά την αποθήκευση, ταυτοποίηση Firebase Authentication και κανόνες Zero-Trust που περιορίζουν την πρόσβαση αποκλειστικά στα εξουσιοδοτημένα μέλη.\n• Διεθνείς Διαβιβάσεις: Εφόσον η υποδομή της Google Cloud / Firebase λειτουργεί παγκοσμίως (ΕΕ & ΗΠΑ), τυχόν διαβιβάσεις εκτός ΕΟΧ/ΗΒ καλύπτονται από τις Τυποποιημένες Συμβατικές Ρήτρες της ΕΕ (SCCs), το UK Addendum και την πιστοποίηση της Google LLC στο EU-US / UK-US Data Privacy Framework.\n• Υπηρεσίες Google Play: Οι συνδρομές Finsay Pro διεκπεραιώνονται αποκλειστικά μέσω Google Play Billing (δεν βλέπουμε ούτε αποθηκεύουμε στοιχεία καρτών). Ενδέχεται να χρησιμοποιούνται διακριτικά τα επίσημα APIs Google Play In-App Review & In-App Updates."
            ) + "\n" + t(
                "• Crash & Performance Diagnostics: Finsay uses Firebase Crashlytics and Firebase Performance Monitoring solely to detect crashes and improve app responsiveness. Only anonymous technical diagnostics (device model, OS version, stack traces, and app startup/sync timings) are collected—never financial entries, amounts, emails, or personal identifiers.",
                "• Διαγνωστικά Σφαλμάτων & Απόδοσης: Το Finsay χρησιμοποιεί τα Firebase Crashlytics και Firebase Performance Monitoring αποκλειστικά για τον εντοπισμό σφαλμάτων (crashes) και τη βελτίωση της ταχύτητας της εφαρμογής. Συλλέγονται μόνο ανώνυμα τεχνικά διαγνωστικά (μοντέλο συσκευής, έκδοση Android, stack traces και χρόνοι φόρτωσης) — ποτέ οικονομικές εγγραφές, ποσά, email ή προσωπικά στοιχεία."
            )
        ),
        LegalSectionData(
            title = t(
                "8. Data Retention, Exports & Permanent Account Deletion",
                "8. Χρόνος Διατήρησης, Εξαγωγές & Οριστική Διαγραφή Λογαριασμού"
            ),
            body = t(
                "• Retention: We retain your account and ledger data only for as long as your account remains active or until you delete specific entries or your entire account.\n• Data Portability: You may export your expenses, incomes, balances, and member settle-up summaries to Excel (.xls), PDF (.pdf), or CSV (.csv) locally on-device anytime.\n• Immediate Account Deletion: You may permanently delete your account, private profile, owned ledgers, and cloud/local data directly in the app (Menu (☰) > My Profile > Delete Account) or via https://finsayapp.com/delete-account.html. Residual cloud backups are purged within standard infrastructure cycles (up to 30 days).",
                "• Διατήρηση: Τα δεδομένα διατηρούνται μόνο όσο ο λογαριασμός σας είναι ενεργός ή μέχρι να διαγράψετε εγγραφές ή τον λογαριασμό σας.\n• Φορητότητα Δεδομένων: Μπορείτε οποτεδήποτε να εξάγετε έξοδα, έσοδα, ισολογισμό και εκκαθάριση μελών τοπικά σε αρχείο Excel (.xls), PDF (.pdf) ή CSV (.csv).\n• Άμεση Οριστική Διαγραφή: Διαγράψτε οριστικά τον λογαριασμό, το ιδιωτικό προφίλ και όλα τα δεδομένα σας από το «Μενού (☰) > Το Προφίλ μου > Διαγραφή Λογαριασμού» ή μέσω https://finsayapp.com/delete-account.html. Τυχόν τεχνικά αντίγραφα ασφαλείας υποδομής εκκαθαρίζονται εντός 30 ημερών."
            )
        ),
        LegalSectionData(
            title = t(
                "9. Your Rights (Greece, EU/EEA, UK & United States)",
                "9. Τα Δικαιώματά σας (Ελλάδα, ΕΕ/ΕΟΧ, ΗΒ & Ηνωμένες Πολιτείες)"
            ),
            body = t(
                "• Greece, EU/EEA & UK (GDPR / Law 4624/2019 / UK GDPR): You have the right to access, rectify, erase, restrict processing, data portability, object, withdraw consent at any time, and lodge a complaint with a supervisory authority—including the Hellenic Data Protection Authority (HDPA, www.dpa.gr) or the UK ICO.\n• United States (California CCPA/CPRA & State Privacy Laws — VA, CO, CT, UT, TX, etc.): US residents have the right to know/access personal information collected, request deletion, request correction of inaccurate data, obtain a portable copy, and receive non-discriminatory treatment when exercising privacy rights. Finsay DOES NOT \"sell\" personal information and DOES NOT \"share\" personal information for cross-context behavioral advertising. Sensitive personal information is used strictly as necessary to provide requested app features.\n• Exercising Rights: Use in-app tools or email support@finsayapp.com; we verify and respond within 30 days (or the statutory period under applicable law).",
                "• Ελλάδα, ΕΕ/ΕΟΧ & ΗΒ (GDPR / Ν. 4624/2019 / UK GDPR): Έχετε δικαίωμα πρόσβασης, διόρθωσης, διαγραφής, περιορισμού επεξεργασίας, φορητότητας, εναντίωσης, ανάκλησης συγκατάθεσης ανά πάσα στιγμή και υποβολής καταγγελίας σε εποπτική αρχή — συμπεριλαμβανομένης της Αρχής Προστασίας Δεδομένων Προσωπικού Χαρακτήρα (ΑΠΔΠΧ, www.dpa.gr) ή της αρμόδιας αρχής της χώρας σας.\n• Ηνωμένες Πολιτείες (CCPA/CPRA Καλιφόρνιας & Πολιτειακοί Νόμοι ΗΠΑ — VA, CO, CT, UT, TX κ.ά.): Οι κάτοικοι ΗΠΑ έχουν δικαίωμα γνώσης/πρόσβασης, διαγραφής, διόρθωσης ανακριβών στοιχείων, φορητότητας και μη διάκρισης. Το Finsay ΔΕΝ «πωλεί» («sell») και ΔΕΝ «κοινοποιεί» («share») προσωπικές πληροφορίες για στοχευμένη διαφήμιση. Τυχόν ευαίσθητες πληροφορίες χρησιμοποιούνται αποκλειστικά για τις ζητούμενες λειτουργίες της εφαρμογής.\n• Άσκηση Δικαιωμάτων: Μέσω της εφαρμογής ή με email στο support@finsayapp.com (απάντηση εντός 30 ημερών ή της νόμιμης προθεσμίας)."
            )
        ),
        LegalSectionData(
            title = t(
                "10. Children's Privacy (COPPA & GDPR Art. 8) & Changes",
                "10. Προστασία Ανηλίκων (COPPA & GDPR Άρθρο 8) & Τροποποιήσεις"
            ),
            body = t(
                "• Children's Privacy: Finsay is not intended for children under 13 years of age (US COPPA) or under 16 years of age (or the applicable digital consent age in your EU/EEA/UK country under GDPR Art. 8). We do not knowingly collect data from children; contact support@finsayapp.com for immediate deletion if you believe a child provided data.\n• Policy Updates: We may update this Privacy Policy to reflect new features or legal requirements. Material changes will be notified in-app or on finsayapp.com.",
                "• Ανήλικοι: Το Finsay δεν απευθύνεται σε παιδιά κάτω των 13 ετών (ΗΠΑ - COPPA) ή κάτω των 16 ετών (ή του εκάστοτε ορίου ψηφιακής συναίνεσης στην ΕΕ/ΕΟΧ/ΗΒ βάσει GDPR Άρθρου 8). Δεν συλλέγουμε εν γνώσει μας δεδομένα ανηλίκων· εάν διαπιστωθεί τέτοια περίπτωση, επικοινωνήστε στο support@finsayapp.com για άμεση διαγραφή.\n• Τροποποιήσεις: Ενδέχεται να ενημερώνουμε την παρούσα Πολιτική για νέες λειτουργίες ή νομικές απαιτήσεις με σχετική ειδοποίηση εντός εφαρμογής ή στο finsayapp.com."
            )
        )
    )
}

private fun termsOfServiceSections(lang: Localization.Language): List<LegalSectionData> {
    fun t(en: String, el: String) = Localization.tr(lang, en, el)
    return listOf(
        LegalSectionData(
            title = t(
                "1. Acceptance of Terms & Eligibility (Updated: November 2026)",
                "1. Αποδοχή Όρων & Προϋποθέσεις Ηλικίας (Ενημέρωση: Νοέμβριος 2026)"
            ),
            body = t(
                "By downloading, installing, registering for, or using Finsay (finsayapp.com), you agree to be bound by these Terms of Service and our Privacy Policy. You must be at least 16 years old in the EU/EEA/UK (or the applicable legal digital consent age in your jurisdiction) and at least 13 years old in the United States (or have verifiable parental consent where required) to use Finsay. If you do not agree, do not use the application.",
                "Με τη λήψη, εγκατάσταση, εγγραφή ή χρήση της εφαρμογής Finsay (finsayapp.com), αποδέχεστε ρητά τους παρόντες Όρους Χρήσης και την Πολιτική Απορρήτου. Για χρήση της εφαρμογής απαιτείται ηλικία τουλάχιστον 16 ετών στην ΕΕ/ΕΟΧ/ΗΒ (ή το νόμιμο όριο ψηφιακής συναίνεσης της χώρας σας) και τουλάχιστον 13 ετών στις ΗΠΑ (ή επαληθεύσιμη γονική συναίνεση όπου απαιτείται). Εάν διαφωνείτε, παρακαλούμε μην χρησιμοποιήσετε την εφαρμογή."
            )
        ),
        LegalSectionData(
            title = t(
                "2. Service Description & Strict Financial / Tax Advice Disclaimer",
                "2. Περιγραφή Υπηρεσίας & Αποποίηση Οικονομικής / Φορολογικής Ευθύνης"
            ),
            body = t(
                "Finsay is an informational personal and household expense, income, budget, multi-currency, and split-tracking tool. Finsay DOES NOT provide financial, investment, banking, accounting, legal, or tax advice, nor does it execute payments, currency conversions, or money transfers. Displayed currencies are purely organizational labels; Finsay does not perform regulated foreign-exchange brokerage. You remain solely responsible for verifying all figures, tax obligations, and financial decisions.",
                "Το Finsay αποτελεί ενημερωτικό εργαλείο καταγραφής, οργάνωσης και παρακολούθησης προσωπικού και οικογενειακού προϋπολογισμού, πολλαπλών νομισμάτων και επιμερισμού εξόδων. Το Finsay ΔΕΝ παρέχει χρηματοοικονομικές, επενδυτικές, τραπεζικές, λογιστικές, νομικές ή φορολογικές συμβουλές, ούτε εκτελεί πληρωμές, μεταφορές χρημάτων ή συναλλαγματικές πράξεις. Η επιλογή νομίσματος έχει καθαρά οργανωτικό/ενημερωτικό χαρακτήρα. Ο χρήστης φέρει την αποκλειστική ευθύνη για τον έλεγχο των εγγραφών και των οικονομικών/φορολογικών του αποφάσεων."
            )
        ),
        LegalSectionData(
            title = t(
                "3. Bank Notification Reader Disclaimer (No Open Banking / PSD2)",
                "3. Αποποίηση Ευθύνης για Ανάγνωση Τραπεζικών Ειδοποιήσεων (Όχι Open Banking / PSD2)"
            ),
            body = t(
                "• Independent Local Utility: The optional Bank Notification Auto-Catcher reads Android payment notifications solely on-device after your explicit system authorization. Finsay is NOT affiliated with, endorsed by, or connected to any bank, card issuer, or payment institution, and does NOT operate as a PSD2 / Open Banking Account Information Service Provider (AISP) or Payment Initiation Service Provider (PISP).\n• Best-Effort Accuracy: Because banks frequently change notification wording and Android battery/OEM policies may delay or restrict background delivery, Finsay cannot guarantee that every card payment will be detected or parsed with 100% accuracy. You must review and confirm every draft before saving and rely on official bank statements for authoritative records.",
                "• Ανεξάρτητη Τοπική Λειτουργία: Η προαιρετική αυτόματη ανίχνευση ειδοποιήσεων πληρωμών διαβάζει ειδοποιήσεις Android αποκλειστικά τοπικά στη συσκευή μετά από ρητή άδειά σας. Το Finsay ΔΕΝ συνδέεται, ΔΕΝ συνεργάζεται και ΔΕΝ εγκρίνεται από καμία τράπεζα ή ίδρυμα πληρωμών και ΔΕΝ λειτουργεί ως πάροχος Open Banking / PSD2 (AISP ή PISP).\n• Ακρίβεια Βέλτιστης Προσπάθειας: Επειδή οι τράπεζες τροποποιούν συχνά τη μορφή των ειδοποιήσεων και οι ρυθμίσεις μπαταρίας του Android ενδέχεται να καθυστερήσουν την παράδοση, το Finsay δεν εγγυάται την ανίχνευση κάθε συναλλαγής ή 100% ακρίβεια ανάγνωσης. Οφείλετε να ελέγχετε κάθε προσχέδιο πριν την αποθήκευση και να βασίζεστε στα επίσημα τραπεζικά σας αντίγραφα."
            )
        ),
        LegalSectionData(
            title = t(
                "4. Receipt OCR, AADE / e-Invoicing QR Lookup & Voice Entry",
                "4. Σάρωση Αποδείξεων (OCR), QR ΑΑΔΕ & Φωνητική Καταχώρηση"
            ),
            body = t(
                "Optical character recognition (OCR), QR receipt lookups (including direct read-only queries to allowlisted Greek tax/e-invoicing portals such as AADE), and voice-to-text parsing are automated convenience features that may occasionally misread text, speech, dates, or totals. Finsay is not affiliated with AADE or any e-invoicing provider. You are solely responsible for checking and correcting auto-filled values against original receipts before saving.",
                "Η οπτική αναγνώριση χαρακτήρων (OCR), η ανάγνωση φορολογικών QR (συμπεριλαμβανομένων κλήσεων ανάγνωσης σε εγκεκριμένους ελληνικούς παρόχους/ΑΑΔΕ) και η φωνητική μετατροπή ομιλίας σε κείμενο αποτελούν αυτοματοποιημένες διευκολύνσεις που ενδέχεται περιστασιακά να περιέχουν σφάλματα αναγνώρισης. Το Finsay δεν συνδέεται με την ΑΑΔΕ ή παρόχους τιμολόγησης. Έχετε την αποκλειστική ευθύνη ελέγχου των προ-συμπληρωμένων στοιχείων βάσει της πρωτότυπης απόδειξης."
            )
        ),
        LegalSectionData(
            title = t(
                "5. Shared Households, Member Roles & Third-Party Consent",
                "5. Κοινά Εξοδολόγια, Ρόλοι Μελών & Συναίνεση Τρίτων"
            ),
            body = t(
                "When creating or participating in a shared ledger (Creator, Editor, Registar), you acknowledge that financial entries, split balances, and ledger activity logs are visible to all authorized members of that ledger according to their roles. You represent and warrant that you have obtained appropriate consent before entering another individual's personal information or inviting them to a shared ledger, and you are responsible for managing member access in ledgers you control.",
                "Κατά τη δημιουργία ή συμμετοχή σε κοινό εξοδολόγιο (Δημιουργός, Συντάκτης, Καταχωρητής), αποδέχεστε ότι οι εγγραφές, τα υπόλοιπα εκκαθάρισης και το ιστορικό δραστηριότητας είναι ορατά στα εξουσιοδοτημένα μέλη βάσει του ρόλου τους. Δηλώνετε και εγγυάστε ότι διαθέτετε την απαραίτητη συναίνεση πριν καταχωρήσετε στοιχεία τρίτων προσώπων ή προσκαλέσετε μέλη και είστε υπεύθυνος για τη διαχείριση των δικαιωμάτων πρόσβασης στα εξοδολόγια που διαχειρίζεστε."
            )
        ),
        LegalSectionData(
            title = t(
                "6. Account Security, Optional App Lock & Data Exports",
                "6. Ασφάλεια Λογαριασμού, Κλείδωμα Εφαρμογής & Εξαγωγές Αρχείων"
            ),
            body = t(
                "• Credentials & Local App Lock: You are responsible for safeguarding your sign-in credentials and device. Optional Biometric/PIN App Lock is a local convenience safeguard; if you forget your PIN or lose device biometric access, recovery may require re-authentication or reinstalling the app.\n• Exported Files: When you export reports (Excel, PDF, CSV) and share them via third-party apps (email, messaging, cloud drives), those exported copies leave Finsay's control and their confidentiality becomes your sole responsibility.",
                "• Διαπιστευτήρια & Τοπικό Κλείδωμα: Είστε υπεύθυνος για την ασφάλεια των στοιχείων σύνδεσης και της συσκευής σας. Το προαιρετικό Κλείδωμα με Δακτυλικό Αποτύπωμα/PIN αποτελεί τοπικό μέτρο διευκόλυνσης· σε περίπτωση απώλειας PIN, η ανάκτηση ενδέχεται να απαιτήσει επαναταυτοποίηση ή επανεγκατάσταση.\n• Εξαγόμενα Αρχεία: Όταν εξάγετε αναφορές (Excel, PDF, CSV) και τις κοινοποιείτε εκτός Finsay (email, μηνύματα, cloud), η ασφάλεια και εμπιστευτικότητα των αρχείων αυτών αποτελεί αποκλειστική σας ευθύνη."
            )
        ),
        LegalSectionData(
            title = t(
                "7. Finsay Pro Subscriptions, Renewals & Refunds (Google Play)",
                "7. Συνδρομές Finsay Pro, Ανανεώσεις & Επιστροφές Χρημάτων (Google Play)"
            ),
            body = t(
                "• Billing & Auto-Renewal: Certain advanced features (such as Bank Notification Auto-Catcher, multiple ledgers, and unlimited members) require a Finsay Pro subscription or lifetime purchase processed exclusively via Google Play Billing. Recurring subscriptions automatically renew unless cancelled in Google Play settings before the end of the current billing period.\n• Refunds & Consumer Rights: Pricing, cancellations, and refund requests are governed by Google Play's billing policies and applicable mandatory consumer laws in your jurisdiction (including statutory EU/UK withdrawal rights where applicable and US consumer statutes).",
                "• Χρεώσεις & Αυτόματη Ανανέωση: Προηγμένες λειτουργίες (όπως αυτόματη ανάγνωση τραπεζικών ειδοποιήσεων, πολλαπλά εξοδολόγια, απεριόριστα μέλη) διατίθενται μέσω Finsay Pro αποκλειστικά μέσω Google Play Billing. Οι περιοδικές συνδρομές ανανεώνονται αυτόματα εκτός αν ακυρωθούν στις ρυθμίσεις Google Play πριν τη λήξη της τρέχουσας περιόδου.\n• Ακυρώσεις & Δικαιώματα Καταναλωτή: Οι τιμές, ακυρώσεις και επιστροφές χρημάτων διέπονται από τους όρους Google Play και την υποχρεωτική νομοθεσία προστασίας καταναλωτή της χώρας σας (ΕΕ/ΗΒ/ΗΠΑ)."
            )
        ),
        LegalSectionData(
            title = t(
                "8. Acceptable Use & Intellectual Property",
                "8. Αποδεκτή Χρήση & Πνευματική Ιδιοκτησία"
            ),
            body = t(
                "You agree not to misuse Finsay, bypass authentication or security rules, attempt unauthorized access to other users' data, reverse-engineer or copy proprietary components except as permitted by mandatory law, or use the app for fraudulent or unlawful purposes. All Finsay trademarks, branding, design, and software remain the intellectual property of Finsay and its creators.",
                "Απαγορεύεται η κατάχρηση της εφαρμογής, η παράκαμψη ταυτοποίησης ή κανόνων ασφαλείας, η μη εξουσιοδοτημένη πρόσβαση σε δεδομένα άλλων χρηστών, η αντίστροφη μηχανική (πέραν όσων επιτρέπει ρητά ο νόμος) και κάθε παράνομη ή παραπλανητική χρήση. Όλα τα σήματα, ο σχεδιασμός και το λογισμικό αποτελούν πνευματική ιδιοκτησία του Finsay και των δημιουργών του."
            )
        ),
        LegalSectionData(
            title = t(
                "9. Disclaimer of Warranties & Limitation of Liability",
                "9. Αποποίηση Εγγυήσεων & Περιορισμός Ευθύνης"
            ),
            body = t(
                "To the fullest extent permitted by applicable law, Finsay is provided \"AS IS\" and \"AS AVAILABLE\" without warranties of uninterrupted operation or error-free parsing. To the maximum extent permitted by law, Finsay and its developers shall not be liable for indirect, incidental, special, punitive, or consequential damages, missed bill reminders, unparsed notifications, or loss of profits/data; aggregate liability shall not exceed the total amount paid by you for Finsay Pro during the twelve (12) months preceding the claim.\n• Statutory Carve-Out (EU/UK/US): Nothing in these Terms excludes or limits liability that cannot legally be excluded under mandatory law (including gross negligence, willful misconduct, fraud, or mandatory statutory consumer rights in the EU/EEA, UK, or US states).",
                "Στο μέγιστο βαθμό που επιτρέπει ο νόμος, το Finsay παρέχεται «ΩΣ ΕΧΕΙ» και «ΟΠΩΣ ΔΙΑΤΙΘΕΤΑΙ» χωρίς εγγύηση αδιάλειπτης λειτουργίας ή αλάνθαστης αυτοματοποιημένης ανάλυσης. Το Finsay και οι δημιουργοί του δεν ευθύνονται για έμμεσες, παρεπόμενες ή αποθετικές ζημίες, χαμένες υπενθυμίσεις, μη ανιχνευθείσες ειδοποιήσεις ή απώλεια δεδομένων/κερδών· τυχόν συνολική ευθύνη περιορίζεται στο ποσό που καταβάλατε για το Finsay Pro κατά τους τελευταίους δώδεκα (12) μήνες.\n• Επιφύλαξη Αναγκαστικού Δικαίου (ΕΕ/ΗΒ/ΗΠΑ): Τίποτα στους παρόντες Όρους δεν περιορίζει ευθύνη που δεν επιτρέπεται να αποκλειστεί εκ του νόμου (όπως δόλος, βαριά αμέλεια, απάτη ή αναγκαστικά δικαιώματα καταναλωτών σε ΕΕ/ΕΟΧ, ΗΒ ή ΗΠΑ)."
            )
        ),
        LegalSectionData(
            title = t(
                "10. Termination, Changes, Governing Law & International Consumer Rights",
                "10. Τερματισμός, Εφαρμοστέο Δίκαιο & Διεθνής Προστασία Καταναλωτή"
            ),
            body = t(
                "• Termination: You may terminate your account anytime via Menu (☰) > My Profile > Delete Account. We may suspend accounts that materially violate these Terms or threaten platform security.\n• Governing Law & Consumer Carve-Out: These Terms are governed by the laws of the Hellenic Republic (Greece) and applicable European Union law, with disputes submitted to the courts of Athens, Greece. However, if you are a consumer residing in the EU/EEA, the UK, or the United States, nothing in these Terms deprives you of the mandatory consumer protection laws or statutory forum rights of your country or state of residence.\n• Contact: support@finsayapp.com.",
                "• Τερματισμός: Μπορείτε να διαγράψετε τον λογαριασμό σας οποτεδήποτε από το «Μενού (☰) > Το Προφίλ μου > Διαγραφή Λογαριασμού». Διατηρούμε το δικαίωμα αναστολής λογαριασμών που παραβιάζουν ουσιωδώς τους Όρους ή απειλούν την ασφάλεια.\n• Εφαρμοστέο Δίκαιο & Διεθνής Επιφύλαξη: Οι παρόντες Όροι διέπονται από το Ελληνικό Δίκαιο και το δίκαιο της Ευρωπαϊκής Ένωσης, με αρμοδιότητα των δικαστηρίων των Αθηνών. Ωστόσο, εάν είστε καταναλωτής με κατοικία σε χώρα της ΕΕ/ΕΟΧ, στο Ηνωμένο Βασίλειο ή στις ΗΠΑ, τίποτα στους παρόντες Όρους δεν σας στερεί την προστασία των αναγκαστικών διατάξεων προστασίας καταναλωτή ή τη νόμιμη δικαιοδοσία του τόπου κατοικίας σας.\n• Επικοινωνία: support@finsayapp.com."
            )
        )
    )
}

@Composable
private fun LegalSection(title: String, body: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = body,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
