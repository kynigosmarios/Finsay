package com.example.util

import com.example.util.i18n.TranslationCatalog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Localization {
    enum class Language(
        val code: String,
        val firebaseCode: String,
        val bcp47Tag: String,
        val displayName: String,
        val flag: String
    ) {
        ENGLISH("EN", "en", "en-US", "English", "🇬🇧"),
        GREEK("EL", "el", "el-GR", "Ελληνικά", "🇬🇷"),
        SPANISH("ES", "es", "es-ES", "Español", "🇪🇸"),
        FRENCH("FR", "fr", "fr-FR", "Français", "🇫🇷"),
        GERMAN("DE", "de", "de-DE", "Deutsch", "🇩🇪"),
        ITALIAN("IT", "it", "it-IT", "Italiano", "🇮🇹"),
        PORTUGUESE("PT", "pt", "pt-PT", "Português", "🇵🇹"),
        DUTCH("NL", "nl", "nl-NL", "Nederlands", "🇳🇱"),
        POLISH("PL", "pl", "pl-PL", "Polski", "🇵🇱"),
        ROMANIAN("RO", "ro", "ro-RO", "Română", "🇷🇴");

        companion object {
            fun fromCode(code: String?): Language {
                val normalized = code?.trim()?.lowercase(Locale.ROOT)
                val primary = normalized?.substringBefore('-')?.substringBefore('_')
                return when (primary) {
                    "el", "gr", "greek", "ελληνικά" -> GREEK
                    "es", "spa", "spanish", "español" -> SPANISH
                    "fr", "fra", "french", "français" -> FRENCH
                    "de", "ger", "deu", "german", "deutsch" -> GERMAN
                    "it", "ita", "italian", "italiano" -> ITALIAN
                    "pt", "por", "portuguese", "português" -> PORTUGUESE
                    "nl", "nld", "dut", "dutch", "nederlands" -> DUTCH
                    "pl", "pol", "polish", "polski" -> POLISH
                    "ro", "ron", "rum", "romanian", "română" -> ROMANIAN
                    else -> ENGLISH
                }
            }
        }
    }

    fun tr(lang: Language, en: String, el: String): String =
        TranslationCatalog.lookup(lang, en, el)

    fun tr(langCode: String?, en: String, el: String): String =
        TranslationCatalog.lookup(Language.fromCode(langCode), en, el)

    fun trFormat(lang: Language, enTemplate: String, elTemplate: String, vararg args: Any): String =
        TranslationCatalog.format(lang, enTemplate, elTemplate, *args)

    fun trFormat(langCode: String?, enTemplate: String, elTemplate: String, vararg args: Any): String =
        TranslationCatalog.format(Language.fromCode(langCode), enTemplate, elTemplate, *args)

    fun getDateLocale(lang: Language): Locale = Locale.forLanguageTag(lang.bcp47Tag)

    fun formatMonthYear(timestamp: Long, lang: Language): String {
        val sdf = SimpleDateFormat("MMMM yyyy", getDateLocale(lang))
        return sdf.format(Date(timestamp)).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(getDateLocale(lang)) else it.toString()
        }
    }

    fun formatDayMonth(timestamp: Long, lang: Language): String {
        val sdf = SimpleDateFormat("d MMM", getDateLocale(lang))
        return sdf.format(Date(timestamp))
    }

    fun appName(lang: Language): String = "Finsay"
    fun appTagline(lang: Language): String = tr(lang, "Smart Expense Tracker", "Έξυπνο Εξοδολόγιο")
    fun tabExpenses(lang: Language): String = tr(lang, "Home", "Αρχική")
    fun tabAnalytics(lang: Language): String = tr(lang, "Statistics", "Στατιστικά")
    fun tabMembers(lang: Language): String = tr(lang, "Members", "Μέλη")
    fun tabCategories(lang: Language): String = tr(lang, "Categories", "Κατηγορίες")
    fun tabSavings(lang: Language): String = tr(lang, "Savings", "Αποταμίευση")
    fun today(lang: Language): String = tr(lang, "Today", "Σήμερα")
    fun yesterday(lang: Language): String = tr(lang, "Yesterday", "Χθες")
    fun close(lang: Language): String = tr(lang, "Close", "Κλείσιμο")
    fun cancel(lang: Language): String = tr(lang, "Cancel", "Ακύρωση")
    fun save(lang: Language): String = tr(lang, "Save", "Αποθήκευση")
    fun edit(lang: Language): String = tr(lang, "Edit", "Επεξεργασία")
    fun delete(lang: Language): String = tr(lang, "Delete", "Διαγραφή")
    fun confirm(lang: Language): String = tr(lang, "Confirm", "Επιβεβαίωση")
    fun clear(lang: Language): String = tr(lang, "Clear", "Καθαρισμός")
    fun view(lang: Language): String = tr(lang, "View", "Προβολή")
    fun add(lang: Language): String = tr(lang, "Add", "Προσθήκη")
    fun newAction(lang: Language): String = tr(lang, "New", "Νέα")
    fun select(lang: Language): String = tr(lang, "Select", "Επιλογή")
    fun copy(lang: Language): String = tr(lang, "Copy", "Αντιγραφή")
    fun share(lang: Language): String = tr(lang, "Share", "Κοινοποίηση")
    fun netBalanceLabel(lang: Language): String = tr(lang, "NET BALANCE", "ΚΑΘΑΡΟ ΥΠΟΛΟΙΠΟ")
    fun withinBudget(lang: Language): String = tr(lang, "Within budget", "Εντός προϋπολογισμού")
    fun overBudget(lang: Language): String = tr(lang, "Over budget", "Υπέρβαση προϋπολογισμού")
    fun income(lang: Language): String = tr(lang, "Income", "Έσοδα")
    fun expenses(lang: Language): String = tr(lang, "Expenses", "Έξοδα")
    fun singleExpense(lang: Language): String = tr(lang, "Expense", "Έξοδο")
    fun singleIncome(lang: Language): String = tr(lang, "Income", "Έσοδο")
    fun remainingBudgetPrefix(lang: Language): String = tr(lang, "Remaining Budget:", "Υπόλοιπο Budget:")
    fun ofBudget(lang: Language): String = tr(lang, "of budget", "του budget")
    fun commonHousehold(lang: Language): String = tr(lang, "Household Shared", "Κοινά Σπιτιού")
    fun individualPersonal(lang: Language): String = tr(lang, "Personal", "Ατομικά")
    fun searchExpensesPlaceholder(lang: Language): String = tr(lang, "Search expense, member, amount...", "Αναζήτηση εξόδου, μέλους, ποσού...")
    fun allFilter(lang: Language): String = tr(lang, "All", "Όλα")
    fun allCategoriesFilter(lang: Language): String = tr(lang, "All Categories", "Όλες οι Κατηγορίες")
    fun commonFamilyFilter(lang: Language): String = tr(lang, "Family Shared", "Κοινά Οικογένειας")
    fun selectMonthYear(lang: Language): String = tr(lang, "Select month & year", "Επιλογή μήνα και έτους")
    fun allMonthsCombined(lang: Language): String = tr(lang, "All combined (All months)", "Όλα μαζί (Όλοι οι μήνες)")
    fun currentMonth(lang: Language): String = tr(lang, "Current Month", "Τρέχων Μήνας")
    fun noExpensesFound(lang: Language): String = tr(lang, "No expenses found", "Δεν βρέθηκαν έξοδα")
    fun noExpensesForPeriod(lang: Language): String = tr(lang, "No recorded expenses for the selected period or member.", "Δεν υπάρχουν καταχωρημένα έξοδα για το επιλεγμένο διάστημα ή μέλος.")
    fun noIncomesFound(lang: Language): String = tr(lang, "No income found", "Δεν βρέθηκαν έσοδα")
    fun noIncomesForPeriod(lang: Language): String = tr(lang, "No recorded income for the selected period or member.", "Δεν υπάρχουν καταχωρημένα έσοδα για το επιλεγμένο διάστημα ή μέλος.")
    fun recordNewExpense(lang: Language): String = tr(lang, "Record New Expense", "Καταχώρηση Νέου Εξόδου")
    fun recordNewIncome(lang: Language): String = tr(lang, "Record New Income", "Καταχώρηση Νέου Εσόδου")
    fun voiceInput(lang: Language): String = tr(lang, "Voice Input", "Φωνητική Καταχώρηση")
    fun addExpense(lang: Language): String = tr(lang, "Manual Entry", "Χειροκίνητη Προσθήκη")
    fun addIncome(lang: Language): String = tr(lang, "Manual Income", "Χειροκίνητο Έσοδο")
    fun monthlyBudgetLimits(lang: Language): String = tr(lang, "Monthly Budget Limits", "Όρια Προϋπολογισμού Μήνα")
    fun allBudgetsHealthy(lang: Language): String = tr(lang, "All category budgets are healthy (<80%)", "Όλα τα όρια κατηγοριών είναι εντάξει (<80%)")
    fun statisticalAnalysis(lang: Language): String = tr(lang, "Statistical Analysis", "Στατιστική Ανάλυση")
    fun monthlyBalanceSheet(lang: Language): String = tr(lang, "Monthly balance sheet", "Μηνιαίος Ισολογισμός")
    fun balanceLabel(lang: Language): String = tr(lang, "Balance", "Υπόλοιπο")
    fun breakEven(lang: Language): String = tr(lang, "Break-even", "Ισοζύγιο")
    fun familyIncomeTitle(lang: Language): String = tr(lang, "Total Income", "Συνολικά Έσοδα")
    fun contributionPerMemberSources(lang: Language): String = tr(lang, "Contribution per member & sources", "Συνεισφορά ανά μέλος & πηγές")
    fun noIncomeRecordedYet(lang: Language): String = tr(lang, "No income recorded yet", "Δεν έχουν καταχωρηθεί ακόμα έσοδα")
    fun recordIncomeHint(lang: Language): String = tr(lang, "Record salaries, rent, bonuses etc. on the home screen with \"+ Income\".", "Καταχωρήστε μισθούς, ενοίκια, επιδοτήσεις κ.α. στην αρχική οθόνη με το κουμπί «+ Έσοδο».")
    fun splitModeIndividual(lang: Language): String = tr(lang, "Individual", "Ατομικά")
    fun splitModeParentsShared(lang: Language): String = tr(lang, "Parents + Shared", "Γονείς + Κοινά")
    fun splitModeEqualAll(lang: Language): String = tr(lang, "Equal to all", "Ισόποσα σε όλους")
    fun chartBars(lang: Language): String = tr(lang, "Bars", "Μπάρες")
    fun chartPie(lang: Language): String = tr(lang, "Pie", "Πίτα")
    fun excelButton(lang: Language): String = "Excel"
    fun analyticsHeader(lang: Language): String = tr(lang, "EXPENSE ANALYTICS", "ΑΝΑΛΥΣΗ ΕΞΟΔΩΝ")
    fun totalExpensesCard(lang: Language): String = tr(lang, "Total Expenses", "Συνολικά Έξοδα")
    fun averagePerDay(lang: Language): String = tr(lang, "Daily Average", "Ημερήσιος Μέσος Όρος")
    fun topCategory(lang: Language): String = tr(lang, "Top Category", "Κορυφαία Κατηγορία")
    fun distributionByCategory(lang: Language): String = tr(lang, "Distribution by Category", "Κατανομή ανά Κατηγορία")
    fun distributionByMember(lang: Language): String = tr(lang, "Distribution by Member", "Κατανομή ανά Μέλος")
    fun monthlyComparison(lang: Language): String = tr(lang, "Monthly Comparison", "Μηνιαία Σύγκριση")
    fun inviteMembersTitle(lang: Language): String = tr(lang, "Invite Users to Household", "Πρόσκληση Χρηστών στο Εξοδολόγιο")
    fun proPaidFeature(lang: Language): String = tr(lang, "🔒 Paid feature (Finsay Pro)", "🔒 Λειτουργία επί πληρωμή (Finsay Pro)")
    fun proActiveLiveSync(lang: Language): String = tr(lang, "Pro Active: Real-time expense sharing", "Pro Ενεργό: Μοιραστείτε τα έξοδα ζωντανά")
    fun inviteCardDesc(lang: Language): String = tr(lang, "Connect family, partner or roommates to the same household to view and log expenses in real-time.", "Συνδέστε οικογένεια, σύντροφο ή συγκατοίκους στο ίδιο εξοδολόγιο για ζωντανή καταγραφή εξόδων.")
    fun upgradeToProToInvite(lang: Language): String = tr(lang, "Upgrade to Pro to Invite", "Αναβάθμιση σε Pro για Πρόσκληση")
    fun inviteMemberToHousehold(lang: Language): String = tr(lang, "Invite Member to Household", "Πρόσκληση Μέλους στο Εξοδολόγιο")
    fun familyMembersHeader(lang: Language): String = tr(lang, "Family Members", "Μέλη Οικογένειας")
    fun membersVoiceKeywordsHint(lang: Language): String = tr(lang, "Set recognition keywords (e.g. \"dad\", \"mom\", \"kids\") so voice recognition knows who participates in each expense.", "Ορίστε λέξεις-κλειδιά (π.χ. «μπαμπάς», «μαμά», «παιδιά») ώστε η φωνητική καταγραφή να αναγνωρίζει ποιος συμμετέχει.")
    fun selectColor(lang: Language): String = tr(lang, "Choose color", "Επιλογή χρώματος")
    fun voiceKeywordsLabel(lang: Language): String = tr(lang, "Voice keywords: comma-separated", "Φωνητικές λέξεις: χωρισμένες με κόμμα")
    fun addMember(lang: Language): String = tr(lang, "Add Member", "Προσθήκη Μέλους")
    fun addMemberFab(lang: Language): String = tr(lang, "Add Member", "Προσθήκη Μέλους")
    fun editMemberTitle(lang: Language): String = tr(lang, "Edit Member", "Επεξεργασία Μέλους")
    fun memberNameLabel(lang: Language): String = tr(lang, "Member Name *", "Όνομα Μέλους *")
    fun memberNamePlaceholder(lang: Language): String = tr(lang, "e.g. George, Mom, Maria", "π.χ. Γιώργος, Μαμά, Μαρία")
    fun memberRoleLabel(lang: Language): String = tr(lang, "Role", "Ρόλος")
    fun memberKeywordsPlaceholder(lang: Language): String = tr(lang, "e.g. dad, father, george", "π.χ. μπαμπάς, πατέρας, γιώργος")
    fun deleteMemberConfirmTitle(lang: Language): String = tr(lang, "Delete Member?", "Διαγραφή Μέλους")
    fun categoriesAndVoice(lang: Language): String = tr(lang, "Categories & Voice", "Κατηγορίες & Φωνή")
    fun categoriesDesc(lang: Language): String = tr(lang, "Each category connects to voice keywords. When you speak them, the app automatically detects the category.", "Κάθε κατηγορία συνδέεται με φωνητικές λέξεις-κλειδιά για αυτόματη αναγνώριση κατά τη φωνητική καταγραφή.")
    fun addCategory(lang: Language): String = tr(lang, "Add Category", "Προσθήκη Κατηγορίας")
    fun addCategoryFab(lang: Language): String = tr(lang, "Add Category", "Προσθήκη Κατηγορίας")
    fun editCategoryTitle(lang: Language): String = tr(lang, "Edit Category", "Επεξεργασία Κατηγορίας")
    fun categoryNameLabel(lang: Language): String = tr(lang, "Category Name *", "Όνομα Κατηγορίας *")
    fun categoryNamePlaceholder(lang: Language): String = tr(lang, "e.g. Gym, Pets, Entertainment", "π.χ. Γυμναστήριο, Κατοικίδια, Χόμπι")
    fun monthlyBudgetLimitLabel(lang: Language, currencySymbol: String = CurrencyFormatter.currentSymbol): String =
        trFormat(lang, "Monthly Budget Limit (%1\$s)", "Μηνιαίο Όριο Budget (%1\$s)", currencySymbol)
    fun categoryIconLabel(lang: Language): String = tr(lang, "Choose Icon", "Επιλογή Εικονιδίου")
    fun categoryKeywordsLabel(lang: Language): String = tr(lang, "Voice keywords: comma-separated", "Φωνητικές λέξεις: χωρισμένες με κόμμα")
    fun categoryKeywordsPlaceholder(lang: Language): String = tr(lang, "e.g. supermarket, groceries, food", "π.χ. σουπερ μαρκετ, σκλαβενιτης, φαγητο")
    fun deleteCategoryConfirmTitle(lang: Language): String = tr(lang, "Delete Category?", "Διαγραφή Κατηγορίας")
    fun savingsTabTitle(lang: Language): String = tr(lang, "Savings Goals", "Στόχοι Αποταμίευσης")
    fun recurringTabTitle(lang: Language): String = tr(lang, "Standing Orders", "Πάγιες Εντολές")
    fun totalFamilySavings(lang: Language): String = tr(lang, "Total Savings", "Συνολική Αποταμίευση")
    fun ofTarget(lang: Language): String = tr(lang, "of", "από")
    fun ourGoals(lang: Language): String = tr(lang, "Our Goals", "Οι Στόχοι Μας")
    fun newGoal(lang: Language): String = tr(lang, "New Goal", "Νέος Στόχος")
    fun addGoal(lang: Language): String = tr(lang, "New Goal", "Νέος Στόχος")
    fun editGoalTitle(lang: Language): String = tr(lang, "Edit Savings Goal", "Επεξεργασία Στόχου")
    fun deadlinePrefix(lang: Language): String = tr(lang, "Deadline: ", "Ημερομηνία: ")
    fun goalAchievedCongratulations(lang: Language): String = tr(lang, "🎉 Congratulations! Goal achieved!", "🎉 Συγχαρητήρια! Ο στόχος επετεύχθη!")
    fun totalStandingOrders(lang: Language): String = tr(lang, "Total Standing Orders", "Σύνολο Πάγιων Εντολών")
    fun newStandingOrder(lang: Language): String = tr(lang, "New Order", "Νέα Πάγια")
    fun addRecurring(lang: Language): String = tr(lang, "New Order", "Νέα Πάγια")
    fun editStandingOrder(lang: Language): String = tr(lang, "Edit Standing Order", "Επεξεργασία Πάγιας Εντολής")
    fun perMonthSuffix(lang: Language): String = tr(lang, "/ month", "/ μήνα")
    fun executeNowAction(lang: Language): String = tr(lang, "Execute now", "Εκτέλεση τώρα")
    fun goalTitleLabel(lang: Language): String = tr(lang, "Goal Title", "Τίτλος στόχου")
    fun goalTitlePlaceholder(lang: Language): String = tr(lang, "e.g. Vacation, Car, Emergency", "π.χ. Διακοπές, Αυτοκίνητο, Έκτακτη ανάγκη")
    fun targetAmountLabel(lang: Language, currencySymbol: String = CurrencyFormatter.currentSymbol): String =
        trFormat(lang, "Target Amount (%1\$s)", "Ποσό στόχου (%1\$s)", currencySymbol)
    fun currentSavedLabel(lang: Language, currencySymbol: String = CurrencyFormatter.currentSymbol): String =
        trFormat(lang, "Already Saved (%1\$s)", "Αρχικό ποσό (%1\$s)", currencySymbol)
    fun recurringDescLabel(lang: Language): String = tr(lang, "Description", "Περιγραφή πάγιας")
    fun recurringDescPlaceholder(lang: Language): String = tr(lang, "e.g. Rent, Netflix, Electric bill", "π.χ. Ενοίκιο, Netflix, ΔΕΗ")
    fun dayOfMonthLabel(lang: Language): String = tr(lang, "Day of month (1-31)", "Ημέρα μήνα (1-31)")
    fun recordExpenseTitle(lang: Language): String = tr(lang, "Record Expense", "Καταγραφή Εξόδου")
    fun editExpenseTitle(lang: Language): String = tr(lang, "Edit Expense", "Επεξεργασία Εξόδου")
    fun expenseDescLabel(lang: Language): String = tr(lang, "Description", "Περιγραφή εξόδου")
    fun expenseDescPlaceholder(lang: Language): String = tr(lang, "e.g. Supermarket, Pharmacy, Coffee", "π.χ. Σκλαβενίτης, Φαρμακείο, Καφές")
    fun expenseAmountLabel(lang: Language, currencySymbol: String = CurrencyFormatter.currentSymbol): String =
        trFormat(lang, "Amount (%1\$s)", "Ποσό (%1\$s)", currencySymbol)
    fun memberWhoPaid(lang: Language): String = tr(lang, "Payer", "Πληρωτής")
    fun payersSectionTitle(lang: Language): String = tr(lang, "Who paid?", "Ποιος πλήρωσε")
    fun payersSubtitle(lang: Language): String = tr(lang, "Select one or more members who contributed", "Επιλέξτε ένα ή περισσότερα μέλη που συνεισέφεραν")
    fun payerSplitModeEqual(lang: Language): String = tr(lang, "Equal split", "Ισόποσα")
    fun payerSplitModeCustom(lang: Language): String = tr(lang, "Custom amounts", "Προσαρμοσμένα ποσά")
    fun payerFullyCovered(lang: Language): String = tr(lang, "Fully covered ✓", "Πλήρης κάλυψη ✓")
    fun paidSingle(lang: Language): String = tr(lang, "Paid:", "Πλήρωσε:")
    fun paidMultiple(lang: Language): String = tr(lang, "Paid by:", "Πλήρωσαν:")
    fun noMembersExpenseHint(lang: Language): String = tr(lang, "No members registered. The expense will be logged as Common.", "Δεν έχουν καταχωρηθεί μέλη. Το έξοδο θα καταχωρηθεί ως Κοινό.")
    fun categoryLabel(lang: Language): String = tr(lang, "Category", "Κατηγορία")
    fun date(lang: Language): String = tr(lang, "Date", "Ημερομηνία")
    fun dateLabel(lang: Language): String = tr(lang, "Target Date", "Προθεσμία")
    fun changeAction(lang: Language): String = tr(lang, "Change", "Αλλαγή")
    fun noteLabel(lang: Language): String = tr(lang, "Note (optional)", "Σημείωση (προαιρετικό)")
    fun notePlaceholder(lang: Language): String = tr(lang, "e.g. Paid by card", "π.χ. Πληρώθηκε με κάρτα")
    fun fillDescriptionError(lang: Language): String = tr(lang, "Please enter a description", "Παρακαλώ συμπληρώστε περιγραφή")
    fun fillAmountError(lang: Language): String = tr(lang, "Please enter a valid amount", "Παρακαλώ συμπληρώστε έγκυρο ποσό")
    fun recordIncomeTitle(lang: Language): String = tr(lang, "Record Income", "Καταγραφή Εσόδου")
    fun editIncomeTitle(lang: Language): String = tr(lang, "Edit Income", "Επεξεργασία Εσόδου")
    fun incomeDescLabel(lang: Language): String = tr(lang, "Income Description *", "Περιγραφή Εσόδου *")
    fun incomeDescPlaceholder(lang: Language): String = tr(lang, "e.g. October Salary, Rent", "π.χ. Μισθός Οκτωβρίου, Ενοίκιο")
    fun incomeAmountLabel(lang: Language, currencySymbol: String = CurrencyFormatter.currentSymbol): String =
        trFormat(lang, "Income Amount (%1\$s) *", "Ποσό Εσόδου (%1\$s) *", currencySymbol)
    fun memberWhoContributed(lang: Language): String = tr(lang, "MEMBER WHO CONTRIBUTED *", "ΜΕΛΟΣ ΠΟΥ ΣΥΝΕΙΣΕΦΕΡΕ *")
    fun noMembersIncomeHint(lang: Language): String = tr(lang, "No members registered. The income will be logged as Common.", "Δεν έχουν καταχωρηθεί μέλη. Το έσοδο θα καταχωρηθεί ως Κοινό.")
    fun incomeCategoryLabel(lang: Language): String = tr(lang, "INCOME CATEGORY", "ΚΑΤΗΓΟΡΙΑ ΕΣΟΔΟΥ")
    fun voiceLoggingTitle(lang: Language): String = tr(lang, "Voice Logging", "Φωνητική Καταγραφή")
    fun speakNowLabel(lang: Language): String = tr(lang, "Speak now...", "Μιλήστε τώρα...")
    fun listeningStatus(lang: Language): String = tr(lang, "Listening...", "Ακρόαση...")
    fun speechExamplesText(lang: Language): String = tr(
        lang,
        "Say for example:\n• \"Supermarket 45 euros dad\"\n• \"Pharmacy 12 euros mom\"\n• \"Coffee 3.50\"",
        "Πείτε για παράδειγμα:\n• «Σούπερ μάρκετ 45 ευρώ μπαμπάς»\n• «Φαρμακείο 12 ευρώ μαμά»\n• «Καφές 3.50»"
    )
    fun voiceConfirmationTitle(lang: Language): String = tr(lang, "Confirm Expense", "Επιβεβαίωση Εξόδου")
    fun voiceDetectedDetails(lang: Language): String = tr(lang, "Detected Details", "Αναγνωρισμένα Στοιχεία")
    fun tryAgainAction(lang: Language): String = tr(lang, "Try Again", "Δοκιμάστε Ξανά")
    fun quickActionTitle(lang: Language): String = tr(lang, "Quick Expense Logging", "Γρήγορη Καταγραφή Εξόδου")
    fun chooseMethodSubtitle(lang: Language): String = tr(lang, "Choose how you want to log your expense", "Επιλέξτε πώς θέλετε να καταχωρήσετε το έξοδο")
    fun voiceOptionTitle(lang: Language): String = tr(lang, "Voice Recognition", "Φωνητική Αναγνώριση")
    fun voiceOptionSubtitle(lang: Language): String = tr(lang, "Fast & easy with your voice", "Γρήγορα & απλά με τη φωνή σας")
    fun manualOptionTitle(lang: Language): String = tr(lang, "Manual Entry", "Χειροκίνητη Καταχώρηση")
    fun manualOptionSubtitle(lang: Language): String = tr(lang, "Type details in form", "Πληκτρολόγηση στη φόρμα")
    fun dontShowAgainCheckbox(lang: Language): String = tr(lang, "Don't show again on launch", "Να μην εμφανίζεται στην εκκίνηση")
    fun cloudSyncTitle(lang: Language): String = tr(lang, "Cloud Sync & Live Sharing", "Συγχρονισμός Cloud & Κοινή Χρήση")
    fun cloudSyncSubtitle(lang: Language): String = tr(lang, "Collaborate in real-time with family or partners across multiple devices.", "Συνεργαστείτε σε πραγματικό χρόνο με την οικογένεια ή συνεργάτες από πολλές συσκευές.")
    fun householdCodeLabel(lang: Language): String = tr(lang, "Household Code:", "Κωδικός Εξοδολογίου:")
    fun joinHouseholdTitle(lang: Language): String = tr(lang, "Join Existing Household", "Σύνδεση σε Υπάρχον Εξοδολόγιο")
    fun enterCodePlaceholder(lang: Language): String = tr(lang, "Enter household code", "Εισαγωγή κωδικού εξοδολογίου")
    fun joinAction(lang: Language): String = tr(lang, "Join", "Σύνδεση")
    fun exportReportTitle(lang: Language): String = tr(lang, "Export Report", "Εξαγωγή Αναφοράς")
    fun exportReportSubtitle(lang: Language): String = tr(lang, "In Excel / CSV format", "Σε μορφή Excel / CSV")
    fun periodLabel(lang: Language): String = tr(lang, "Period:", "Περίοδος:")
    fun expenseCountLabel(lang: Language): String = tr(lang, "Expense Count:", "Πλήθος Εξόδων:")
    fun totalAmountLabel(lang: Language): String = tr(lang, "Total Amount:", "Συνολικό Ποσό:")
    fun shareReportAction(lang: Language): String = tr(lang, "Share Report (Email / Viber / Drive)", "Κοινοποίηση (Email / Viber / Drive)")
    fun copyCsvAction(lang: Language): String = tr(lang, "Copy CSV text", "Αντιγραφή κειμένου CSV")
    fun csvCopiedMessage(lang: Language): String = tr(lang, "CSV report copied to clipboard!", "Η αναφορά CSV αντιγράφηκε στο πρόχειρο!")
    fun myProfile(lang: Language): String = tr(lang, "Profile", "Προφίλ")
    fun finsayUser(lang: Language): String = tr(lang, "Finsay User", "Χρήστης Finsay")
    fun freePlan(lang: Language): String = tr(lang, "Free Plan", "Δωρεάν Πλάνο")
    fun proSubscriber(lang: Language): String = tr(lang, "Pro Subscriber", "Συνδρομητής Pro")
    fun upgradeToPro(lang: Language): String = tr(lang, "Upgrade to Pro", "Αναβάθμιση σε Pro")
    fun transactionsLabel(lang: Language): String = tr(lang, "Transactions", "Συναλλαγές")
    fun totalExpensesLabel(lang: Language): String = tr(lang, "Total Expenses", "Σύνολο Εξόδων")
    fun signOutAccount(lang: Language): String = tr(lang, "Sign Out Account", "Αποσύνδεση Λογαριασμού")
    fun signOut(lang: Language): String = tr(lang, "Sign Out", "Αποσύνδεση")
    fun myExodologio(lang: Language): String = tr(lang, "My Household Book", "Το Εξοδολόγιό μου")
    fun activeExodologioHeader(lang: Language): String = tr(lang, "ACTIVE HOUSEHOLD", "ΕΝΕΡΓΟ ΕΞΟΔΟΛΟΓΙΟ")
    fun exodologioMembers(lang: Language): String = tr(lang, "Household Members", "Μέλη Εξοδολογίου")
    fun createNewExodologio(lang: Language): String = tr(lang, "Create New Household", "Δημιουργία Νέου Εξοδολογίου")
    fun separateExodologio(lang: Language): String = tr(lang, "Separate household", "Ξεχωριστό εξοδολόγιο")
    fun clearAllExpenses(lang: Language): String = tr(lang, "Clear all expenses", "Διαγραφή όλων των εξόδων")
    fun clearAllExpensesConfirmTitle(lang: Language): String = tr(lang, "Delete All Expenses?", "Διαγραφή Όλων των Εξόδων")
    fun clearAllExpensesConfirmMsg(lang: Language): String = tr(lang, "Are you sure you want to delete all expenses in this household? This cannot be undone.", "Είστε σίγουροι ότι θέλετε να διαγράψετε όλα τα έξοδα αυτού του εξοδολογίου; Η ενέργεια δεν αναιρείται.")
    fun menuTitle(lang: Language): String = tr(lang, "Menu Options", "Μενού Επιλογών")
    fun language(lang: Language): String = tr(lang, "Language", "Γλώσσα")
    fun languageSection(lang: Language): String = tr(lang, "APP LANGUAGE", "ΓΛΩΣΣΑ ΕΦΑΡΜΟΓΗΣ")
    fun appSettings(lang: Language): String = tr(lang, "App Settings", "Ρυθμίσεις Εφαρμογής")
    fun appSettingsSubtitle(lang: Language): String = tr(lang, "Theme, language & preferences", "Θέμα εμφάνισης, γλώσσα & προτιμήσεις")
    fun householdManager(lang: Language): String = tr(lang, "Manage Households", "Διαχείριση Εξοδολογίων")
    fun themeDisplay(lang: Language): String = tr(lang, "DISPLAY THEME", "ΘΕΜΑ ΕΜΦΑΝΙΣΗΣ")
    fun themeLight(lang: Language): String = tr(lang, "Light", "Φωτεινό")
    fun themeDark(lang: Language): String = tr(lang, "Dark", "Σκοτεινό")
    fun themeSystem(lang: Language): String = tr(lang, "System", "Σύστημα")
    fun visualDesign(lang: Language): String = tr(lang, "VISUAL DESIGN & FONTS", "ΣΧΕΔΙΑΣΜΟΣ & ΓΡΑΜΜΑΤΟΣΕΙΡΕΣ")
    fun styleModernFintech(lang: Language): String = tr(lang, "Modern FinTech", "Μοντέρνο FinTech")
    fun styleModernFintechDesc(lang: Language): String = tr(lang, "New rounded geometric cards, donut chart & Plus Jakarta Sans", "Νέες στρογγυλεμένες κάρτες, Plus Jakarta Sans & γραφιστικά")
    fun styleClassic(lang: Language): String = tr(lang, "Classic", "Κλασικό")
    fun styleClassicDesc(lang: Language): String = tr(lang, "Original clean Material 3 look", "Αρχικό κλασικό στυλ Material 3")
    fun preferencesSection(lang: Language): String = tr(lang, "PREFERENCES", "ΠΡΟΤΙΜΗΣΕΙΣ")
    fun quickActionLaunch(lang: Language): String = tr(lang, "Quick Action on Launch", "Γρήγορη επιλογή κατά την έναρξη")
    fun quickActionLaunchSubtitle(lang: Language): String = tr(lang, "Show Voice / Typing prompt dialog", "Εμφάνιση μενού φωνής/πληκτρολόγησης")
    fun dataManagement(lang: Language): String = tr(lang, "DATA MANAGEMENT", "ΔΙΑΧΕΙΡΙΣΗ ΔΕΔΟΜΕΝΩΝ")
    fun clearAllData(lang: Language): String = tr(lang, "Clear all household data", "Εκκαθάριση όλων των δεδομένων εξοδολογίου")
    fun clearAllDataSubtitle(lang: Language): String = tr(lang, "Clear active household records (kept safely in Firebase)", "Εκκαθάριση εξοδολογίου με ασφαλή διατήρηση στο Firebase")
    fun resetAction(lang: Language): String = tr(lang, "Reset", "Επαναφορά")
    fun analyticsStatisticalAnalysis(lang: Language): String = tr(lang, "Statistical Analysis", "Στατιστική Ανάλυση")
    fun familyMonthlyBalance(lang: Language): String = tr(lang, "Monthly balance", "Μηνιαίος Ισολογισμός")
    fun exportToExcel(lang: Language): String = tr(lang, "Export to Excel", "Εξαγωγή σε Excel")
    fun balanceWord(lang: Language): String = tr(lang, "Balance", "Ισοζύγιο")
    fun netBalanceTitle(lang: Language): String = tr(lang, "Balance", "Υπόλοιπο")
    fun familyIncome(lang: Language): String = tr(lang, "Total Incomes", "Συνολικά Έσοδα")
    fun contributionByMember(lang: Language): String = tr(lang, "Contribution by member & sources", "Συνεισφορά ανά μέλος & πηγές")
    fun noIncomesYet(lang: Language): String = tr(lang, "No income registered yet", "Δεν έχουν καταχωρηθεί ακόμα έσοδα")
    fun registerIncomeHint(lang: Language): String = tr(lang, "Register salaries, rents, allowances, etc. from the home screen with «+ Income».", "Καταχωρήστε μισθούς, ενοίκια, επιδοτήσεις κ.α. από την αρχική οθόνη με το κουμπί «+ Έσοδο».")
    fun commonOther(lang: Language): String = tr(lang, "Common / Other", "Κοινό / Άλλο")
    fun commonOtherIncome(lang: Language): String = tr(lang, "Common / Other Income", "Κοινό / Λοιπά Έσοδα")
    fun incomeSourcesByCategory(lang: Language): String = tr(lang, "Income Sources by Category", "Πηγές Εσόδων ανά Κατηγορία")
    fun expenseDistribution(lang: Language): String = tr(lang, "Expense Distribution", "Κατανομή Εξόδων")
    fun parents(lang: Language): String = tr(lang, "Parents", "Γονείς")
    fun children(lang: Language): String = tr(lang, "Children", "Παιδιά")
    fun breakdownByMember(lang: Language): String = tr(lang, "Breakdown by Member", "Καταμερισμός ανά Μέλος")
    fun individualSpendingSubtitle(lang: Language): String = tr(lang, "Personal expenses per family member", "Προσωπικά έξοδα ανά μέλος της οικογένειας")
    fun parentsSpendingSubtitle(lang: Language): String = tr(lang, "Parents expenses & shared household coverage", "Έξοδα γονέων & κάλυψη κοινών εξόδων σπιτιού")
    fun kidsSpendingSubtitle(lang: Language): String = tr(lang, "Expenses & activities for children", "Έξοδα & δραστηριότητες για τα παιδιά")
    fun individualMode(lang: Language): String = tr(lang, "Individual", "Ατομικά")
    fun parentsMode(lang: Language): String = tr(lang, "Parents", "Γονείς")
    fun kidsMode(lang: Language): String = tr(lang, "Children", "Παιδιά")
    fun budgetMonitoring(lang: Language): String = tr(lang, "Budget Monitoring", "Έλεγχος Budgets")
    fun exceededBudget(lang: Language): String = tr(lang, "Exceeded", "Υπέρβαση")
    fun warningBudget(lang: Language): String = tr(lang, "Warning", "Προσοχή")
    fun atBudget(lang: Language): String = tr(lang, "At budget limit (100%)", "Στο όριο (100%)")
    fun withinLimit(lang: Language): String = tr(lang, "Within limit", "Εντός ορίου")
    fun memberIncomeDeficitTitle(lang: Language): String = tr(lang, "Expenses Exceeded Declared Income", "Υπέρβαση Εσόδων Μέλους")
    fun memberIncomeDeficitDesc(lang: Language): String = tr(lang, "Member has paid more in expenses than their declared income for this period.", "Το μέλος έχει πληρώσει περισσότερα έξοδα από όσα έχει δηλώσει ως έσοδα για το επιλεγμένο διάστημα.")
    fun deficitLabel(lang: Language): String = tr(lang, "Deficit", "Έλλειμμα")
    fun expensesExceedIncomeTag(lang: Language): String = tr(lang, "Expenses > Income", "Έξοδα > Έσοδα")
    fun barChartView(lang: Language): String = tr(lang, "Bar Chart View", "Προβολή Μπάρας")
    fun pieChartView(lang: Language): String = tr(lang, "Pie Chart View", "Προβολή Πίτας")
    fun noExpensesRegistered(lang: Language): String = tr(lang, "No expenses registered", "Δεν υπάρχουν καταχωρημένα έξοδα")
    fun total(lang: Language): String = tr(lang, "Total", "Σύνολο")
    fun noKidsRegisteredHint(lang: Language): String = tr(lang, "No members with role «Child» registered.", "Δεν έχουν καταχωρηθεί μέλη με ρόλο «Παιδί».")
    fun noMembersInCategoryHint(lang: Language): String = tr(lang, "No members in this category.", "Δεν υπάρχουν μέλη στη συγκεκριμένη κατηγορία.")
    fun kidsExpensesSubtitle(lang: Language): String = tr(lang, "Child • Expenses & activities", "Παιδί • Έξοδα & δραστηριότητες")
    fun incomes(lang: Language): String = tr(lang, "Income", "Έσοδα")
    fun loadingExpenses(lang: Language): String = tr(lang, "Loading expenses...", "Φόρτωση εξόδων...")
    fun dailyAverageTitle(lang: Language): String = tr(lang, "Daily Average", "Μέσο Ημερήσιο")
    fun dailyAverageUnit(lang: Language): String = tr(lang, "/ day", "/ ημέρα")
    fun savingsRatioTitle(lang: Language): String = tr(lang, "Savings Rate", "Δείκτης Αποταμίευσης")
    fun peakDayTitle(lang: Language): String = tr(lang, "Peak Spending Day", "Υψηλότερη Ημέρα")
    fun noPeakDay(lang: Language): String = tr(lang, "No expenses", "Χωρίς έξοδα")
    fun fixedObligationsTitle(lang: Language): String = tr(lang, "Standing Orders", "Πάγιες Υποχρεώσεις")
    fun deleteAccountTitle(lang: Language): String = tr(lang, "Delete Account", "Διαγραφή Λογαριασμού")
    fun deleteAccountButton(lang: Language): String = tr(lang, "Delete Account & Data", "Διαγραφή Λογαριασμού & Δεδομένων")
    fun deleteAccountConfirmTitle(lang: Language): String = tr(lang, "Permanent Account Deletion?", "Οριστική Διαγραφή Λογαριασμού")
    fun deleteAccountConfirmMsg(lang: Language): String = tr(lang, "Are you sure you want to permanently delete your account and all associated financial data from the cloud? This action cannot be undone.", "Είστε σίγουροι ότι θέλετε να διαγράψετε οριστικά τον λογαριασμό σας και όλα τα οικονομικά δεδομένα από το cloud; Η ενέργεια δεν αναιρείται.")
    fun privacyAndLegal(lang: Language): String = tr(lang, "Privacy Policy & Terms", "Πολιτική Απορρήτου & Όροι")
    fun accountDeletedSuccess(lang: Language): String = tr(lang, "Your account and data were permanently deleted.", "Ο λογαριασμός και τα δεδομένα σας διαγράφηκαν οριστικά.")
    fun inviteViaEmailTitle(lang: Language): String = tr(lang, "Invite Member (Email & Link)", "Πρόσκληση Μέλους (Email & Link)")
    fun partnerEmailLabel(lang: Language): String = tr(lang, "Partner / Roommate Email", "Email Συνεργάτη / Συγκατοίκου")
    fun sendInviteAction(lang: Language): String = tr(lang, "Send Email Invitation", "Αποστολή Πρόσκλησης με Email")
    fun copyInviteLinkAction(lang: Language): String = tr(lang, "Copy Invite Link", "Αντιγραφή Συνδέσμου")
    fun inviteCopiedMsg(lang: Language): String = tr(lang, "Invite link copied to clipboard!", "Ο σύνδεσμος πρόσκλησης αντιγράφηκε!")

    fun isAllMembersLabel(rawLabel: String): Boolean {
        val clean = rawLabel.trim().lowercase(Locale.ROOT)
        return clean in setOf(
            "οικογένεια", "family", "κοινό οικογένειας", "family shared",
            "όλοι", "all", "όλα τα μέλη", "all members",
            "todos", "familia", "gastos familiares", "todos los miembros",
            "tous", "famille", "dépenses familiales", "tous les membres",
            "alle", "familie", "familienausgaben", "alle mitglieder",
            "tutti", "famiglia", "spese familiari", "tutti i membri",
            "família", "despesas familiares", "todos os membros",
            "gezin", "gedeeld gezin", "alle leden",
            "wszyscy", "rodzina", "wspólne rodzinne", "wszyscy członkowie",
            "toți", "toti", "cheltuieli de familie", "toți membrii"
        )
    }

    fun allMembersWord(lang: Language): String = tr(lang, "All", "Όλοι")
    fun sharedFamily(lang: Language): String = tr(lang, "Family Shared", "Κοινό Οικογένειας")
    fun familyShared(lang: Language): String = sharedFamily(lang)
    fun defaultHouseholdName(lang: Language): String = myExodologio(lang)

    fun translateCategory(name: String, lang: Language): String {
        val key = name.trim().lowercase(Locale.ROOT)
        val canonicalEnEl: Pair<String, String>? = when (key) {
            // Supermarket
            "σουπερ μαρκετ", "σούπερ μάρκετ", "supermarket", "supermercado", "supermarché", "supermarkt", "supermercato" ->
                "Supermarket" to "Σούπερ μάρκετ"
            // Food & Coffee
            "φαγητό & καφές", "φαγητο & καφες", "food & coffee", "comida y café", "repas & café",
            "essen & kaffee", "cibo e caffè", "comida e café", "eten & koffie", "jedzenie i kawa", "mâncare & cafea" ->
                "Food & Coffee" to "Φαγητό & Καφές"
            // Food
            "διατροφή", "φαγητό", "food", "comida", "alimentation", "essen", "cibo", "alimentação", "eten", "jedzenie", "mâncare" ->
                "Food" to "Διατροφή"
            // Bills & Home
            "λογαριασμοί & σπίτι", "λογαριασμοι & σπιτι", "bills & home", "facturas y hogar", "factures & maison",
            "rechnungen & wohnen", "bollette e casa", "contas e casa", "rekeningen & huis", "rachunki i dom", "facturi & casă" ->
                "Bills & Home" to "Λογαριασμοί & Σπίτι"
            // Bills
            "λογαριασμοί", "bills", "facturas", "factures", "rechnungen", "bollette", "contas", "rekeningen", "rachunki", "facturi" ->
                "Bills" to "Λογαριασμοί"
            // Rent
            "ενοίκιο", "ενοικιο", "rent", "alquiler", "loyer", "miete", "affitto", "renda", "huur", "czynsz", "chirie" ->
                "Rent" to "Ενοίκιο"
            // Transport & Fuel
            "μετακινήσεις & καύσιμα", "μετακινησεις & καυσιμα", "transport & fuel", "transporte y combustible",
            "transport & carburant", "transport & kraftstoff", "trasporti e carburante", "transporte e combustível",
            "vervoer & brandstof", "transport i paliwo", "transport & combustibil" ->
                "Transport & Fuel" to "Μετακινήσεις & Καύσιμα"
            // Transportation
            "μετακίνηση", "transport", "transportation", "βενζίνη", "transporte", "trasporti", "vervoer" ->
                "Transportation" to "Μετακίνηση"
            // Health & Pharmacy
            "υγεία & φαρμακείο", "υγεια & φαρμακειο", "health & pharmacy", "salud y farmacia",
            "santé & pharmacie", "gesundheit & apotheke", "salute e farmacia", "saúde e farmácia",
            "gezondheid & apotheek", "zdrowie i apteka", "sănătate & farmacie" ->
                "Health & Pharmacy" to "Υγεία & Φαρμακείο"
            // Health
            "υγεία", "health", "health & care", "φάρμακα", "salud", "santé", "gesundheit", "salute", "saúde", "gezondheid", "zdrowie", "sănătate" ->
                "Health & Care" to "Υγεία"
            // Entertainment
            "διασκέδαση & ψυχαγωγία", "διασκεδαση & ψυχαγωγια", "διασκέδαση", "entertainment", "ocio",
            "loisirs", "unterhaltung", "svago", "lazer", "vrije tijd", "rozrywka", "divertisment" ->
                "Entertainment" to "Διασκέδαση & Ψυχαγωγία"
            // Children
            "παιδιά", "kids", "children", "niños", "enfants", "kinder", "bambini", "crianças", "kinderen", "dzieci", "copii" ->
                "Children" to "Παιδιά"
            // Home
            "σπίτι", "home", "home & maintenance", "hogar", "maison", "wohnen", "casa", "huis", "dom", "casă" ->
                "Home & Maintenance" to "Σπίτι"
            // Clothing
            "ρούχα", "ένδυση", "clothing", "ropa", "vêtements", "kleidung", "abbigliamento", "roupa", "kleding", "odzież", "îmbrăcăminte" ->
                "Clothing" to "Ρούχα"
            // Education
            "εκπαίδευση", "education", "educación", "éducation", "bildung", "istruzione", "educação", "onderwijs", "edukacja", "educație" ->
                "Education" to "Εκπαίδευση"
            // Salary
            "μισθός", "μισθος", "salary", "salario", "salaire", "gehalt", "stipendio", "salário", "salaris", "wynagrodzenie", "salariu" ->
                "Salary" to "Μισθός"
            // Business / Freelance
            "επιχείρηση / freelance", "επιχειρηση / freelance", "business / freelance", "negocio / freelance",
            "entreprise / freelance", "geschäft / freelance", "attività / freelance", "negócio / freelance",
            "bedrijf / freelance", "firma / freelance", "afacere / freelance" ->
                "Business / Freelance" to "Επιχείρηση / Freelance"
            // Subsidy / Allowance
            "επιδότηση", "επιδοτηση", "subsidy", "subsidy / allowance", "subsidio / ayuda", "allocation / aide",
            "zuschuss / beihilfe", "sussidio / assegno", "subsídio / apoio", "toeslag / subsidie",
            "dotacja / zasiłek", "subvenție / alocație" ->
                "Subsidy / Allowance" to "Επιδότηση"
            // Bonus / Gift
            "bonus / δώρο", "bonus / δωρο", "bonus / gift", "bono / regalo", "prime / cadeau",
            "bonus / geschenk", "bonus / regalo", "bónus / presente", "bonus / cadeau", "bonus / prezent", "bonus / cadou" ->
                "Bonus / Gift" to "Bonus / Δώρο"
            // Investments
            "επενδύσεις", "επενδυσεις", "investments", "inversiones", "investissements", "investitionen",
            "investimenti", "investimentos", "investeringen", "inwestycje", "investiții" ->
                "Investments" to "Επενδύσεις"
            // Savings & Goals
            "αποταμίευση & στόχοι", "αποταμιευση & στοχοι", "savings & goals", "ahorro y metas",
            "épargne & objectifs", "sparen & ziele", "risparmio e obiettivi", "poupança e metas",
            "sparen & doelen", "oszczędności i cele", "economii & obiective" ->
                "Savings & Goals" to "Αποταμίευση & Στόχοι"
            // Other
            "άλλο", "αλλο", "λοιπά", "other", "otros", "autre", "sonstiges", "altro", "outros", "overig", "inne", "altele" ->
                "Other" to "Άλλο"
            else -> null
        }
        return if (canonicalEnEl != null) tr(lang, canonicalEnEl.first, canonicalEnEl.second) else name
    }
    fun translateCategory(lang: Language, name: String): String = translateCategory(name, lang)

    fun canonicalRoleKey(role: String): String {
        val r = role.trim().lowercase(Locale.ROOT)
        return when {
            r.contains("γον") || r.contains("parent") || r.contains("padre") || r.contains("madre") ||
                r.contains("eltern") || r.contains("genitor") || r == "pais" || r == "pai" || r == "mãe" || r.contains("pai/mãe") ||
                r.contains("ouder") || r.contains("rodzic") || r.contains("părin") || r.contains("parin") -> "PARENT"
            r.contains("παιδ") || r.contains("child") || r.contains("kid") || r.contains("niñ") || r.contains("hij") ||
                r.contains("enfant") || r.contains("kinder") || r == "kind" || r.contains("bambin") || r.contains("figli") ||
                r.contains("crianç") || r.contains("filh") || r.contains("dziec") || r.contains("copi") -> "CHILD"
            r.contains("συγκ") || r.contains("συνκ") || r.contains("room") || r.contains("compañero de piso") || r.contains("compañeros de piso") ||
                r.contains("coloc") || r.contains("mitbewohn") || r.contains("coinquilin") || r.contains("colega de casa") || r.contains("colegas de casa") ||
                r.contains("huisgenoot") || r.contains("huisgenoten") || r.contains("współlok") || r.contains("coleg de camer") || r.contains("colegi de camer") -> "ROOMMATE"
            r.contains("συντρ") || r.contains("σύντρ") || r.contains("partner") || r.contains("pareja") ||
                r.contains("partenaire") || r.contains("parceir") || r.contains("partener") -> "PARTNER"
            r.contains("συνεργ") || r.contains("συναδ") || r.contains("συνάδ") || r.contains("colleague") ||
                r.contains("colega") || r.contains("collègue") || r.contains("kolleg") || r.contains("collega") || r.contains("collegh") ||
                r.contains("współprac") || r.contains("coleg") -> "COLLEAGUE"
            r.contains("φιλ") || r.contains("φίλ") || r.contains("friend") || r.contains("amig") ||
                r.contains("ami") || r.contains("freund") || r.contains("amic") || r.contains("vriend") ||
                r.contains("przyjac") || r.contains("prieten") -> "FRIEND"
            r.contains("μονοπρ") || r.contains("individua") || r.contains("solo") || r.contains("individuel") ||
                r.contains("einzel") || r.contains("singol") || r.contains("individueel") || r.contains("individue") || r.contains("indywidual") -> "INDIVIDUAL"
            r.contains("αλλ") || r.contains("άλλ") || r.contains("other") || r.contains("otro") ||
                r.contains("autre") || r.contains("ander") || r.contains("sonstig") || r.contains("altr") || r.contains("outro") ||
                r.contains("overig") || r.contains("inn") || r.contains("alt") || r.contains("alț") -> "OTHER"
            else -> r
        }
    }

    fun translateRole(role: String, lang: Language): String {
        return when (canonicalRoleKey(role)) {
            "INDIVIDUAL" -> tr(lang, "Individual", "Μονοπρόσωπο")
            "PARENT" -> tr(lang, "Parent", "Γονέας")
            "CHILD" -> tr(lang, "Child", "Παιδί")
            "PARTNER" -> tr(lang, "Partner", "Σύντροφος")
            "ROOMMATE" -> tr(lang, "Roommate", "Συγκάτοικος")
            "COLLEAGUE" -> tr(lang, "Colleague", "Συνεργάτης")
            "FRIEND" -> tr(lang, "Friend", "Φίλος")
            "OTHER" -> tr(lang, "Other", "Άλλο")
            else -> role
        }
    }
    fun translateRole(lang: Language, role: String): String = translateRole(role, lang)

    fun pluralRoleName(role: String, lang: Language): String {
        return when (canonicalRoleKey(role)) {
            "PARENT" -> tr(lang, "Parents", "Γονείς")
            "CHILD" -> tr(lang, "Children", "Παιδιά")
            "ROOMMATE" -> tr(lang, "Roommates", "Συγκάτοικοι")
            "PARTNER" -> tr(lang, "Partners", "Σύντροφοι")
            "COLLEAGUE" -> tr(lang, "Colleagues", "Συνεργάτες")
            "FRIEND" -> tr(lang, "Friends", "Φίλοι")
            "INDIVIDUAL" -> tr(lang, "Individuals", "Μονοπρόσωπα")
            "OTHER" -> tr(lang, "Others", "Άλλοι")
            else -> role.trim()
        }
    }
    fun pluralRoleName(lang: Language, role: String): String = pluralRoleName(role, lang)

    private val knownPluralRoleLabels: Set<String> by lazy {
        val base = setOf(
            // Greek
            "γονείς", "παιδιά", "συγκάτοικοι", "σύντροφοι", "συνεργάτες", "συνάδελφοι", "φίλοι", "μονοπρόσωπα", "άλλοι",
            // English
            "parents", "children", "roommates", "partners", "colleagues", "friends", "individuals", "others",
            // Spanish
            "padres", "hijos", "niños", "compañeros de piso", "parejas", "colegas", "amigos", "individuales", "otros",
            // French
            "enfants", "colocataires", "partenaires", "collègues", "amis", "individuels", "autres",
            // German
            "eltern", "kinder", "mitbewohner", "partner", "kollegen", "freunde", "einzelpersonen", "andere",
            // Italian
            "genitori", "figli", "bambini", "coinquilini", "colleghi", "amici", "singoli", "altri",
            // Portuguese
            "pais", "filhos", "crianças", "colegas de casa", "parceiros", "individuais", "outros",
            // Dutch
            "ouders", "kinderen", "huisgenoten", "collega's", "vrienden", "individuen", "anderen",
            // Polish
            "rodzice", "dzieci", "współlokatorzy", "partnerzy", "współpracownicy", "przyjaciele", "indywidualni", "inni",
            // Romanian
            "părinți", "parinti", "copii", "colegi de cameră", "parteneri", "colegi", "prieteni", "individuali", "alții"
        )
        base
    }

    fun translateCollectiveRoleLabel(label: String, lang: Language): String? {
        val clean = label.trim()
        if (clean.isBlank()) return null
        return if (clean.lowercase(Locale.ROOT) in knownPluralRoleLabels) {
            pluralRoleName(clean, lang)
        } else {
            null
        }
    }

    fun isPluralRoleDisplay(text: String): Boolean {
        return translateCollectiveRoleLabel(text, Language.GREEK) != null
    }

    fun resolveMemberGroupDisplay(
        selected: List<com.example.data.entity.FamilyMember>,
        allMembers: List<com.example.data.entity.FamilyMember>,
        lang: Language
    ): String {
        val distinctSelected = selected.distinctBy { it.id }
        if (distinctSelected.isEmpty()) return ""
        if (distinctSelected.size == 1) return distinctSelected[0].name

        if (allMembers.isNotEmpty()) {
            val selectedIds = distinctSelected.map { it.id }.toSet()
            val allIds = allMembers.map { it.id }.toSet()
            if (selectedIds.containsAll(allIds)) {
                return allMembersWord(lang)
            }

            val firstRoleKey = canonicalRoleKey(distinctSelected.first().role)
            if (firstRoleKey.isNotBlank() && distinctSelected.all { canonicalRoleKey(it.role) == firstRoleKey }) {
                val allWithSameRoleIds = allMembers
                    .filter { canonicalRoleKey(it.role) == firstRoleKey }
                    .map { it.id }
                    .toSet()
                if (allWithSameRoleIds.size >= 2 && selectedIds == allWithSameRoleIds) {
                    return pluralRoleName(distinctSelected.first().role, lang)
                }
            }
        }

        return distinctSelected.joinToString(" & ") { it.name }
    }

    private val knownDefaultHouseholdNames = setOf(
        "το εξοδολόγιό μου", "my household book", "my household",
        "mi libro de gastos", "mon carnet de dépenses", "mein haushaltsbuch",
        "il mio registro spese", "o meu livro de despesas", "mijn huishoudboek",
        "mój budżet domowy", "registrul meu de cheltuieli"
    )

    fun translateHouseholdName(name: String, lang: Language): String {
        val lower = name.trim().lowercase(Locale.ROOT)
        if (lower in knownDefaultHouseholdNames) {
            return myExodologio(lang)
        }
        return when {
            lower == "οικογένεια" || lower == "family" -> tr(lang, "Family", "Οικογένεια")
            lower == "επιχείρηση" || lower == "business" -> tr(lang, "Business", "Επιχείρηση")
            else -> name
        }
    }
    fun translateHouseholdName(lang: Language, name: String): String = translateHouseholdName(name, lang)

    fun categoriesExceededLimit(lang: Language, count: Int): String =
        trFormat(lang, "%1\$d categories exceeded budget!", "%1\$d κατηγορίες ξεπέρασαν το όριο!", count)
    fun categoriesExceededLimit(count: Int, lang: Language): String = categoriesExceededLimit(lang, count)

    fun categoriesApproachingLimit(lang: Language, count: Int): String =
        trFormat(lang, "%1\$d categories approaching budget limit", "%1\$d κατηγορίες πλησιάζουν το όριο", count)
    fun categoriesApproachingLimit(count: Int, lang: Language): String = categoriesApproachingLimit(lang, count)

    fun activeMembersCount(lang: Language, count: Int): String =
        trFormat(lang, "%1\$d active members", "%1\$d ενεργά μέλη", count)
    fun activeMembersCount(count: Int, lang: Language): String = activeMembersCount(lang, count)

    fun activeHouseholdCount(lang: Language, count: Int): String =
        trFormat(lang, "%1\$d household", "%1\$d εξοδολόγιο", count)
    fun activeHouseholdCount(count: Int, lang: Language): String = activeHouseholdCount(lang, count)

    fun payerRemaining(lang: Language, formattedAmount: String): String =
        trFormat(lang, "Remaining to assign: %1\$s", "Απομένουν προς κατανομή: %1\$s", formattedAmount)
    fun payerRemaining(formattedAmount: String, lang: Language): String = payerRemaining(lang, formattedAmount)

    fun payerQuickBalance(lang: Language, formattedAmount: String): String =
        trFormat(lang, "Balance: %1\$s", "Υπόλοιπο: %1\$s", formattedAmount)
    fun payerQuickBalance(formattedAmount: String, lang: Language): String = payerQuickBalance(lang, formattedAmount)
    fun payerQuickBalance(lang: Language, memberName: String, formattedAmount: String): String =
        trFormat(lang, "+%1\$s to %2\$s", "+%1\$s στον/στην %2\$s", formattedAmount, memberName)

    fun payerExceeded(lang: Language, formattedAmount: String): String =
        trFormat(lang, "Exceeded by: %1\$s", "Υπέρβαση κατά: %1\$s", formattedAmount)
    fun payerExceeded(formattedAmount: String, lang: Language): String = payerExceeded(lang, formattedAmount)

    fun entriesCount(count: Int, lang: Language): String =
        trFormat(lang, "%1\$d entries", "%1\$d εγγραφές", count)
    fun entriesCount(lang: Language, count: Int): String = entriesCount(count, lang)

    fun transactionsCount(count: Int, lang: Language): String =
        trFormat(lang, "%1\$d transactions", "%1\$d συναλλαγές", count)
    fun transactionsCount(lang: Language, count: Int): String = transactionsCount(count, lang)

    fun savingsRateLabel(rate: Int, lang: Language): String =
        trFormat(lang, "Savings: %1\$d%%", "Αποταμίευση %1\$d%%", rate)
    fun savingsRateLabel(lang: Language, rate: Int): String = savingsRateLabel(rate, lang)

    fun percentOfTotalIncome(percent: Int, lang: Language): String =
        trFormat(lang, "%1\$d%% of total income", "%1\$d%% του συνολικού εισοδήματος", percent)
    fun percentOfTotalIncome(percent: Double, lang: Language): String = percentOfTotalIncome(percent.toInt(), lang)
    fun percentOfTotalIncome(lang: Language, percent: Int): String = percentOfTotalIncome(percent, lang)

    fun unassignedMemberIncome(percent: Int, lang: Language): String =
        trFormat(lang, "%1\$d%% unassigned", "%1\$d%% μη κατανεμημένο", percent)
    fun unassignedMemberIncome(lang: Language, percent: Int): String = unassignedMemberIncome(percent, lang)

    fun categoriesCount(count: Int, lang: Language): String =
        trFormat(lang, "%1\$d categories", "%1\$d κατηγορίες", count)
    fun categoriesCount(lang: Language, count: Int): String = categoriesCount(count, lang)

    fun everyDayOfMonth(lang: Language, day: Int): String =
        trFormat(lang, "Every %1\$d of month", "Κάθε %1\$d του μήνα", day)
    fun everyDayOfMonth(day: Int, lang: Language): String = everyDayOfMonth(lang, day)

    fun householdRoleLabel(role: com.example.data.model.HouseholdRole, lang: Language): String = when (role) {
        com.example.data.model.HouseholdRole.CREATOR -> tr(lang, "Creator", "Δημιουργός")
        com.example.data.model.HouseholdRole.EDITOR -> tr(lang, "Editor", "Συντάκτης")
        com.example.data.model.HouseholdRole.REGISTAR -> tr(lang, "Registar", "Καταχωρητής")
    }

    fun findAction(lang: Language): String = tr(lang, "Find", "Εύρεση")
    fun userNotFound(lang: Language): String = tr(lang, "Not found", "Δεν βρέθηκε")
    fun userEmailPlaceholder(lang: Language): String = tr(lang, "User email", "Email χρήστη")
    fun alreadyAddedMsg(lang: Language): String = tr(lang, "Already added", "Ήδη προστέθηκε")
    fun sharedUsersTitle(lang: Language, count: Int): String =
        trFormat(lang, "Shared users (%1\$d)", "Χρήστες με πρόσβαση (%1\$d)", count)
    fun removeUserAction(lang: Language): String = tr(lang, "Remove user", "Αφαίρεση χρήστη")
    fun leaveHouseholdAction(lang: Language): String = tr(lang, "Leave", "Αποχώρηση")
    fun myHouseholdsHeader(lang: Language, count: Int): String =
        trFormat(lang, "MY HOUSEHOLDS (%1\$d)", "ΤΑ ΕΞΟΔΟΛΟΓΙΑ ΜΟΥ (%1\$d)", count)
    fun loadingEmail(lang: Language): String = tr(lang, "Loading email...", "Φόρτωση email...")
    fun registeredUserLabel(lang: Language): String = tr(lang, "Registered user", "Εγγεγραμμένος χρήστης")

    // 1.1 Default Payer per Connected User
    fun linkedAppUserLabel(lang: Language): String = tr(lang, "Linked app user (default payer)", "Συνδεδεμένος χρήστης (προεπ. πληρωτής)")
    fun noLinkedUserLabel(lang: Language): String = tr(lang, "None", "Κανένας")
    fun defaultPayerShort(lang: Language): String = tr(lang, "Default payer", "Προεπ. πληρωτής")
    fun selectDefaultPayerLabel(lang: Language): String = tr(lang, "Not set", "Μη ορισμένος")

    // 1.2 Undo Deletion Banner
    fun undoAction(lang: Language): String = tr(lang, "Undo", "Αναίρεση")
    fun deletedPrefix(lang: Language): String = tr(lang, "Deleted:", "Διαγράφηκε:")

    // 1.3 Settle Up Now
    fun settleNowAction(lang: Language): String = tr(lang, "Settle now", "Εξόφληση τώρα")
    fun settleConfirmTitle(lang: Language): String = tr(lang, "Confirm Settlement", "Επιβεβαίωση Εξόφλησης")
    fun settleConfirmMessage(lang: Language, from: String, to: String, formattedAmount: String): String =
        trFormat(
            lang,
            "Record a settlement of %1\$s from %2\$s to %3\$s?",
            "Να καταχωρηθεί εξόφληση ποσού %1\$s από %2\$s προς %3\$s;",
            formattedAmount,
            from,
            to
        )

    // 1.4 Smart Category Learning & 1.5 Frequencies
    fun learnedCategoryHint(lang: Language): String = tr(lang, "Auto-suggested category", "Έξυπνη πρόταση κατηγορίας")
    fun frequencyFieldLabel(lang: Language): String = tr(lang, "Frequency", "Συχνότητα")
    fun anchorMonthLabel(lang: Language): String = tr(lang, "Start / Anchor Month", "Μήνας αναφοράς")
    fun weekdayLabel(lang: Language): String = tr(lang, "Day of week", "Ημέρα εβδομάδας")

    // 2.1 Sync Status Indicator
    fun syncingStatus(lang: Language): String = tr(lang, "Syncing", "Συγχρονισμός")
    fun syncedStatus(lang: Language): String = tr(lang, "Synced", "Ενημερωμένο")
    fun offlineStatus(lang: Language): String = tr(lang, "Offline", "Εκτός σύνδεσης")

    // 3.1 Household Activity History
    fun activityHistoryTitle(lang: Language): String = tr(lang, "Activity History", "Ιστορικό Κινήσεων")
    fun noActivityYet(lang: Language): String = tr(lang, "No recorded activity yet", "Δεν υπάρχει ακόμη ιστορικό κινήσεων")

    // 3.2 Month-over-Month Comparison & Forecast
    fun monthlyComparisonTitle(lang: Language): String = tr(lang, "Month-over-Month & Forecast", "Σύγκριση & Πρόβλεψη Μήνα")
    fun vsPreviousMonth(lang: Language): String = tr(lang, "vs prev. month", "έναντι προηγ. μήνα")
    fun forecastEndOfMonthTitle(lang: Language): String = tr(lang, "End-of-Month Projection", "Πρόβλεψη Τέλους Μήνα")
    fun projectedTotalLabel(lang: Language): String = tr(lang, "Projected expenses", "Εκτίμηση εξόδων")
    fun remainingScheduledLabel(lang: Language): String = tr(lang, "Upcoming standing orders", "Υπόλοιπες πάγιες εντολές")
    fun projectedSurplusLabel(lang: Language): String = tr(lang, "Projected surplus", "Εκτιμώμενο πλεόνασμα")
    fun projectedOverBudgetLabel(lang: Language): String = tr(lang, "Projected deficit", "Εκτιμώμενο έλλειμμα")
}
