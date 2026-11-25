package com.example.data.repository

import android.content.Context
import com.example.data.dao.ExpenseDao
import com.example.data.entity.Expense
import com.example.data.entity.ExpenseCategory
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.data.entity.RecurringExpense
import com.example.data.entity.SavingsGoal
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val context: Context? = null
) {

    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val allIncomes: Flow<List<Income>> = expenseDao.getAllIncomes()
    val allMembers: Flow<List<FamilyMember>> = expenseDao.getAllMembers()
    val allCategories: Flow<List<ExpenseCategory>> = expenseDao.getAllCategories()
    val allRecurringExpenses: Flow<List<RecurringExpense>> = expenseDao.getAllRecurringExpenses()
    val allSavingsGoals: Flow<List<SavingsGoal>> = expenseDao.getAllSavingsGoals()

    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)
    suspend fun insertExpenses(expenses: List<Expense>) = expenseDao.insertExpenses(expenses)
    suspend fun updateExpense(expense: Expense): Int = expenseDao.updateExpense(expense)
    suspend fun saveOrUpdateExpense(expense: Expense): Long = expenseDao.saveOrUpdateExpense(expense)
    suspend fun replaceAllExpenses(expenses: List<Expense>) {
        val existing = expenseDao.getAllExpensesList()
        if (existing == expenses) return
        expenseDao.replaceAllExpensesTransaction(expenses)
    }
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)
    suspend fun deleteExpenseById(id: Long) = expenseDao.deleteExpenseById(id)
    suspend fun deleteAllExpenses() = expenseDao.deleteAllExpenses()

    suspend fun insertIncome(income: Income): Long = expenseDao.insertIncome(income)
    suspend fun insertIncomes(incomes: List<Income>) = expenseDao.insertIncomes(incomes)
    suspend fun updateIncome(income: Income) = expenseDao.updateIncome(income)
    suspend fun deleteIncome(income: Income) = expenseDao.deleteIncome(income)
    suspend fun deleteIncomeById(id: Long) = expenseDao.deleteIncomeById(id)
    suspend fun deleteAllIncomes() = expenseDao.deleteAllIncomes()
    suspend fun replaceAllIncomes(incomes: List<Income>) {
        expenseDao.replaceAllIncomesTransaction(incomes)
    }

    suspend fun insertMember(member: FamilyMember): Long = expenseDao.insertMember(member)
    suspend fun insertMembers(members: List<FamilyMember>) = expenseDao.insertMembers(members)
    suspend fun replaceAllMembers(members: List<FamilyMember>) {
        val existing = expenseDao.getAllMembersList()
        if (existing == members) return
        expenseDao.replaceAllMembersTransaction(members)
    }
    suspend fun updateMember(member: FamilyMember) = expenseDao.updateMember(member)
    suspend fun updateMembers(members: List<FamilyMember>) = expenseDao.updateMembers(members)
    suspend fun deleteMember(member: FamilyMember) = expenseDao.deleteMember(member)
    suspend fun deleteAllMembers() = expenseDao.deleteAllMembers()

    suspend fun insertCategory(category: ExpenseCategory): Long = expenseDao.insertCategory(category)
    suspend fun insertCategories(categories: List<ExpenseCategory>) = expenseDao.insertCategories(categories)
    suspend fun replaceAllCategories(categories: List<ExpenseCategory>) {
        val existing = expenseDao.getAllCategoriesList()
        if (existing == categories) return
        expenseDao.replaceAllCategoriesTransaction(categories)
    }
    suspend fun updateCategory(category: ExpenseCategory) = expenseDao.updateCategory(category)
    suspend fun updateCategories(categories: List<ExpenseCategory>) = expenseDao.updateCategories(categories)
    suspend fun deleteCategory(category: ExpenseCategory) = expenseDao.deleteCategory(category)
    suspend fun deleteAllCategories() = expenseDao.deleteAllCategories()

    suspend fun insertRecurringExpense(recurring: RecurringExpense): Long = expenseDao.insertRecurringExpense(recurring)
    suspend fun insertRecurringExpenses(recurringList: List<RecurringExpense>) = expenseDao.insertRecurringExpenses(recurringList)
    suspend fun replaceAllRecurringExpenses(recurringList: List<RecurringExpense>) {
        expenseDao.deleteAllRecurringExpenses()
        if (recurringList.isNotEmpty()) {
            expenseDao.insertRecurringExpenses(recurringList)
        }
    }
    suspend fun updateRecurringExpense(recurring: RecurringExpense) = expenseDao.updateRecurringExpense(recurring)
    suspend fun deleteRecurringExpense(recurring: RecurringExpense) = expenseDao.deleteRecurringExpense(recurring)
    suspend fun deleteAllRecurringExpenses() = expenseDao.deleteAllRecurringExpenses()
    suspend fun getRecurringCount(): Int = expenseDao.getRecurringCount()

    suspend fun insertSavingsGoal(goal: SavingsGoal): Long = expenseDao.insertSavingsGoal(goal)
    suspend fun insertSavingsGoals(goals: List<SavingsGoal>) = expenseDao.insertSavingsGoals(goals)
    suspend fun replaceAllSavingsGoals(goals: List<SavingsGoal>) {
        val existing = expenseDao.getAllSavingsGoalsList()
        if (existing == goals) return
        expenseDao.replaceAllSavingsGoalsTransaction(goals)
    }
    suspend fun updateSavingsGoal(goal: SavingsGoal) = expenseDao.updateSavingsGoal(goal)
    suspend fun deleteSavingsGoal(goal: SavingsGoal) = expenseDao.deleteSavingsGoal(goal)
    suspend fun deleteAllSavingsGoals() = expenseDao.deleteAllSavingsGoals()
    suspend fun getSavingsGoalCount(): Int = expenseDao.getSavingsGoalCount()
    suspend fun getMemberCount(): Int = expenseDao.getMemberCount()
    suspend fun getCategoryCount(): Int = expenseDao.getCategoryCount()

    // One-shot list getters for sync verification and recovery
    suspend fun getAllCategoriesList(): List<ExpenseCategory> = expenseDao.getAllCategoriesList()
    suspend fun getAllMembersList(): List<FamilyMember> = expenseDao.getAllMembersList()
    suspend fun getAllSavingsGoalsList(): List<SavingsGoal> = expenseDao.getAllSavingsGoalsList()
    suspend fun getAllRecurringExpensesList(): List<RecurringExpense> = expenseDao.getAllRecurringExpensesList()
    suspend fun getAllExpensesList(): List<Expense> = expenseDao.getAllExpensesList()
    suspend fun getAllIncomesList(): List<Income> = expenseDao.getAllIncomesList()

    /**
     * ZERO DATA LOSS GUARANTEE:
     * This method clears the local Room cache on the device ONLY.
     * It MUST NEVER be invoked upon app start, initialization, or registration.
     * All persistent cloud records in Firebase Firestore (households & users collections)
     * remain 100% intact and are automatically repopulated upon login.
     */
    suspend fun clearAllData() {
        expenseDao.deleteAllExpenses()
        expenseDao.deleteAllIncomes()
        expenseDao.deleteAllMembers()
        expenseDao.deleteAllCategories()
        expenseDao.deleteAllRecurringExpenses()
        expenseDao.deleteAllSavingsGoals()
    }

    suspend fun seedInitialDataIfEmpty() {
        val existingCategories = expenseDao.getCategoryCount()
        if (existingCategories == 0) {
            seedStandardCategories()
        }
    }

    suspend fun seedStandardCategories(
        maxTotalExpenseCategories: Int? = null,
        isEnglish: Boolean = false
    ) {
        val existing = expenseDao.getAllCategoriesList().filter { !it.isIncomeCategory }
        val maxSort = existing.maxOfOrNull { it.sortOrder } ?: -1
        val standardCategories = if (isEnglish) {
            listOf(
                ExpenseCategory(
                    id = 0L,
                    name = "Supermarket",
                    iconKey = "cart",
                    monthlyBudget = 0.0,
                    colorHex = 0xFF059669,
                    voiceKeywords = "supermarket, groceries, grocery, market, lidl, shopping, σούπερ μάρκετ, μάρκετ, ψώνια",
                    sortOrder = maxSort + 1,
                    categoryType = "EXPENSE"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Food & Coffee",
                    iconKey = "food",
                    monthlyBudget = 0.0,
                    colorHex = 0xFFEF4444,
                    voiceKeywords = "food, coffee, cafe, delivery, restaurant, pizza, lunch, dinner, φαγητό, καφές, εστιατόριο",
                    sortOrder = maxSort + 2,
                    categoryType = "EXPENSE"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Bills & Home",
                    iconKey = "house",
                    monthlyBudget = 0.0,
                    colorHex = 0xFF0D9488,
                    voiceKeywords = "bill, bills, electricity, water, rent, internet, phone, home, utilities, λογαριασμός, ρεύμα, ενοίκιο",
                    sortOrder = maxSort + 3,
                    categoryType = "EXPENSE"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Transport & Fuel",
                    iconKey = "gas_station",
                    monthlyBudget = 0.0,
                    colorHex = 0xFFEA580C,
                    voiceKeywords = "fuel, gas, petrol, tolls, parking, bus, metro, taxi, uber, transport, βενζίνη, καύσιμα, ταξί",
                    sortOrder = maxSort + 4,
                    categoryType = "EXPENSE"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Health & Pharmacy",
                    iconKey = "medical",
                    monthlyBudget = 0.0,
                    colorHex = 0xFFEC4899,
                    voiceKeywords = "doctor, pharmacy, medicine, health, hospital, dentist, γιατρός, φαρμακείο, φάρμακα",
                    sortOrder = maxSort + 5,
                    categoryType = "EXPENSE"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Entertainment",
                    iconKey = "entertainment",
                    monthlyBudget = 0.0,
                    colorHex = 0xFF8B5CF6,
                    voiceKeywords = "entertainment, cinema, movie, drinks, bar, concert, netflix, διασκέδαση, σινεμά, ποτό",
                    sortOrder = maxSort + 6,
                    categoryType = "EXPENSE"
                )
            )
        } else {
            listOf(
                ExpenseCategory(
                    id = 0L,
                    name = "Σούπερ μάρκετ",
                    iconKey = "cart",
                    monthlyBudget = 0.0,
                    colorHex = 0xFF059669,
                    voiceKeywords = "σούπερ μάρκετ, σουπερμάρκετ, σκλαβενίτης, βασιλόπουλος, lidl, μάρκετ, τρόφιμα, ψώνια, μανάβικο, κρεοπωλείο, supermarket, groceries",
                    sortOrder = maxSort + 1,
                    categoryType = "EXPENSE"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Φαγητό & Καφές",
                    iconKey = "food",
                    monthlyBudget = 0.0,
                    colorHex = 0xFFEF4444,
                    voiceKeywords = "φαγητό, καφές, καφέδες, ντελίβερι, σουβλάκια, πίτσα, delivery, efood, wolt, ταβέρνα, εστιατόριο, food, coffee",
                    sortOrder = maxSort + 2,
                    categoryType = "EXPENSE"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Λογαριασμοί & Σπίτι",
                    iconKey = "house",
                    monthlyBudget = 0.0,
                    colorHex = 0xFF0D9488,
                    voiceKeywords = "ενοίκιο, ρεύμα, δεη, αέριο, κοινόχρηστα, ίντερνετ, νερό, λογαριασμός, bills, rent, utilities",
                    sortOrder = maxSort + 3,
                    categoryType = "EXPENSE"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Μετακινήσεις & Καύσιμα",
                    iconKey = "gas_station",
                    monthlyBudget = 0.0,
                    colorHex = 0xFFEA580C,
                    voiceKeywords = "βενζίνη, καύσιμα, βενζινάδικο, διόδια, πάρκινγκ, λεωφορείο, μετρό, ταξί, fuel, gas, transport",
                    sortOrder = maxSort + 4,
                    categoryType = "EXPENSE"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Υγεία & Φαρμακείο",
                    iconKey = "medical",
                    monthlyBudget = 0.0,
                    colorHex = 0xFFEC4899,
                    voiceKeywords = "γιατρός, φαρμακείο, φάρμακα, εξετάσεις, οδοντίατρος, υγεία, health, pharmacy, doctor",
                    sortOrder = maxSort + 5,
                    categoryType = "EXPENSE"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Διασκέδαση & Ψυχαγωγία",
                    iconKey = "entertainment",
                    monthlyBudget = 0.0,
                    colorHex = 0xFF8B5CF6,
                    voiceKeywords = "διασκέδαση, σινεμά, θέατρο, ποτό, συναυλία, έξοδος, netflix, entertainment, cinema",
                    sortOrder = maxSort + 6,
                    categoryType = "EXPENSE"
                )
            )
        }
        val missing = standardCategories.filter { std ->
            existing.none { it.name.trim().equals(std.name.trim(), ignoreCase = true) }
        }
        val toAdd = if (maxTotalExpenseCategories != null) {
            val allowedNew = (maxTotalExpenseCategories - existing.size).coerceAtLeast(0)
            missing.take(allowedNew)
        } else {
            missing
        }
        if (toAdd.isNotEmpty()) {
            expenseDao.insertCategories(toAdd)
        }
    }

    suspend fun seedStandardIncomeCategories(
        maxTotalIncomeCategories: Int? = null,
        isEnglish: Boolean = false
    ) {
        val existing = expenseDao.getAllCategoriesList().filter { it.isIncomeCategory }
        val maxSort = existing.maxOfOrNull { it.sortOrder } ?: -1
        val standardIncomeCategories = if (isEnglish) {
            listOf(
                ExpenseCategory(
                    id = 0L,
                    name = "Salary",
                    iconKey = "work",
                    monthlyBudget = 0.0,
                    colorHex = 0xFF059669,
                    voiceKeywords = "salary, paycheck, wages, payroll, μισθός, μισθοδοσία",
                    sortOrder = maxSort + 1,
                    categoryType = "INCOME"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Rent",
                    iconKey = "house",
                    monthlyBudget = 0.0,
                    colorHex = 0xFF0D9488,
                    voiceKeywords = "rent, rental, lease, ενοίκιο, μίσθωμα",
                    sortOrder = maxSort + 2,
                    categoryType = "INCOME"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Business / Freelance",
                    iconKey = "tech",
                    monthlyBudget = 0.0,
                    colorHex = 0xFF0284C7,
                    voiceKeywords = "business, freelance, invoice, client, project, επιχείρηση, τιμολόγιο",
                    sortOrder = maxSort + 3,
                    categoryType = "INCOME"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Subsidy / Allowance",
                    iconKey = "loan",
                    monthlyBudget = 0.0,
                    colorHex = 0xFF8B5CF6,
                    voiceKeywords = "subsidy, allowance, grant, refund, benefit, επιδότηση, επίδομα",
                    sortOrder = maxSort + 4,
                    categoryType = "INCOME"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Bonus / Gift",
                    iconKey = "gift",
                    monthlyBudget = 0.0,
                    colorHex = 0xFFD97706,
                    voiceKeywords = "bonus, gift, reward, tip, μπόνους, δώρο",
                    sortOrder = maxSort + 5,
                    categoryType = "INCOME"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Investments",
                    iconKey = "savings",
                    monthlyBudget = 0.0,
                    colorHex = 0xFF10B981,
                    voiceKeywords = "investments, dividend, interest, stocks, crypto, επενδύσεις, μέρισμα",
                    sortOrder = maxSort + 6,
                    categoryType = "INCOME"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Other",
                    iconKey = "other",
                    monthlyBudget = 0.0,
                    colorHex = 0xFF475569,
                    voiceKeywords = "other, misc, income, άλλο, διάφορα, έσοδο",
                    sortOrder = maxSort + 7,
                    categoryType = "INCOME"
                )
            )
        } else {
            listOf(
                ExpenseCategory(
                    id = 0L,
                    name = "Μισθός",
                    iconKey = "work",
                    monthlyBudget = 0.0,
                    colorHex = 0xFF059669,
                    voiceKeywords = "μισθός, μισθοδοσία, salary, paycheck",
                    sortOrder = maxSort + 1,
                    categoryType = "INCOME"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Ενοίκιο",
                    iconKey = "house",
                    monthlyBudget = 0.0,
                    colorHex = 0xFF0D9488,
                    voiceKeywords = "ενοίκιο, μίσθωμα, rent",
                    sortOrder = maxSort + 2,
                    categoryType = "INCOME"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Επιχείρηση / Freelance",
                    iconKey = "tech",
                    monthlyBudget = 0.0,
                    colorHex = 0xFF0284C7,
                    voiceKeywords = "επιχείρηση, freelance, τιμολόγιο, πελάτης, έργο, business",
                    sortOrder = maxSort + 3,
                    categoryType = "INCOME"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Επιδότηση",
                    iconKey = "loan",
                    monthlyBudget = 0.0,
                    colorHex = 0xFF8B5CF6,
                    voiceKeywords = "επιδότηση, επίδομα, επιστροφή, subsidy, allowance",
                    sortOrder = maxSort + 4,
                    categoryType = "INCOME"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Bonus / Δώρο",
                    iconKey = "gift",
                    monthlyBudget = 0.0,
                    colorHex = 0xFFD97706,
                    voiceKeywords = "bonus, μπόνους, δώρο, πάσχα, χριστούγεννα, gift",
                    sortOrder = maxSort + 5,
                    categoryType = "INCOME"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Επενδύσεις",
                    iconKey = "savings",
                    monthlyBudget = 0.0,
                    colorHex = 0xFF10B981,
                    voiceKeywords = "επενδύσεις, μέρισμα, τόκοι, μετοχές, investments",
                    sortOrder = maxSort + 6,
                    categoryType = "INCOME"
                ),
                ExpenseCategory(
                    id = 0L,
                    name = "Άλλο",
                    iconKey = "other",
                    monthlyBudget = 0.0,
                    colorHex = 0xFF475569,
                    voiceKeywords = "άλλο, διάφορα, έσοδο, other",
                    sortOrder = maxSort + 7,
                    categoryType = "INCOME"
                )
            )
        }
        val missing = standardIncomeCategories.filter { std ->
            existing.none { it.name.trim().equals(std.name.trim(), ignoreCase = true) }
        }
        val toAdd = if (maxTotalIncomeCategories != null) {
            val allowedNew = (maxTotalIncomeCategories - existing.size).coerceAtLeast(0)
            missing.take(allowedNew)
        } else {
            missing
        }
        if (toAdd.isNotEmpty()) {
            expenseDao.insertCategories(toAdd)
        }
    }
}
