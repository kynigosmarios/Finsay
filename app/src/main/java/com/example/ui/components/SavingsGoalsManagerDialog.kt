package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.entity.Expense
import com.example.data.entity.FamilyMember
import com.example.data.entity.Income
import com.example.data.entity.RecurringExpense
import com.example.data.entity.SavingsGoal
import com.example.ui.theme.Emerald700
import com.example.util.Localization
import java.util.Locale

@Suppress("UNUSED_PARAMETER")
@Composable
fun SavingsGoalsManagerDialog(
    goals: List<SavingsGoal>,
    allExpenses: List<Expense> = emptyList(),
    allIncomes: List<Income> = emptyList(),
    recurringExpenses: List<RecurringExpense> = emptyList(),
    members: List<FamilyMember> = emptyList(),
    defaultPayerMember: FamilyMember? = null,
    householdName: String = "",
    currencySymbol: String = "€",
    canManage: Boolean = true,
    appLanguage: String = "EL",
    onDismiss: () -> Unit,
    onOpenGoalEditor: (SavingsGoal?) -> Unit,
    onDeleteGoal: (SavingsGoal) -> Unit,
    onDepositToGoal: (SavingsGoal, Double, FamilyMember?, String) -> Unit
) {
    val lang = Localization.Language.fromCode(appLanguage)

    var goalForDeposit by remember { mutableStateOf<SavingsGoal?>(null) }
    var goalPendingDelete by remember { mutableStateOf<SavingsGoal?>(null) }

    val subtitle = remember(householdName, lang) {
        if (householdName.isNotBlank()) Localization.translateHouseholdName(householdName, lang)
        else Localization.tr(lang, "Active household savings goals", "Στόχοι ενεργού εξοδολογίου")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("savings_goals_manager_dialog")
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
                        .testTag("goals_close_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = Localization.tr(lang, "Back", "Επιστροφή"),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = Emerald700.copy(alpha = 0.14f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = Localization.tr(lang, "Goals", "Στόχοι"),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Primary Action Button: + Νέος Στόχος
        if (canManage) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onOpenGoalEditor(null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(44.dp)
                    .testTag("create_new_goal_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Emerald700)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = Localization.tr(lang, "New Savings Goal", "Νέος Στόχος Αποταμίευσης"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )

        if (goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                EmptyGoalsSection(
                    lang = lang,
                    canManage = canManage,
                    onSelectTemplate = { title, targetAmount, note ->
                        onOpenGoalEditor(
                            SavingsGoal(
                                id = 0L,
                                title = title,
                                targetAmount = targetAmount,
                                currentAmount = 0.0,
                                deadlineDate = "",
                                note = note
                            )
                        )
                    }
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                items(
                    items = goals,
                    key = { it.id },
                    contentType = { "savings_goal_card" }
                ) { goal ->
                    SavingsGoalCard(
                        goal = goal,
                        currencySymbol = currencySymbol,
                        lang = lang,
                        canManage = canManage,
                        onDepositClick = { goalForDeposit = goal },
                        onEditClick = { onOpenGoalEditor(goal) },
                        onDeleteClick = { goalPendingDelete = goal },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    // Quick Deposit Sub-dialog
    goalForDeposit?.let { targetGoal ->
        GoalDepositDialog(
            goal = targetGoal,
            members = members,
            defaultPayerMember = defaultPayerMember,
            currencySymbol = currencySymbol,
            lang = lang,
            onDismiss = { goalForDeposit = null },
            onConfirmDeposit = { amount, payer, note ->
                onDepositToGoal(targetGoal, amount, payer, note)
                goalForDeposit = null
            }
        )
    }

    // Delete Confirmation Dialog
    goalPendingDelete?.let { targetGoal ->
        AlertDialog(
            onDismissRequest = { goalPendingDelete = null },
            shape = RoundedCornerShape(22.dp),
            title = {
                Text(
                    text = Localization.tr(lang, "Delete Goal?", "Διαγραφή Στόχου;"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = Localization.trFormat(
                        lang,
                        "Are you sure you want to delete the goal \"%1\$s\"?",
                        "Θέλετε σίγουρα να διαγράψετε τον στόχο «%1\$s»;",
                        targetGoal.title
                    ),
                    fontSize = 13.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGoal(targetGoal)
                        goalPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(Localization.delete(lang), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { goalPendingDelete = null }) {
                    Text(Localization.cancel(lang))
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyGoalsSection(
    lang: Localization.Language,
    canManage: Boolean,
    onSelectTemplate: (title: String, targetAmount: Double, note: String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = Emerald700.copy(alpha = 0.12f),
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = Emerald700,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = Localization.tr(lang, "No savings goals yet", "Δεν υπάρχουν ακόμη στόχοι"),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (canManage) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = Localization.tr(lang, "Suggested goal templates:", "Γρήγορες προτάσεις στόχων:"),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val templates = listOf(
                    Triple(
                        "🏖️ " + Localization.tr(lang, "Vacation", "Διακοπές"),
                        1200.0,
                        Localization.tr(lang, "Summer trip", "Καλοκαιρινές διακοπές")
                    ),
                    Triple(
                        "🆘 " + Localization.tr(lang, "Emergency Fund", "Ταμείο Ανάγκης"),
                        2000.0,
                        Localization.tr(lang, "Safety buffer", "Οικονομικό μαξιλάρι ασφαλείας")
                    ),
                    Triple(
                        "🚗 " + Localization.tr(lang, "Car", "Αυτοκίνητο"),
                        3500.0,
                        Localization.tr(lang, "Vehicle fund", "Αγορά / συντήρηση οχήματος")
                    ),
                    Triple(
                        "🏠 " + Localization.tr(lang, "Home", "Σπίτι"),
                        1500.0,
                        Localization.tr(lang, "Home upgrade", "Εξοπλισμός & βελτιώσεις")
                    )
                )
                templates.forEach { (label, amount, note) ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            onSelectTemplate(label.substringAfter(" "), amount, note)
                        },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun SavingsGoalCard(
    goal: SavingsGoal,
    currencySymbol: String,
    lang: Localization.Language,
    canManage: Boolean,
    onDepositClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeTarget = goal.targetAmount.coerceAtLeast(0.0)
    val safeCurrent = goal.currentAmount.coerceAtLeast(0.0)
    val remainingAmount = (safeTarget - safeCurrent).coerceAtLeast(0.0)
    val progress = if (safeTarget > 0.0) (safeCurrent / safeTarget).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = FinsayMotion.ProgressSpec,
        label = "savingsGoalProgress"
    )
    val isCompleted = safeTarget > 0.0 && safeCurrent >= safeTarget
    val percent = (progress * 100f).toInt().coerceIn(0, 100)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.36f)
        ),
        border = BorderStroke(
            1.dp,
            if (isCompleted) Emerald700.copy(alpha = 0.45f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Icon + Title + % Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Emerald700.copy(alpha = 0.14f),
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Default.Check else Icons.Default.Flag,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (goal.deadlineDate.isNotBlank() || goal.note.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (goal.deadlineDate.isNotBlank()) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(11.dp)
                                )
                                Text(
                                    text = goal.deadlineDate,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (goal.deadlineDate.isNotBlank() && goal.note.isNotBlank()) {
                                Text("•", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (goal.note.isNotBlank()) {
                                Text(
                                    text = goal.note,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Emerald700.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = if (isCompleted) "100% ✓" else "$percent%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald700,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Progress Bar + Saved / Target & Remaining
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    color = Emerald700,
                    trackColor = Emerald700.copy(alpha = 0.14f),
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${formatCurrency(safeCurrent, currencySymbol)} / ${formatCurrency(safeTarget, currencySymbol)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isCompleted) {
                            Localization.tr(lang, "Completed!", "Ολοκληρώθηκε!")
                        } else {
                            Localization.trFormat(
                                lang,
                                "Left: %1\$s",
                                "Λείπουν: %1\$s",
                                formatCurrency(remainingAmount, currencySymbol)
                            )
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isCompleted) Emerald700 else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions Row
            if (canManage) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = onDepositClick,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = Localization.tr(lang, "Deposit", "Κατάθεση"),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = Localization.tr(lang, "Edit", "Επεξεργασία"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = Localization.tr(lang, "Delete", "Διαγραφή"),
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalDepositDialog(
    goal: SavingsGoal,
    members: List<FamilyMember>,
    defaultPayerMember: FamilyMember?,
    currencySymbol: String,
    lang: Localization.Language,
    onDismiss: () -> Unit,
    onConfirmDeposit: (Double, FamilyMember?, String) -> Unit
) {
    val remainingAmount = (goal.targetAmount - goal.currentAmount).coerceAtLeast(0.0)
    val defaultSuggestion = if (remainingAmount > 0.0) remainingAmount.coerceAtMost(50.0) else 50.0
    var amountText by remember(goal.id) {
        mutableStateOf(String.format(Locale.US, "%.0f", defaultSuggestion))
    }
    var selectedPayer by remember(goal.id, defaultPayerMember, members) {
        mutableStateOf(defaultPayerMember ?: members.firstOrNull())
    }
    var noteText by remember(goal.id) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = Localization.tr(lang, "Deposit to Goal", "Κατάθεση στον Στόχο"),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = goal.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Emerald700
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text(Localization.trFormat(lang, "Deposit amount (%1\$s)", "Ποσό κατάθεσης (%1\$s)", currencySymbol)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald700,
                        focusedLabelColor = Emerald700,
                        cursorColor = Emerald700
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goal_deposit_amount_input")
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(20.0, 50.0, 100.0).forEach { quickAmt ->
                        FilterChip(
                            selected = false,
                            onClick = { amountText = String.format(Locale.US, "%.0f", quickAmt) },
                            label = { Text("+${quickAmt.toInt()}$currencySymbol", fontSize = 11.5.sp) }
                        )
                    }
                    if (remainingAmount >= 1.0) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                amountText = String.format(Locale.US, "%.2f", remainingAmount)
                            },
                            label = {
                                Text(
                                    text = Localization.tr(lang, "Pay off", "Υπόλοιπο"),
                                    fontSize = 11.5.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Emerald700.copy(alpha = 0.12f),
                                labelColor = Emerald700
                            )
                        )
                    }
                }

                if (members.size > 1) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Emerald700,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = Localization.tr(lang, "Deposited by", "Ποιος κατέθεσε;"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            members.forEach { member ->
                                val isSel = selectedPayer?.id == member.id
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedPayer = member },
                                    label = {
                                        Text(
                                            text = member.name,
                                            fontSize = 11.5.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Emerald700.copy(alpha = 0.16f),
                                        selectedLabelColor = Emerald700
                                    )
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text(Localization.tr(lang, "Note (optional)", "Σημείωση (προαιρετικά)")) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald700,
                        focusedLabelColor = Emerald700,
                        cursorColor = Emerald700
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Emerald700.copy(alpha = 0.09f),
                    border = BorderStroke(1.dp, Emerald700.copy(alpha = 0.22f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = Emerald700,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = Localization.tr(
                                lang,
                                "Automatically recorded in Expenses.",
                                "Καταχωρείται αυτόματα και στα Έξοδα."
                            ),
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = amountText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    if (parsed > 0.0) {
                        onConfirmDeposit(parsed, selectedPayer, noteText.trim())
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald700),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("goal_deposit_confirm_button")
            ) {
                Text(
                    text = Localization.tr(lang, "Deposit", "Κατάθεση"),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Localization.cancel(lang))
            }
        }
    )
}

private fun formatCurrency(value: Double, symbol: String): String {
    val clean = if (kotlin.math.abs(value - value.toLong()) < 0.005) {
        String.format(Locale.US, "%.0f", value)
    } else {
        String.format(Locale.US, "%.2f", value)
    }
    return "$clean$symbol"
}
