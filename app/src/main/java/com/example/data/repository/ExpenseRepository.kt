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
        isEnglish: Boolean = false,
        lang: com.example.util.Localization.Language? = null
    ) {
        val resolvedLang = lang ?: if (isEnglish) com.example.util.Localization.Language.ENGLISH else com.example.util.Localization.Language.GREEK
        val existing = expenseDao.getAllCategoriesList().filter { !it.isIncomeCategory }
        val maxSort = existing.maxOfOrNull { it.sortOrder } ?: -1
        val standardCategories = com.example.ui.viewmodel.OikoNomosViewModel
            .getStandardDefaultCategories(resolvedLang)
            .take(6)
            .mapIndexed { idx, cat ->
                cat.copy(id = 0L, sortOrder = maxSort + idx + 1)
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
        isEnglish: Boolean = false,
        lang: com.example.util.Localization.Language? = null
    ) {
        val resolvedLang = lang ?: if (isEnglish) com.example.util.Localization.Language.ENGLISH else com.example.util.Localization.Language.GREEK
        val existing = expenseDao.getAllCategoriesList().filter { it.isIncomeCategory }
        val maxSort = existing.maxOfOrNull { it.sortOrder } ?: -1
        val standardIncomeCategories = com.example.ui.viewmodel.OikoNomosViewModel
            .getStandardDefaultIncomeCategories(resolvedLang)
            .mapIndexed { idx, cat ->
                cat.copy(id = 0L, sortOrder = maxSort + idx + 1)
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
