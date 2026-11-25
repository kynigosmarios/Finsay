package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

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

    fun buildQuickStartSteps(isEnglish: Boolean): List<QuickStartStep> {
        return if (isEnglish) {
            listOf(
                QuickStartStep(
                    number = 1,
                    title = "Add Household Members",
                    whereLabel = "Members Tab",
                    description = "Add family members or roommates with their role (Parent, Child, Roommate), color, and voice keywords. Link each member to their user account so they are automatically pre-selected as the default payer.",
                    icon = Icons.Default.Group,
                    accentColor = Color(0xFF0284C7),
                    actionType = "ADD_MEMBER"
                ),
                QuickStartStep(
                    number = 2,
                    title = "Set Up Categories & Budgets",
                    whereLabel = "Categories Tab",
                    description = "Add the standard categories in 1 tap or create your own with custom icons, colors, voice keywords, and monthly budget limits to get automatic overspending alerts.",
                    icon = Icons.Default.Category,
                    accentColor = Color(0xFF059669),
                    actionType = "SEED_CATEGORIES"
                ),
                QuickStartStep(
                    number = 3,
                    title = "Name or Share Your Expense Book",
                    whereLabel = "Top Bar → Manage Books",
                    description = "Rename your active expense book, create separate books (e.g. Home, Personal, Vacation), or share with verified users by assigning roles (Creator, Editor, Registrar).",
                    icon = Icons.Default.Book,
                    accentColor = Color(0xFF0D9488)
                ),
                QuickStartStep(
                    number = 4,
                    title = "Record Your First Expense or Income",
                    whereLabel = "Home Tab & Floating Mic",
                    description = "Tap + Expense or + Income, press the floating microphone button to speak naturally, or scan a paper receipt using your camera or AADE QR code.",
                    icon = Icons.Default.Mic,
                    accentColor = Color(0xFF7C3AED)
                ),
                QuickStartStep(
                    number = 5,
                    title = "Turn On Reminders, Goals & Bank Auto-Catch",
                    whereLabel = "Side Menu & Settings",
                    description = "Schedule recurring bill reminders (rent, utilities, subscriptions), create savings goals that link directly to your expenses, and optionally enable automatic bank/Google Pay notification capture in Settings.",
                    icon = Icons.Default.NotificationsActive,
                    accentColor = Color(0xFFD97706)
                )
            )
        } else {
            listOf(
                QuickStartStep(
                    number = 1,
                    title = "Προσθέστε τα Μέλη σας",
                    whereLabel = "Καρτέλα «Μέλη»",
                    description = "Καταχωρήστε τα άτομα της οικογένειας ή της παρέας (Γονέας, Παιδί, Συγκάτοικος κ.ά.) με χρώμα και λέξεις φωνής. Συνδέστε κάθε μέλος με τον λογαριασμό του ώστε να προεπιλέγεται αυτόματα ως πληρωτής.",
                    icon = Icons.Default.Group,
                    accentColor = Color(0xFF0284C7),
                    actionType = "ADD_MEMBER"
                ),
                QuickStartStep(
                    number = 2,
                    title = "Ρυθμίστε Κατηγορίες & Μηνιαία Όρια",
                    whereLabel = "Καρτέλα «Κατηγορίες»",
                    description = "Προσθέστε τις βασικές κατηγορίες με 1 πάτημα ή φτιάξτε δικές σας με εικονίδια, χρώματα, λέξεις-κλειδιά φωνής και μηνιαίο προϋπολογισμό (budget) για έγκαιρες ειδοποιήσεις.",
                    icon = Icons.Default.Category,
                    accentColor = Color(0xFF059669),
                    actionType = "SEED_CATEGORIES"
                ),
                QuickStartStep(
                    number = 3,
                    title = "Οργανώστε ή Μοιραστείτε το Εξοδολόγιο",
                    whereLabel = "Επάνω Μπάρα / Διαχείριση Εξοδολογίων",
                    description = "Μετονομάστε το εξοδολόγιό σας, δημιουργήστε ξεχωριστά βιβλία (π.χ. Σπίτι, Προσωπικό, Ταξίδι) ή κοινοποιήστε το σε άλλους χρήστες με ρόλους (Δημιουργός, Συντάκτης, Καταχωρητής).",
                    icon = Icons.Default.Book,
                    accentColor = Color(0xFF0D9488)
                ),
                QuickStartStep(
                    number = 4,
                    title = "Καταχωρήστε το πρώτο Έξοδο ή Έσοδο",
                    whereLabel = "Καρτέλα «Αρχική» & Μικρόφωνο",
                    description = "Πατήστε + Έξοδο ή + Έσοδο, χρησιμοποιήστε το πλωτό μικρόφωνο για φωνητική καταγραφή στα Ελληνικά, ή σαρώστε απόδειξη με την κάμερα ή QR code ΑΑΔΕ.",
                    icon = Icons.Default.Mic,
                    accentColor = Color(0xFF7C3AED)
                ),
                QuickStartStep(
                    number = 5,
                    title = "Ενεργοποιήστε Υπενθυμίσεις, Στόχους & Τράπεζες",
                    whereLabel = "Πλευρικό Μενού & Ρυθμίσεις",
                    description = "Ορίστε υπενθυμίσεις πάγιων λογαριασμών, φτιάξτε στόχους αποταμίευσης που συνδέονται άμεσα με τα έξοδα και ενεργοποιήστε (προαιρετικά) τον αυτόματο εντοπισμό τραπεζικών ειδοποιήσεων στις Ρυθμίσεις.",
                    icon = Icons.Default.NotificationsActive,
                    accentColor = Color(0xFFD97706)
                )
            )
        }
    }

    fun buildTabGuideSections(isEnglish: Boolean): List<TabGuideSection> {
        return if (isEnglish) {
            listOf(
                TabGuideSection(
                    id = "home",
                    title = "Home (Expenses & Income)",
                    subtitle = "Daily ledger, fast entry & filters",
                    locationBadge = "Bottom Tab 1",
                    icon = Icons.Default.Receipt,
                    accentColor = Color(0xFF059669),
                    capabilities = listOf(
                        "View your monthly summary card (Total Expenses, Income & Net Balance) and switch between months.",
                        "Add expenses manually with category, date, single or multi-payer split, beneficiary members, and notes.",
                        "Record household incomes (salary, freelance, benefits, etc.) per member.",
                        "Log expenses by voice using the floating microphone button, or scan receipts via Camera OCR & AADE QR.",
                        "Confirm or dismiss detected bank/Google Pay payment notifications and due bill reminders right from the top banners.",
                        "Filter transactions by month, type (Expenses / Income / All), member, category, or free-text search.",
                        "Tap any entry to edit it, swipe left on an expense card to delete it immediately (with a 5-second Undo banner), or swipe right to repeat the expense today.",
                        "Long-press the Finsay app icon on your home screen for instant App Shortcuts: Voice Entry, Scan Receipt, or New Expense."
                    )
                ),
                TabGuideSection(
                    id = "analytics",
                    title = "Statistics",
                    subtitle = "Charts, forecasts, budgets & settle-up",
                    locationBadge = "Bottom Tab 2",
                    icon = Icons.Default.PieChart,
                    accentColor = Color(0xFF0D9488),
                    capabilities = listOf(
                        "Switch analysis scope between Month, Year, or All recorded data.",
                        "Track Budget Alerts when categories approach or exceed their monthly limits.",
                        "Inspect KPI totals (Income, Expenses, Balance, Savings Rate, Daily Average & Peak Spending Day) and monthly forecast vs. previous month.",
                        "Explore breakdowns by Income, Family vs. Children, Members/Payers, Categories, and Savings Goals — tap any item to drill down straight into matching Home transactions.",
                        "Use automatic Settle-Up to see exactly who owes whom and record a settlement transfer in 1 tap.",
                        "Export and share structured reports to Excel (CSV) and PDF."
                    )
                ),
                TabGuideSection(
                    id = "members",
                    title = "Members",
                    subtitle = "People, roles & automatic default payer",
                    locationBadge = "Bottom Tab 3",
                    icon = Icons.Default.Group,
                    accentColor = Color(0xFF0284C7),
                    capabilities = listOf(
                        "Add, edit, color-code, and reorder members (Parent, Child, Roommate, etc.).",
                        "Set custom voice keywords per member for accurate Greek voice recognition.",
                        "Link each member to a connected user account so their expenses automatically pre-select them as Payer.",
                        "Move removed members to the Recycle Bin and restore or permanently delete them anytime."
                    )
                ),
                TabGuideSection(
                    id = "categories",
                    title = "Categories (Expenses & Income)",
                    subtitle = "Custom icons, colors, income categories & budgets",
                    locationBadge = "Bottom Tab 4",
                    icon = Icons.Default.Category,
                    accentColor = Color(0xFF7C3AED),
                    capabilities = listOf(
                        "Manage both Expense Categories and Income Categories using the switcher at the top.",
                        "Free edition allows up to 5 active Expense Categories — upgrade to Finsay Pro for unlimited expense categories.",
                        "Customize icons, badge colors, ledger font colors, display order, and voice keywords for both expenses and incomes.",
                        "Set monthly budget caps on expense categories for live progress bars and overspending alerts (income categories do not require a budget).",
                        "Restore deleted categories anytime from the Category Recycle Bin."
                    )
                ),
                TabGuideSection(
                    id = "households",
                    title = "Expense Books & Top Bar",
                    subtitle = "Multiple books, cloud sync, sharing & history",
                    locationBadge = "Top Bar & Menu",
                    icon = Icons.Default.Book,
                    accentColor = Color(0xFF047857),
                    capabilities = listOf(
                        "Quickly switch your active expense book from the top bar or check real-time Cloud Sync status with manual refresh.",
                        "Open the Activity History bell to see who added, updated, or deleted items in shared books.",
                        "Create, rename, pick a custom currency (€, $, £, CHF, etc.), clear expenses, or delete books in Manage Expense Books.",
                        "Share a book with verified email accounts, manage roles (Creator, Editor, Registrar), and assign each user's default payer."
                    )
                ),
                TabGuideSection(
                    id = "reminders",
                    title = "Reminders (Recurring Bills)",
                    subtitle = "Scheduled alerts for rent, utilities & subscriptions",
                    locationBadge = "Side Menu",
                    icon = Icons.Default.NotificationsActive,
                    accentColor = Color(0xFFD97706),
                    capabilities = listOf(
                        "Create bill reminders with Weekly, Biweekly, Monthly, Bimonthly, or Yearly frequency.",
                        "Pick the exact day, month (for bimonthly/yearly), and time (hour & minute) for the alert.",
                        "Receive a reminder banner only at the exact scheduled day & time, and log the pre-filled expense in 1 tap.",
                        "Pause, re-enable, edit, or delete reminders anytime."
                    )
                ),
                TabGuideSection(
                    id = "goals",
                    title = "Savings Goals",
                    subtitle = "Targets linked directly to Expenses & Statistics",
                    locationBadge = "Side Menu",
                    icon = Icons.Default.Savings,
                    accentColor = Color(0xFF10B981),
                    capabilities = listOf(
                        "Create savings goals with target amount, calendar deadline, notes, or quick-start templates.",
                        "Tap Deposit to transfer money into any goal, choosing the amount, payer member, and optional note.",
                        "Every deposit automatically creates a matching expense under \"Savings & Goals\" so it appears immediately on Home and in Statistics.",
                        "Two-way sync: editing or deleting a goal-deposit expense on Home automatically updates the linked goal's balance."
                    )
                ),
                TabGuideSection(
                    id = "profile_settings",
                    title = "Profile, Settings & Legal",
                    subtitle = "Account, theme, language, notifications & privacy",
                    locationBadge = "Side Menu & Avatar",
                    icon = Icons.Default.Settings,
                    accentColor = Color(0xFF475569),
                    capabilities = listOf(
                        "Profile: Update personal details and avatar photo, review account stats, manage Pro access, sign out, or delete your account.",
                        "Settings: Switch Light / Dark / System theme, change language (Greek / English), enable optional App Lock (Biometrics / 4-digit PIN), and toggle Quick Launch Chooser.",
                        "Notifications: Configure Evening Daily Reminder time and enable Bank/Google Pay Auto-Catcher (which remembers your category corrections per merchant).",
                        "Widgets & Legal: Pin a Quick-Add Home Screen shortcut/tile and review Terms of Service & Privacy Policy (GDPR)."
                    )
                ),
                TabGuideSection(
                    id = "finsay_pro",
                    title = "Finsay Pro (Pricing & Benefits)",
                    subtitle = "Full power, automation & unlimited tools",
                    locationBadge = "Subscription / Lifetime",
                    icon = Icons.Default.Star,
                    accentColor = Color(0xFFD97706),
                    capabilities = listOf(
                        "What is Finsay Pro? The upgraded edition of Finsay that removes Free limits and unlocks all smart automation and reporting tools.",
                        "Plans & Pricing: Monthly 2.99€/month • Yearly 29€/year (Popular — save over 19%) • Lifetime 49€ (one-time payment, forever).",
                        "Unlimited Expense Categories (Free edition includes up to 5 active expense categories).",
                        "Unlimited Voice Entries (Free edition includes 15 voice entries per month).",
                        "Receipt Scanner with Camera & Gallery OCR and AADE QR recognition.",
                        "Automatic Bank & Wallet Notification Catcher for expenses and incomes worldwide.",
                        "Multiple independent Expense Books (Households) with custom currencies and sharing.",
                        "Unlimited Savings Goals (Free: 1) and Unlimited Bill Reminders / Standing Orders (Free: 2).",
                        "Export detailed financial reports to PDF and Excel (CSV)."
                    )
                )
            )
        } else {
            listOf(
                TabGuideSection(
                    id = "home",
                    title = "Αρχική (Έξοδα & Έσοδα)",
                    subtitle = "Καθημερινές κινήσεις, γρήγορη καταχώρηση & φίλτρα",
                    locationBadge = "1η Καρτέλα",
                    icon = Icons.Default.Receipt,
                    accentColor = Color(0xFF059669),
                    capabilities = listOf(
                        "Δείτε τη σύνοψη του μήνα (Συνολικά Έξοδα, Έσοδα & Υπόλοιπο) και μετακινηθείτε εύκολα μεταξύ μηνών.",
                        "Καταχωρήστε έξοδα χειροκίνητα με κατηγορία, ημερομηνία, πληρωτή (ή πολλαπλούς πληρωτές), επιμερισμό σε μέλη και σημείωση.",
                        "Προσθέστε έσοδα (μισθός, επίδομα, ελεύθερο επάγγελμα κ.ά.) ανά μέλος.",
                        "Καταγράψτε έξοδα με τη φωνή σας στα Ελληνικά (πλωτό μικρόφωνο) ή σκανάρετε αποδείξεις με Κάμερα OCR & QR ΑΑΔΕ.",
                        "Επιβεβαιώστε ή απορρίψτε αυτόματα εντοπισμένες τραπεζικές πληρωμές και ειδοποιήσεις πάγιων λογαριασμών από το επάνω μέρος.",
                        "Φιλτράρετε τις κινήσεις ανά μήνα, τύπο (Έξοδα / Έσοδα / Όλα), μέλος, κατηγορία ή ελεύθερη αναζήτηση.",
                        "Πατήστε σε οποιαδήποτε εγγραφή για επεξεργασία, σύρετε αριστερά σε μια κάρτα εξόδου για άμεση διαγραφή (με 5 δευτ. Αναίρεση), ή σύρετε δεξιά για άμεση «Επανάληψη σήμερα».",
                        "Πατήστε παρατεταμένα το εικονίδιο του Finsay στην αρχική οθόνη για άμεσες συντομεύσεις: Φωνητική Καταχώρηση, Σάρωση Απόδειξης ή Νέο Έξοδο."
                    )
                ),
                TabGuideSection(
                    id = "analytics",
                    title = "Στατιστικά",
                    subtitle = "Γραφήματα, προβλέψεις, προϋπολογισμός & εκκαθάριση",
                    locationBadge = "2η Καρτέλα",
                    icon = Icons.Default.PieChart,
                    accentColor = Color(0xFF0D9488),
                    capabilities = listOf(
                        "Επιλέξτε ανάλυση ανά Μήνα, Έτος ή για Όλα τα καταγεγραμμένα δεδομένα.",
                        "Λάβετε ειδοποιήσεις προϋπολογισμού (Budget Alerts) όταν μια κατηγορία πλησιάζει ή ξεπερνά το όριο.",
                        "Δείτε συγκεντρωτικούς δείκτες (Έσοδα, Έξοδα, Καθαρό Υπόλοιπο, Δείκτη Αποταμίευσης, Μέσο Όρο/Ημέρα & Ημέρα αιχμής) και μηνιαία πρόβλεψη σε σύγκριση με τον προηγούμενο μήνα.",
                        "Εξερευνήστε γραφήματα για Έσοδα, Γονείς/Παιδιά, Μέλη/Πληρωτές, Κατηγορίες και Στόχους Αποταμίευσης — πατήστε σε κάθε στοιχείο για άμεσο φιλτράρισμα στην Αρχική.",
                        "Χρησιμοποιήστε την Εκκαθάριση Οφειλών (Settle-Up) για να δείτε ποιος χρωστάει σε ποιον και να καταχωρήσετε εξόφληση με 1 πάτημα.",
                        "Εξάγετε και μοιραστείτε αναλυτικές αναφορές σε Excel (CSV) και PDF."
                    )
                ),
                TabGuideSection(
                    id = "members",
                    title = "Μέλη",
                    subtitle = "Άτομα εξοδολογίου, ρόλοι & προεπιλεγμένος πληρωτής",
                    locationBadge = "3η Καρτέλα",
                    icon = Icons.Default.Group,
                    accentColor = Color(0xFF0284C7),
                    capabilities = listOf(
                        "Προσθέστε, επεξεργαστείτε, χρωματίστε και αλλάξτε τη σειρά των μελών (Γονέας, Παιδί, Συγκάτοικος κ.ά.).",
                        "Ορίστε λέξεις-κλειδιά φωνής για κάθε μέλος ώστε να αναγνωρίζεται άμεσα στη φωνητική καταγραφή.",
                        "Συνδέστε ένα μέλος με λογαριασμό χρήστη ώστε όταν αυτός καταχωρεί έξοδα να επιλέγεται αυτόματα ως Πληρωτής.",
                        "Ανακτήστε ή διαγράψτε οριστικά παλαιότερα μέλη από τον Κάδο Ανακύκλωσης."
                    )
                ),
                TabGuideSection(
                    id = "categories",
                    title = "Κατηγορίες (Εξόδων & Εσόδων)",
                    subtitle = "Εικονίδια, χρώματα, κατηγορίες εσόδων & budget",
                    locationBadge = "4η Καρτέλα",
                    icon = Icons.Default.Category,
                    accentColor = Color(0xFF7C3AED),
                    capabilities = listOf(
                        "Διαχειριστείτε τόσο Κατηγορίες Εξόδων όσο και Κατηγορίες Εσόδων από τον επιλογέα στο επάνω μέρος.",
                        "Στη δωρεάν έκδοση επιτρέπονται έως 5 ενεργές Κατηγορίες Εξόδων — με το Finsay Pro έχετε απεριόριστες κατηγορίες εξόδων.",
                        "Προσαρμόστε εικονίδιο, χρώμα κάρτας, χρώμα γραμματοσειράς, σειρά εμφάνισης και λέξεις-κλειδιά φωνής.",
                        "Ορίστε μηνιαίο όριο (budget) στις κατηγορίες εξόδων για μπάρες προόδου και ειδοποιήσεις υπέρβασης (στις κατηγορίες εσόδων δεν απαιτείται budget).",
                        "Επαναφέρετε διαγραμμένες κατηγορίες ανά πάσα στιγμή από τον Κάδο Ανακύκλωσης."
                    )
                ),
                TabGuideSection(
                    id = "households",
                    title = "Εξοδολόγια & Επάνω Μπάρα",
                    subtitle = "Πολλαπλά βιβλία, cloud συγχρονισμός & κοινοποίηση",
                    locationBadge = "Επάνω Μπάρα & Μενού",
                    icon = Icons.Default.Book,
                    accentColor = Color(0xFF047857),
                    capabilities = listOf(
                        "Αλλάξτε ενεργό εξοδολόγιο από την επάνω μπάρα και ελέγξτε/ανανεώστε τον συγχρονισμό Cloud με ένα πάτημα.",
                        "Ανοίξτε το Ιστορικό Δραστηριότητας (καμπανάκι) για να βλέπετε ποιος πρόσθεσε ή άλλαξε κάθε εγγραφή.",
                        "Δημιουργήστε νέα εξοδολόγια, μετονομάστε τα, επιλέξτε νόμισμα (€, $, £, CHF κ.λπ.), καθαρίστε τα έξοδα ή διαγράψτε τα από τη «Διαχείριση εξοδολογίων».",
                        "Κοινοποιήστε εξοδολόγια σε επιβεβαιωμένους χρήστες με ρόλους (Δημιουργός, Συντάκτης, Καταχωρητής) και αντιστοίχιση προεπιλεγμένου πληρωτή."
                    )
                ),
                TabGuideSection(
                    id = "reminders",
                    title = "Υπενθυμίσεις (Πάγια Έξοδα)",
                    subtitle = "Προγραμματισμένοι λογαριασμοί & συνδρομές",
                    locationBadge = "Πλευρικό Μενού",
                    icon = Icons.Default.NotificationsActive,
                    accentColor = Color(0xFFD97706),
                    capabilities = listOf(
                        "Προγραμματίστε πάγιους λογαριασμούς με συχνότητα Εβδομαδιαία, Δεκαπενθήμερη, Μηνιαία, Διμηνιαία ή Ετήσια.",
                        "Επιλέξτε την ακριβή ημέρα, μήνα εκκίνησης (όπου χρειάζεται) και ώρα υπενθύμισης.",
                        "Η κάρτα υπενθύμισης εμφανίζεται ακριβώς τη μέρα και ώρα που ορίσατε, με καταχώρηση του εξόδου με 1 πάτημα.",
                        "Ενεργοποιήστε, παύστε προσωρινά, επεξεργαστείτε ή διαγράψτε οποιαδήποτε υπενθύμιση."
                    )
                ),
                TabGuideSection(
                    id = "goals",
                    title = "Στόχοι Αποταμίευσης",
                    subtitle = "Συνδεδεμένοι άμεσα με Έξοδα & Στατιστικά",
                    locationBadge = "Πλευρικό Μενού",
                    icon = Icons.Default.Savings,
                    accentColor = Color(0xFF10B981),
                    capabilities = listOf(
                        "Δημιουργήστε στόχους με ποσό στόχου, προθεσμία από ημερολόγιο, σημείωση ή έτοιμα πρότυπα.",
                        "Πατήστε «Κατάθεση» για να περάσετε χρήματα σε έναν στόχο επιλέγοντας ποσό, πληρωτή και προαιρετική σημείωση.",
                        "Κάθε κατάθεση καταχωρείται αυτόματα και ως έξοδο στην κατηγορία «Αποταμίευση & Στόχοι», ώστε να φαίνεται αμέσως στην Αρχική και στα Στατιστικά.",
                        "Αμφίδρομος συγχρονισμός: αν αλλάξετε ποσό ή διαγράψετε την κατάθεση από τα Έξοδα, ενημερώνεται αυτόματα και ο αντίστοιχος στόχος."
                    )
                ),
                TabGuideSection(
                    id = "profile_settings",
                    title = "Προφίλ, Ρυθμίσεις & Νομικά",
                    subtitle = "Λογαριασμός, θέμα, γλώσσα, ειδοποιήσεις & GDPR",
                    locationBadge = "Πλευρικό Μενού & Άβαταρ",
                    icon = Icons.Default.Settings,
                    accentColor = Color(0xFF475569),
                    capabilities = listOf(
                        "Προφίλ: Ενημερώστε τα προσωπικά στοιχεία και τη φωτογραφία σας, δείτε στατιστικά, διαχειριστείτε την έκδοση Pro, αποσυνδεθείτε ή διαγράψτε τον λογαριασμό.",
                        "Ρυθμίσεις: Επιλέξτε Θέμα (Ανοιχτό / Σκούρο / Συστήματος), Γλώσσα (Ελληνικά / English), προαιρετικό Κλείδωμα Εφαρμογής (Βιομετρικά / 4-ψήφιο PIN) και γρήγορο μενού εκκίνησης.",
                        "Ειδοποιήσεις: Ορίστε ώρα βραδινής υπενθύμισης και ενεργοποιήστε τον Αυτόματο Εντοπισμό Τραπεζικών Συναλλαγών (με έξυπνη μνήμη κατηγορίας ανά έμπορο).",
                        "Συντομεύσεις & Όροι: Προσθέστε γρήγορο πλακίδιο (Widget) στην αρχική οθόνη και διαβάστε τους Όρους Χρήσης & την Πολιτική Απορρήτου."
                    )
                ),
                TabGuideSection(
                    id = "finsay_pro",
                    title = "Finsay Pro (Κόστος & Προνόμια)",
                    subtitle = "Όλες οι έξυπνες λειτουργίες χωρίς περιορισμούς",
                    locationBadge = "Συνδρομή / Εφάπαξ",
                    icon = Icons.Default.Star,
                    accentColor = Color(0xFFD97706),
                    capabilities = listOf(
                        "Τι είναι το Finsay Pro; Είναι η αναβαθμισμένη έκδοση του Finsay που καταργεί τα όρια της δωρεάν έκδοσης και ξεκλειδώνει όλους τους έξυπνους αυτοματισμούς.",
                        "Κόστος & Πακέτα: Μηνιαία 2,99€/μήνα • Ετήσια 29€/έτος (Δημοφιλές — εξοικονόμηση άνω του 19%) • Lifetime 49€ (εφάπαξ πληρωμή για πάντα).",
                        "Απεριόριστες Κατηγορίες Εξόδων (η δωρεάν έκδοση επιτρέπει έως 5 κατηγορίες εξόδων).",
                        "Απεριόριστη Φωνητική Καταχώρηση (η δωρεάν έκδοση περιλαμβάνει 15 φωνητικές καταχωρήσεις τον μήνα).",
                        "Σάρωση Αποδείξεων με Κάμερα, Συλλογή (OCR) & QR ΑΑΔΕ.",
                        "Αυτόματος Αναγνώστης Τραπεζικών Ειδοποιήσεων (παγκόσμιες τράπεζες & Google Pay) για έξοδα και έσοδα.",
                        "Πολλαπλά ανεξάρτητα Εξοδολόγια με επιλογή νομίσματος και κοινοποίηση σε μέλη.",
                        "Απεριόριστοι Στόχοι Αποταμίευσης (δωρεάν: 1) & Απεριόριστες Υπενθυμίσεις / Πάγιες Εντολές (δωρεάν: 2).",
                        "Εξαγωγή αναλυτικών οικονομικών αναφορών σε PDF & Excel (CSV)."
                    )
                )
            )
        }
    }

    fun buildSmartFeatureItems(isEnglish: Boolean): List<SmartFeatureItem> {
        return if (isEnglish) {
            listOf(
                SmartFeatureItem(
                    id = "voice",
                    title = "Natural Greek Voice Entry",
                    summary = "Speak naturally to log an expense in seconds — amount, category, payer, and split members are parsed automatically before you confirm.",
                    highlights = listOf(
                        "Recognizes Greek & decimal amounts, categories, payer prefixes, and beneficiary members.",
                        "Always shows an editable confirmation card before saving."
                    ),
                    tipText = "Try saying: \"24.50 euros supermarket paid by Marios\" or \"12 euros coffee for Anna\"",
                    icon = Icons.Default.Mic,
                    accentColor = Color(0xFF7C3AED)
                ),
                SmartFeatureItem(
                    id = "receipt",
                    title = "Receipt Scanner (OCR & AADE QR)",
                    summary = "Point your camera at a printed receipt or AADE QR code to auto-fill total amount, merchant name, and date.",
                    highlights = listOf(
                        "Supports both AADE QR scanning and printed receipt text OCR.",
                        "Smart merchant learning remembers your preferred category per store / VAT ID."
                    ),
                    tipText = null,
                    icon = Icons.Default.CameraAlt,
                    accentColor = Color(0xFF059669)
                ),
                SmartFeatureItem(
                    id = "bank_auto",
                    title = "Bank & Google Pay Auto-Catcher",
                    summary = "Optionally reads payment and deposit notifications from Greek and international banks/wallets (Google Pay, Revolut, Wise, PayPal, etc.) to prepare expenses and incomes automatically.",
                    highlights = listOf(
                        "Automatically distinguishes expenses from incoming credits/deposits while filtering out spam, promos, and OTP alerts.",
                        "Scoped strictly to the active expense book — nothing is saved silently until you confirm."
                    ),
                    tipText = "Enable or disable anytime under Menu → Settings (Finsay Pro feature).",
                    icon = Icons.Default.NotificationsNone,
                    accentColor = Color(0xFF0284C7)
                ),
                SmartFeatureItem(
                    id = "goals_sync",
                    title = "Goals Connected to Expenses & Statistics",
                    summary = "Money put aside for your goals is part of your real cashflow — never isolated from your budget.",
                    highlights = listOf(
                        "Depositing into a goal logs an expense under \"Savings & Goals\" automatically.",
                        "Visible on Home, in Category Breakdown, and in the dedicated Savings Goals card in Statistics."
                    ),
                    tipText = "Editing or deleting the deposit on Home keeps the goal progress in sync.",
                    icon = Icons.Default.Savings,
                    accentColor = Color(0xFF10B981)
                ),
                SmartFeatureItem(
                    id = "roles_settle",
                    title = "Shared Books, Roles & Settle-Up",
                    summary = "Collaborate in real time with family, partners, or roommates with role-based permissions and fair debt settlement.",
                    highlights = listOf(
                        "Creator: full control, sharing & deletion. Editor: full expense, member, category, goal & reminder management. Registrar: fast expense/income logging.",
                        "Settle-Up calculates who owes whom and records repayment transfers in 1 tap."
                    ),
                    tipText = null,
                    icon = Icons.Default.SwapHoriz,
                    accentColor = Color(0xFF0D9488)
                ),
                SmartFeatureItem(
                    id = "export_cloud",
                    title = "Cloud Sync, Activity Log, Undo & Reports",
                    summary = "Your books stay backed up in the cloud across devices with full accountability and safe editing.",
                    highlights = listOf(
                        "Live Firestore synchronization with instant manual refresh from the top bar.",
                        "5-second Undo banner after deleting any expense or income.",
                        "Export clean Excel (CSV) and PDF summaries directly from Statistics."
                    ),
                    tipText = null,
                    icon = Icons.Default.CloudSync,
                    accentColor = Color(0xFFD97706)
                )
            )
        } else {
            listOf(
                SmartFeatureItem(
                    id = "voice",
                    title = "Φωνητική Καταγραφή στα Ελληνικά",
                    summary = "Πείτε το έξοδο με φυσικό λόγο και η εφαρμογή αναγνωρίζει αυτόματα ποσό, κατηγορία, πληρωτή και μέλη σε λίγα δευτερόλεπτα.",
                    highlights = listOf(
                        "Αναγνωρίζει ποσά με δεκαδικά, λέξεις-κλειδιά κατηγοριών, ποιος πλήρωσε και για ποιον.",
                        "Εμφανίζει πάντα κάρτα επιβεβαίωσης για έλεγχο ή τροποποίηση πριν την αποθήκευση."
                    ),
                    tipText = "Παράδειγμα: «24,50 ευρώ σούπερ μάρκετ πλήρωσε ο Μάριος» ή «12 ευρώ καφές για τη Μαρία»",
                    icon = Icons.Default.Mic,
                    accentColor = Color(0xFF7C3AED)
                ),
                SmartFeatureItem(
                    id = "receipt",
                    title = "Σάρωση Απόδειξης (Κάμερα OCR & QR ΑΑΔΕ)",
                    summary = "Σαρώστε έντυπες αποδείξεις με την κάμερα ή τον κωδικό QR ΑΑΔΕ για άμεση συμπλήρωση καταστήματος, ποσού και ημερομηνίας.",
                    highlights = listOf(
                        "Υποστηρίζει ανάγνωση QR ΑΑΔΕ αλλά και οπτική αναγνώριση κειμένου (OCR).",
                        "Έξυπνη εκμάθηση: θυμάται την κατηγορία που επιλέγετε για κάθε κατάστημα ή ΑΦΜ."
                    ),
                    tipText = null,
                    icon = Icons.Default.CameraAlt,
                    accentColor = Color(0xFF059669)
                ),
                SmartFeatureItem(
                    id = "bank_auto",
                    title = "Αυτόματος Εντοπισμός Τραπεζικών Συναλλαγών",
                    summary = "Εντοπίζει ειδοποιήσεις πληρωμών και καταθέσεων από ελληνικές και διεθνείς τράπεζες ή ψηφιακά πορτοφόλια (Google Pay, Revolut, Wise, PayPal κ.ά.) και τις ετοιμάζει για καταχώρηση με 1 πάτημα.",
                    highlights = listOf(
                        "Ξεχωρίζει αυτόματα τα Έξοδα από τα Έσοδα (πιστώσεις/καταθέσεις) και αγνοεί προσφορές, διαφημίσεις και κωδικούς OTP.",
                        "Τίποτα δεν αποθηκεύεται χωρίς την έγκρισή σας — επιλέγετε Καταχώρηση ή Αγνόηση."
                    ),
                    tipText = "Ενεργοποιείται ή απενεργοποιείται ανά πάσα στιγμή από Μενού → Ρυθμίσεις (δυνατότητα Finsay Pro).",
                    icon = Icons.Default.NotificationsNone,
                    accentColor = Color(0xFF0284C7)
                ),
                SmartFeatureItem(
                    id = "goals_sync",
                    title = "Στόχοι Συνδεδεμένοι με Έξοδα & Στατιστικά",
                    summary = "Τα χρήματα που αποταμιεύετε στους στόχους αποτυπώνονται άμεσα στις κινήσεις και στα οικονομικά σας στατιστικά.",
                    highlights = listOf(
                        "Κάθε κατάθεση σε στόχο δημιουργεί αυτόματα έξοδο στην κατηγορία «Αποταμίευση & Στόχοι».",
                        "Εμφανίζεται στην Αρχική, στην ανάλυση κατηγοριών και στην κάρτα Στόχων στα Στατιστικά."
                    ),
                    tipText = "Αν επεξεργαστείτε ή διαγράψετε την κατάθεση από την Αρχική, ενημερώνεται αυτόματα και ο στόχος.",
                    icon = Icons.Default.Savings,
                    accentColor = Color(0xFF10B981)
                ),
                SmartFeatureItem(
                    id = "roles_settle",
                    title = "Κοινά Εξοδολόγια, Ρόλοι & Εκκαθάριση (Settle-Up)",
                    summary = "Συνεργαστείτε σε πραγματικό χρόνο με οικογένεια ή συγκατοίκους, με σαφή δικαιώματα και αυτόματο υπολογισμό οφειλών.",
                    highlights = listOf(
                        "Δημιουργός: πλήρης διαχείριση, κοινοποίηση & διαγραφή. Συντάκτης: πλήρης διαχείριση εξόδων, μελών, κατηγοριών, στόχων & υπενθυμίσεων. Καταχωρητής: γρήγορη προσθήκη εξόδων/εσόδων.",
                        "Η Εκκαθάριση Οφειλών δείχνει ποιος χρωστάει σε ποιον και καταχωρεί την εξόφληση με 1 πάτημα."
                    ),
                    tipText = null,
                    icon = Icons.Default.SwapHoriz,
                    accentColor = Color(0xFF0D9488)
                ),
                SmartFeatureItem(
                    id = "export_cloud",
                    title = "Συγχρονισμός Cloud, Ιστορικό, Αναίρεση & Αναφορές",
                    summary = "Τα δεδομένα σας συγχρονίζονται με ασφάλεια στο cloud με πλήρη έλεγχο αλλαγών και εύκολη εξαγωγή.",
                    highlights = listOf(
                        "Ζωντανός συγχρονισμός μεταξύ συσκευών και χειροκίνητη ανανέωση από το επάνω σύμβολο cloud.",
                        "Μπάρα Αναίρεσης (5 δευτ.) μετά από διαγραφή εξόδου ή εσόδου για προστασία από λάθη.",
                        "Εξαγωγή αναλυτικών αναφορών σε Excel (CSV) και PDF μέσα από τα Στατιστικά."
                    ),
                    tipText = null,
                    icon = Icons.Default.CloudSync,
                    accentColor = Color(0xFFD97706)
                )
            )
        }
    }
}
