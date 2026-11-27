package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.util.Localization

internal data class QuickStartStep(
    val number: Int,
    val title: String,
    val whereLabel: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val actionType: String? = null // "ADD_MEMBER", "SEED_CATEGORIES", "ADD_CATEGORY"
)

internal data class TabGuideSection(
    val id: String,
    val title: String,
    val subtitle: String,
    val locationBadge: String,
    val icon: ImageVector,
    val accentColor: Color,
    val capabilities: List<String>
)

internal data class SmartFeatureItem(
    val id: String,
    val title: String,
    val summary: String,
    val highlights: List<String>,
    val tipText: String?,
    val icon: ImageVector,
    val accentColor: Color
)

internal object GuideContentData {

    fun buildQuickStartSteps(lang: Localization.Language): List<QuickStartStep> {
        fun t(en: String, el: String) = Localization.tr(lang, en, el)
        return listOf(
            QuickStartStep(
                number = 1,
                title = t("Add Household Members", "Προσθέστε τα Μέλη σας"),
                whereLabel = t("Members Tab", "Καρτέλα «Μέλη»"),
                description = t(
                    "Add family members or roommates with their role (Parent, Child, Roommate), color, and voice keywords. Link each member to their user account so they are automatically pre-selected as the default payer.",
                    "Καταχωρήστε τα άτομα της οικογένειας ή της παρέας (Γονέας, Παιδί, Συγκάτοικος κ.ά.) με χρώμα και λέξεις φωνής. Συνδέστε κάθε μέλος με τον λογαριασμό του ώστε να προεπιλέγεται αυτόματα ως πληρωτής."
                ),
                icon = Icons.Default.Group,
                accentColor = Color(0xFF0284C7),
                actionType = "ADD_MEMBER"
            ),
            QuickStartStep(
                number = 2,
                title = t("Set Up Categories & Budgets", "Ρυθμίστε Κατηγορίες & Μηνιαία Όρια"),
                whereLabel = t("Categories Tab", "Καρτέλα «Κατηγορίες»"),
                description = t(
                    "Add the standard categories in 1 tap or create your own with custom icons, colors, voice keywords, and monthly budget limits to get automatic overspending alerts.",
                    "Προσθέστε τις βασικές κατηγορίες με 1 πάτημα ή φτιάξτε δικές σας με εικονίδια, χρώματα, λέξεις-κλειδιά φωνής και μηνιαίο προϋπολογισμό (budget) για έγκαιρες ειδοποιήσεις."
                ),
                icon = Icons.Default.Category,
                accentColor = Color(0xFF059669),
                actionType = "SEED_CATEGORIES"
            ),
            QuickStartStep(
                number = 3,
                title = t("Name or Share Your Expense Book", "Οργανώστε ή Μοιραστείτε το Εξοδολόγιο"),
                whereLabel = t("Top Bar → Manage Books", "Επάνω Μπάρα / Διαχείριση Εξοδολογίων"),
                description = t(
                    "Rename your active expense book, create separate books (e.g. Home, Personal, Vacation), or share with verified users by assigning roles (Creator, Editor, Registrar).",
                    "Μετονομάστε το εξοδολόγιό σας, δημιουργήστε ξεχωριστά βιβλία (π.χ. Σπίτι, Προσωπικό, Ταξίδι) ή κοινοποιήστε το σε άλλους χρήστες με ρόλους (Δημιουργός, Συντάκτης, Καταχωρητής)."
                ),
                icon = Icons.Default.Book,
                accentColor = Color(0xFF0D9488)
            ),
            QuickStartStep(
                number = 4,
                title = t("Record Your First Expense or Income", "Καταχωρήστε το πρώτο Έξοδο ή Έσοδο"),
                whereLabel = t("Home Tab & Floating Mic", "Καρτέλα «Αρχική» & Μικρόφωνο"),
                description = t(
                    "Tap + Expense or + Income, press the floating microphone button to speak naturally, or scan a paper receipt using your camera or AADE QR code.",
                    "Πατήστε + Έξοδο ή + Έσοδο, χρησιμοποιήστε το πλωτό μικρόφωνο για φωνητική καταγραφή στα Ελληνικά, ή σαρώστε απόδειξη με την κάμερα ή QR code ΑΑΔΕ."
                ),
                icon = Icons.Default.Mic,
                accentColor = Color(0xFF7C3AED)
            ),
            QuickStartStep(
                number = 5,
                title = t("Turn On Reminders, Goals & Bank Auto-Catch", "Ενεργοποιήστε Υπενθυμίσεις, Στόχους & Τράπεζες"),
                whereLabel = t("Side Menu & Settings", "Πλευρικό Μενού & Ρυθμίσεις"),
                description = t(
                    "Schedule recurring bill reminders (rent, utilities, subscriptions), create savings goals that link directly to your expenses, and optionally enable automatic bank/Google Pay notification capture in Settings.",
                    "Ορίστε υπενθυμίσεις πάγιων λογαριασμών, φτιάξτε στόχους αποταμίευσης που συνδέονται άμεσα με τα έξοδα και ενεργοποιήστε (προαιρετικά) τον αυτόματο εντοπισμό τραπεζικών ειδοποιήσεων στις Ρυθμίσεις."
                ),
                icon = Icons.Default.NotificationsActive,
                accentColor = Color(0xFFD97706)
            )
        )
    }

    fun buildTabGuideSections(lang: Localization.Language): List<TabGuideSection> {
        fun t(en: String, el: String) = Localization.tr(lang, en, el)
        return listOf(
            TabGuideSection(
                id = "home",
                title = t("Home (Expenses & Income)", "Αρχική (Έξοδα & Έσοδα)"),
                subtitle = t("Daily ledger, fast entry & filters", "Καθημερινές κινήσεις, γρήγορη καταχώρηση & φίλτρα"),
                locationBadge = t("Bottom Tab 1", "1η Καρτέλα"),
                icon = Icons.Default.Receipt,
                accentColor = Color(0xFF059669),
                capabilities = listOf(
                    t(
                        "View your monthly summary card (Total Expenses, Income & Net Balance) and switch between months.",
                        "Δείτε τη σύνοψη του μήνα (Συνολικά Έξοδα, Έσοδα & Υπόλοιπο) και μετακινηθείτε εύκολα μεταξύ μηνών."
                    ),
                    t(
                        "Add expenses manually with category, date, single or multi-payer split, beneficiary members, and notes.",
                        "Καταχωρήστε έξοδα χειροκίνητα με κατηγορία, ημερομηνία, πληρωτή (ή πολλαπλούς πληρωτές), επιμερισμό σε μέλη και σημείωση."
                    ),
                    t(
                        "Record household incomes (salary, freelance, benefits, etc.) per member.",
                        "Προσθέστε έσοδα (μισθός, επίδομα, ελεύθερο επάγγελμα κ.ά.) ανά μέλος."
                    ),
                    t(
                        "Log expenses by voice using the floating microphone button, or scan receipts via Camera OCR & AADE QR.",
                        "Καταγράψτε έξοδα με τη φωνή σας στα Ελληνικά (πλωτό μικρόφωνο) ή σκανάρετε αποδείξεις με Κάμερα OCR & QR ΑΑΔΕ."
                    ),
                    t(
                        "Confirm or dismiss detected bank/Google Pay payment notifications and due bill reminders right from the top banners.",
                        "Επιβεβαιώστε ή απορρίψτε αυτόματα εντοπισμένες τραπεζικές πληρωμές και ειδοποιήσεις πάγιων λογαριασμών από το επάνω μέρος."
                    ),
                    t(
                        "Filter transactions by month, type (Expenses / Income / All), member, category, or free-text search.",
                        "Φιλτράρετε τις κινήσεις ανά μήνα, τύπο (Έξοδα / Έσοδα / Όλα), μέλος, κατηγορία ή ελεύθερη αναζήτηση."
                    ),
                    t(
                        "Tap any entry to edit it, swipe left on an expense card to delete it immediately (with a 5-second Undo banner), or swipe right to repeat the expense today.",
                        "Πατήστε σε οποιαδήποτε εγγραφή για επεξεργασία, σύρετε αριστερά σε μια κάρτα εξόδου για άμεση διαγραφή (με 5 δευτ. Αναίρεση), ή σύρετε δεξιά για άμεση «Επανάληψη σήμερα»."
                    ),
                    t(
                        "Long-press the Finsay app icon on your home screen for instant App Shortcuts: Voice Entry, Scan Receipt, or New Expense.",
                        "Πατήστε παρατεταμένα το εικονίδιο του Finsay στην αρχική οθόνη για άμεσες συντομεύσεις: Φωνητική Καταχώρηση, Σάρωση Απόδειξης ή Νέο Έξοδο."
                    )
                )
            ),
            TabGuideSection(
                id = "analytics",
                title = t("Statistics", "Στατιστικά"),
                subtitle = t("Charts, forecasts, budgets & settle-up", "Γραφήματα, προβλέψεις, προϋπολογισμός & εκκαθάριση"),
                locationBadge = t("Bottom Tab 2", "2η Καρτέλα"),
                icon = Icons.Default.PieChart,
                accentColor = Color(0xFF0D9488),
                capabilities = listOf(
                    t(
                        "Switch analysis scope between Month, Year, or All recorded data.",
                        "Επιλέξτε ανάλυση ανά Μήνα, Έτος ή για Όλα τα καταγεγραμμένα δεδομένα."
                    ),
                    t(
                        "Track Budget Alerts when categories approach or exceed their monthly limits.",
                        "Λάβετε ειδοποιήσεις προϋπολογισμού (Budget Alerts) όταν μια κατηγορία πλησιάζει ή ξεπερνά το όριο."
                    ),
                    t(
                        "Inspect KPI totals (Income, Expenses, Balance, Savings Rate, Daily Average & Peak Spending Day) and monthly forecast vs. previous month.",
                        "Δείτε συγκεντρωτικούς δείκτες (Έσοδα, Έξοδα, Καθαρό Υπόλοιπο, Δείκτη Αποταμίευσης, Μέσο Όρο/Ημέρα & Ημέρα αιχμής) και μηνιαία πρόβλεψη σε σύγκριση με τον προηγούμενο μήνα."
                    ),
                    t(
                        "Explore breakdowns by Income, Family vs. Children, Members/Payers, Categories, and Savings Goals — tap any item to drill down straight into matching Home transactions.",
                        "Εξερευνήστε γραφήματα για Έσοδα, Γονείς/Παιδιά, Μέλη/Πληρωτές, Κατηγορίες και Στόχους Αποταμίευσης — πατήστε σε κάθε στοιχείο για άμεσο φιλτράρισμα στην Αρχική."
                    ),
                    t(
                        "Use automatic Settle-Up to see exactly who owes whom and record a settlement transfer in 1 tap.",
                        "Χρησιμοποιήστε την Εκκαθάριση Οφειλών (Settle-Up) για να δείτε ποιος χρωστάει σε ποιον και να καταχωρήσετε εξόφληση με 1 πάτημα."
                    ),
                    t(
                        "Export and share structured reports to Excel (CSV) and PDF.",
                        "Εξάγετε και μοιραστείτε αναλυτικές αναφορές σε Excel (CSV) και PDF."
                    )
                )
            ),
            TabGuideSection(
                id = "members",
                title = t("Members", "Μέλη"),
                subtitle = t("People, roles & automatic default payer", "Άτομα εξοδολογίου, ρόλοι & προεπιλεγμένος πληρωτής"),
                locationBadge = t("Bottom Tab 3", "3η Καρτέλα"),
                icon = Icons.Default.Group,
                accentColor = Color(0xFF0284C7),
                capabilities = listOf(
                    t(
                        "Add, edit, color-code, and reorder members (Parent, Child, Roommate, etc.).",
                        "Προσθέστε, επεξεργαστείτε, χρωματίστε και αλλάξτε τη σειρά των μελών (Γονέας, Παιδί, Συγκάτοικος κ.ά.)."
                    ),
                    t(
                        "Set custom voice keywords per member for accurate Greek voice recognition.",
                        "Ορίστε λέξεις-κλειδιά φωνής για κάθε μέλος ώστε να αναγνωρίζεται άμεσα στη φωνητική καταγραφή."
                    ),
                    t(
                        "Link each member to a connected user account so their expenses automatically pre-select them as Payer.",
                        "Συνδέστε ένα μέλος με λογαριασμό χρήστη ώστε όταν αυτός καταχωρεί έξοδα να επιλέγεται αυτόματα ως Πληρωτής."
                    ),
                    t(
                        "Move removed members to the Recycle Bin and restore or permanently delete them anytime.",
                        "Ανακτήστε ή διαγράψτε οριστικά παλαιότερα μέλη από τον Κάδο Ανακύκλωσης."
                    )
                )
            ),
            TabGuideSection(
                id = "categories",
                title = t("Categories (Expenses & Income)", "Κατηγορίες (Εξόδων & Εσόδων)"),
                subtitle = t("Custom icons, colors, income categories & budgets", "Εικονίδια, χρώματα, κατηγορίες εσόδων & budget"),
                locationBadge = t("Bottom Tab 4", "4η Καρτέλα"),
                icon = Icons.Default.Category,
                accentColor = Color(0xFF7C3AED),
                capabilities = listOf(
                    t(
                        "Manage both Expense Categories and Income Categories using the switcher at the top.",
                        "Διαχειριστείτε τόσο Κατηγορίες Εξόδων όσο και Κατηγορίες Εσόδων από τον επιλογέα στο επάνω μέρος."
                    ),
                    t(
                        "Free edition allows up to 5 active Expense Categories — upgrade to Finsay Pro for unlimited expense categories.",
                        "Στη δωρεάν έκδοση επιτρέπονται έως 5 ενεργές Κατηγορίες Εξόδων — με το Finsay Pro έχετε απεριόριστες κατηγορίες εξόδων."
                    ),
                    t(
                        "Customize icons, badge colors, ledger font colors, display order, and voice keywords for both expenses and incomes.",
                        "Προσαρμόστε εικονίδιο, χρώμα κάρτας, χρώμα γραμματοσειράς, σειρά εμφάνισης και λέξεις-κλειδιά φωνής."
                    ),
                    t(
                        "Set monthly budget caps on expense categories for live progress bars and overspending alerts (income categories do not require a budget).",
                        "Ορίστε μηνιαίο όριο (budget) στις κατηγορίες εξόδων για μπάρες προόδου και ειδοποιήσεις υπέρβασης (στις κατηγορίες εσόδων δεν απαιτείται budget)."
                    ),
                    t(
                        "Restore deleted categories anytime from the Category Recycle Bin.",
                        "Επαναφέρετε διαγραμμένες κατηγορίες ανά πάσα στιγμή από τον Κάδο Ανακύκλωσης."
                    )
                )
            ),
            TabGuideSection(
                id = "households",
                title = t("Expense Books & Top Bar", "Εξοδολόγια & Επάνω Μπάρα"),
                subtitle = t("Multiple books, cloud sync, sharing & history", "Πολλαπλά βιβλία, cloud συγχρονισμός & κοινοποίηση"),
                locationBadge = t("Top Bar & Menu", "Επάνω Μπάρα & Μενού"),
                icon = Icons.Default.Book,
                accentColor = Color(0xFF047857),
                capabilities = listOf(
                    t(
                        "Quickly switch your active expense book from the top bar or check real-time Cloud Sync status with manual refresh.",
                        "Αλλάξτε ενεργό εξοδολόγιο από την επάνω μπάρα και ελέγξτε/ανανεώστε τον συγχρονισμό Cloud με ένα πάτημα."
                    ),
                    t(
                        "Open the Activity History bell to see who added, updated, or deleted items in shared books.",
                        "Ανοίξτε το Ιστορικό Δραστηριότητας (καμπανάκι) για να βλέπετε ποιος πρόσθεσε ή άλλαξε κάθε εγγραφή."
                    ),
                    t(
                        "Create, rename, pick a custom currency (€, $, £, CHF, etc.), clear expenses, or delete books in Manage Expense Books.",
                        "Δημιουργήστε νέα εξοδολόγια, μετονομάστε τα, επιλέξτε νόμισμα (€, $, £, CHF κ.λπ.), καθαρίστε τα έξοδα ή διαγράψτε τα από τη «Διαχείριση εξοδολογίων»."
                    ),
                    t(
                        "Share a book with verified email accounts, manage roles (Creator, Editor, Registrar), and assign each user's default payer.",
                        "Κοινοποιήστε εξοδολόγια σε επιβεβαιωμένους χρήστες με ρόλους (Δημιουργός, Συντάκτης, Καταχωρητής) και αντιστοίχιση προεπιλεγμένου πληρωτή."
                    )
                )
            ),
            TabGuideSection(
                id = "reminders",
                title = t("Reminders (Recurring Bills)", "Υπενθυμίσεις (Πάγια Έξοδα)"),
                subtitle = t("Scheduled alerts for rent, utilities & subscriptions", "Προγραμματισμένοι λογαριασμοί & συνδρομές"),
                locationBadge = t("Side Menu", "Πλευρικό Μενού"),
                icon = Icons.Default.NotificationsActive,
                accentColor = Color(0xFFD97706),
                capabilities = listOf(
                    t(
                        "Create bill reminders with Weekly, Biweekly, Monthly, Bimonthly, or Yearly frequency.",
                        "Προγραμματίστε πάγιους λογαριασμούς με συχνότητα Εβδομαδιαία, Δεκαπενθήμερη, Μηνιαία, Διμηνιαία ή Ετήσια."
                    ),
                    t(
                        "Pick the exact day, month (for bimonthly/yearly), and time (hour & minute) for the alert.",
                        "Επιλέξτε την ακριβή ημέρα, μήνα εκκίνησης (όπου χρειάζεται) και ώρα υπενθύμισης."
                    ),
                    t(
                        "Receive a reminder banner only at the exact scheduled day & time, and log the pre-filled expense in 1 tap.",
                        "Η κάρτα υπενθύμισης εμφανίζεται ακριβώς τη μέρα και ώρα που ορίσατε, με καταχώρηση του εξόδου με 1 πάτημα."
                    ),
                    t(
                        "Pause, re-enable, edit, or delete reminders anytime.",
                        "Ενεργοποιήστε, παύστε προσωρινά, επεξεργαστείτε ή διαγράψτε οποιαδήποτε υπενθύμιση."
                    )
                )
            ),
            TabGuideSection(
                id = "goals",
                title = t("Savings Goals", "Στόχοι Αποταμίευσης"),
                subtitle = t("Targets linked directly to Expenses & Statistics", "Συνδεδεμένοι άμεσα με Έξοδα & Στατιστικά"),
                locationBadge = t("Side Menu", "Πλευρικό Μενού"),
                icon = Icons.Default.Savings,
                accentColor = Color(0xFF10B981),
                capabilities = listOf(
                    t(
                        "Create savings goals with target amount, calendar deadline, notes, or quick-start templates.",
                        "Δημιουργήστε στόχους με ποσό στόχου, προθεσμία από ημερολόγιο, σημείωση ή έτοιμα πρότυπα."
                    ),
                    t(
                        "Tap Deposit to transfer money into any goal, choosing the amount, payer member, and optional note.",
                        "Πατήστε «Κατάθεση» για να περάσετε χρήματα σε έναν στόχο επιλέγοντας ποσό, πληρωτή και προαιρετική σημείωση."
                    ),
                    t(
                        "Every deposit automatically creates a matching expense under \"Savings & Goals\" so it appears immediately on Home and in Statistics.",
                        "Κάθε κατάθεση καταχωρείται αυτόματα και ως έξοδο στην κατηγορία «Αποταμίευση & Στόχοι», ώστε να φαίνεται αμέσως στην Αρχική και στα Στατιστικά."
                    ),
                    t(
                        "Two-way sync: editing or deleting a goal-deposit expense on Home automatically updates the linked goal's balance.",
                        "Αμφίδρομος συγχρονισμός: αν αλλάξετε ποσό ή διαγράψετε την κατάθεση από τα Έξοδα, ενημερώνεται αυτόματα και ο αντίστοιχος στόχος."
                    )
                )
            ),
            TabGuideSection(
                id = "profile_settings",
                title = t("Profile, Settings & Legal", "Προφίλ, Ρυθμίσεις & Νομικά"),
                subtitle = t("Account, theme, language, notifications & privacy", "Λογαριασμός, θέμα, γλώσσα, ειδοποιήσεις & GDPR"),
                locationBadge = t("Side Menu & Avatar", "Πλευρικό Μενού & Άβαταρ"),
                icon = Icons.Default.Settings,
                accentColor = Color(0xFF475569),
                capabilities = listOf(
                    t(
                        "Profile: Update personal details and avatar photo, review account stats, manage Pro access, sign out, or delete your account.",
                        "Προφίλ: Ενημερώστε τα προσωπικά στοιχεία και τη φωτογραφία σας, δείτε στατιστικά, διαχειριστείτε την έκδοση Pro, αποσυνδεθείτε ή διαγράψτε τον λογαριασμό."
                    ),
                    t(
                        "Settings: Switch Light / Dark / System theme, change language (Greek / English), enable optional App Lock (Biometrics / 4-digit PIN), and toggle Quick Launch Chooser.",
                        "Ρυθμίσεις: Επιλέξτε Θέμα (Ανοιχτό / Σκούρο / Συστήματος), Γλώσσα (Ελληνικά / English), προαιρετικό Κλείδωμα Εφαρμογής (Βιομετρικά / 4-ψήφιο PIN) και γρήγορο μενού εκκίνησης."
                    ),
                    t(
                        "Notifications: Configure Evening Daily Reminder time and enable Bank/Google Pay Auto-Catcher (which remembers your category corrections per merchant).",
                        "Ειδοποιήσεις: Ορίστε ώρα βραδινής υπενθύμισης και ενεργοποιήστε τον Αυτόματο Εντοπισμό Τραπεζικών Συναλλαγών (με έξυπνη μνήμη κατηγορίας ανά έμπορο)."
                    ),
                    t(
                        "Widgets & Legal: Pin a Quick-Add Home Screen shortcut/tile and review Terms of Service & Privacy Policy (GDPR).",
                        "Συντομεύσεις & Όροι: Προσθέστε γρήγορο πλακίδιο (Widget) στην αρχική οθόνη και διαβάστε τους Όρους Χρήσης & την Πολιτική Απορρήτου."
                    )
                )
            ),
            TabGuideSection(
                id = "finsay_pro",
                title = t("Finsay Pro (Pricing & Benefits)", "Finsay Pro (Κόστος & Προνόμια)"),
                subtitle = t("Full power, automation & unlimited tools", "Όλες οι έξυπνες λειτουργίες χωρίς περιορισμούς"),
                locationBadge = t("Subscription / Lifetime", "Συνδρομή / Εφάπαξ"),
                icon = Icons.Default.Star,
                accentColor = Color(0xFFD97706),
                capabilities = listOf(
                    t(
                        "What is Finsay Pro? The upgraded edition of Finsay that removes Free limits and unlocks all smart automation and reporting tools.",
                        "Τι είναι το Finsay Pro; Είναι η αναβαθμισμένη έκδοση του Finsay που καταργεί τα όρια της δωρεάν έκδοσης και ξεκλειδώνει όλους τους έξυπνους αυτοματισμούς."
                    ),
                    t(
                        "Plans & Pricing: Monthly 2.99€/month • Yearly 29€/year (Popular — save over 19%) • Lifetime 49€ (one-time payment, forever).",
                        "Κόστος & Πακέτα: Μηνιαία 2,99€/μήνα • Ετήσια 29€/έτος (Δημοφιλές — εξοικονόμηση άνω του 19%) • Lifetime 49€ (εφάπαξ πληρωμή για πάντα)."
                    ),
                    t(
                        "Unlimited Expense Categories (Free edition includes up to 5 active expense categories).",
                        "Απεριόριστες Κατηγορίες Εξόδων (η δωρεάν έκδοση επιτρέπει έως 5 κατηγορίες εξόδων)."
                    ),
                    t(
                        "Unlimited Voice Entries (Free edition includes 15 voice entries per month).",
                        "Απεριόριστη Φωνητική Καταχώρηση (η δωρεάν έκδοση περιλαμβάνει 15 φωνητικές καταχωρήσεις τον μήνα)."
                    ),
                    t(
                        "Receipt Scanner with Camera & Gallery OCR and AADE QR recognition.",
                        "Σάρωση Αποδείξεων με Κάμερα, Συλλογή (OCR) & QR ΑΑΔΕ."
                    ),
                    t(
                        "Automatic Bank & Wallet Notification Catcher for expenses and incomes worldwide.",
                        "Αυτόματος Αναγνώστης Τραπεζικών Ειδοποιήσεων (παγκόσμιες τράπεζες & Google Pay) για έξοδα και έσοδα."
                    ),
                    t(
                        "Multiple independent Expense Books (Households) with custom currencies and sharing.",
                        "Πολλαπλά ανεξάρτητα Εξοδολόγια με επιλογή νομίσματος και κοινοποίηση σε μέλη."
                    ),
                    t(
                        "Unlimited Savings Goals (Free: 1) and Unlimited Bill Reminders / Standing Orders (Free: 2).",
                        "Απεριόριστοι Στόχοι Αποταμίευσης (δωρεάν: 1) & Απεριόριστες Υπενθυμίσεις / Πάγιες Εντολές (δωρεάν: 2)."
                    ),
                    t(
                        "Export detailed financial reports to PDF and Excel (CSV).",
                        "Εξαγωγή αναλυτικών οικονομικών αναφορών σε PDF & Excel (CSV)."
                    )
                )
            )
        )
    }

    fun buildSmartFeatureItems(lang: Localization.Language): List<SmartFeatureItem> {
        fun t(en: String, el: String) = Localization.tr(lang, en, el)
        return listOf(
            SmartFeatureItem(
                id = "voice",
                title = t("Natural Greek Voice Entry", "Φωνητική Καταγραφή στα Ελληνικά"),
                summary = t(
                    "Speak naturally to log an expense in seconds — amount, category, payer, and split members are parsed automatically before you confirm.",
                    "Πείτε το έξοδο με φυσικό λόγο και η εφαρμογή αναγνωρίζει αυτόματα ποσό, κατηγορία, πληρωτή και μέλη σε λίγα δευτερόλεπτα."
                ),
                highlights = listOf(
                    t(
                        "Recognizes Greek & decimal amounts, categories, payer prefixes, and beneficiary members.",
                        "Αναγνωρίζει ποσά με δεκαδικά, λέξεις-κλειδιά κατηγοριών, ποιος πλήρωσε και για ποιον."
                    ),
                    t(
                        "Always shows an editable confirmation card before saving.",
                        "Εμφανίζει πάντα κάρτα επιβεβαίωσης για έλεγχο ή τροποποίηση πριν την αποθήκευση."
                    )
                ),
                tipText = t(
                    "Try saying: \"24.50 euros supermarket paid by Marios\" or \"12 euros coffee for Anna\"",
                    "Παράδειγμα: «24,50 ευρώ σούπερ μάρκετ πλήρωσε ο Μάριος» ή «12 ευρώ καφές για τη Μαρία»"
                ),
                icon = Icons.Default.Mic,
                accentColor = Color(0xFF7C3AED)
            ),
            SmartFeatureItem(
                id = "receipt",
                title = t("Receipt Scanner (OCR & AADE QR)", "Σάρωση Απόδειξης (Κάμερα OCR & QR ΑΑΔΕ)"),
                summary = t(
                    "Point your camera at a printed receipt or AADE QR code to auto-fill total amount, merchant name, and date.",
                    "Σαρώστε έντυπες αποδείξεις με την κάμερα ή τον κωδικό QR ΑΑΔΕ για άμεση συμπλήρωση καταστήματος, ποσού και ημερομηνίας."
                ),
                highlights = listOf(
                    t(
                        "Supports both AADE QR scanning and printed receipt text OCR.",
                        "Υποστηρίζει ανάγνωση QR ΑΑΔΕ αλλά και οπτική αναγνώριση κειμένου (OCR)."
                    ),
                    t(
                        "Smart merchant learning remembers your preferred category per store / VAT ID.",
                        "Έξυπνη εκμάθηση: θυμάται την κατηγορία που επιλέγετε για κάθε κατάστημα ή ΑΦΜ."
                    )
                ),
                tipText = null,
                icon = Icons.Default.CameraAlt,
                accentColor = Color(0xFF059669)
            ),
            SmartFeatureItem(
                id = "bank_auto",
                title = t("Bank & Google Pay Auto-Catcher", "Αυτόματος Εντοπισμός Τραπεζικών Συναλλαγών"),
                summary = t(
                    "Optionally reads payment and deposit notifications from Greek and international banks/wallets (Google Pay, Revolut, Wise, PayPal, etc.) to prepare expenses and incomes automatically.",
                    "Εντοπίζει ειδοποιήσεις πληρωμών και καταθέσεων από ελληνικές και διεθνείς τράπεζες ή ψηφιακά πορτοφόλια (Google Pay, Revolut, Wise, PayPal κ.ά.) και τις ετοιμάζει για καταχώρηση με 1 πάτημα."
                ),
                highlights = listOf(
                    t(
                        "Automatically distinguishes expenses from incoming credits/deposits while filtering out spam, promos, and OTP alerts.",
                        "Ξεχωρίζει αυτόματα τα Έξοδα από τα Έσοδα (πιστώσεις/καταθέσεις) και αγνοεί προσφορές, διαφημίσεις και κωδικούς OTP."
                    ),
                    t(
                        "Scoped strictly to the active expense book — nothing is saved silently until you confirm.",
                        "Τίποτα δεν αποθηκεύεται χωρίς την έγκρισή σας — επιλέγετε Καταχώρηση ή Αγνόηση."
                    )
                ),
                tipText = t(
                    "Enable or disable anytime under Menu → Settings (Finsay Pro feature).",
                    "Ενεργοποιείται ή απενεργοποιείται ανά πάσα στιγμή από Μενού → Ρυθμίσεις (δυνατότητα Finsay Pro)."
                ),
                icon = Icons.Default.NotificationsNone,
                accentColor = Color(0xFF0284C7)
            ),
            SmartFeatureItem(
                id = "goals_sync",
                title = t("Goals Connected to Expenses & Statistics", "Στόχοι Συνδεδεμένοι με Έξοδα & Στατιστικά"),
                summary = t(
                    "Money put aside for your goals is part of your real cashflow — never isolated from your budget.",
                    "Τα χρήματα που αποταμιεύετε στους στόχους αποτυπώνονται άμεσα στις κινήσεις και στα οικονομικά σας στατιστικά."
                ),
                highlights = listOf(
                    t(
                        "Depositing into a goal logs an expense under \"Savings & Goals\" automatically.",
                        "Κάθε κατάθεση σε στόχο δημιουργεί αυτόματα έξοδο στην κατηγορία «Αποταμίευση & Στόχοι»."
                    ),
                    t(
                        "Visible on Home, in Category Breakdown, and in the dedicated Savings Goals card in Statistics.",
                        "Εμφανίζεται στην Αρχική, στην ανάλυση κατηγοριών και στην κάρτα Στόχων στα Στατιστικά."
                    )
                ),
                tipText = t(
                    "Editing or deleting the deposit on Home keeps the goal progress in sync.",
                    "Αν επεξεργαστείτε ή διαγράψετε την κατάθεση από την Αρχική, ενημερώνεται αυτόματα και ο στόχος."
                ),
                icon = Icons.Default.Savings,
                accentColor = Color(0xFF10B981)
            ),
            SmartFeatureItem(
                id = "roles_settle",
                title = t("Shared Books, Roles & Settle-Up", "Κοινά Εξοδολόγια, Ρόλοι & Εκκαθάριση (Settle-Up)"),
                summary = t(
                    "Collaborate in real time with family, partners, or roommates with role-based permissions and fair debt settlement.",
                    "Συνεργαστείτε σε πραγματικό χρόνο με οικογένεια ή συγκατοίκους, με σαφή δικαιώματα και αυτόματο υπολογισμό οφειλών."
                ),
                highlights = listOf(
                    t(
                        "Creator: full control, sharing & deletion. Editor: full expense, member, category, goal & reminder management. Registrar: fast expense/income logging.",
                        "Δημιουργός: πλήρης διαχείριση, κοινοποίηση & διαγραφή. Συντάκτης: πλήρης διαχείριση εξόδων, μελών, κατηγοριών, στόχων & υπενθυμίσεων. Καταχωρητής: γρήγορη προσθήκη εξόδων/εσόδων."
                    ),
                    t(
                        "Settle-Up calculates who owes whom and records repayment transfers in 1 tap.",
                        "Η Εκκαθάριση Οφειλών δείχνει ποιος χρωστάει σε ποιον και καταχωρεί την εξόφληση με 1 πάτημα."
                    )
                ),
                tipText = null,
                icon = Icons.Default.SwapHoriz,
                accentColor = Color(0xFF0D9488)
            ),
            SmartFeatureItem(
                id = "export_cloud",
                title = t("Cloud Sync, Activity Log, Undo & Reports", "Συγχρονισμός Cloud, Ιστορικό, Αναίρεση & Αναφορές"),
                summary = t(
                    "Your books stay backed up in the cloud across devices with full accountability and safe editing.",
                    "Τα δεδομένα σας συγχρονίζονται με ασφάλεια στο cloud με πλήρη έλεγχο αλλαγών και εύκολη εξαγωγή."
                ),
                highlights = listOf(
                    t(
                        "Live Firestore synchronization with instant manual refresh from the top bar.",
                        "Ζωντανός συγχρονισμός μεταξύ συσκευών και χειροκίνητη ανανέωση από το επάνω σύμβολο cloud."
                    ),
                    t(
                        "5-second Undo banner after deleting any expense or income.",
                        "Μπάρα Αναίρεσης (5 δευτ.) μετά από διαγραφή εξόδου ή εσόδου για προστασία από λάθη."
                    ),
                    t(
                        "Export clean Excel (CSV) and PDF summaries directly from Statistics.",
                        "Εξαγωγή αναλυτικών αναφορών σε Excel (CSV) και PDF μέσα από τα Στατιστικά."
                    )
                ),
                tipText = null,
                icon = Icons.Default.CloudSync,
                accentColor = Color(0xFFD97706)
            )
        )
    }
}
