package com.example.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Goal
import com.example.ui.components.EmptyStateView
import com.example.ui.components.FinancialProgressBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatAmount
import com.example.ui.theme.FinanceGreen
import com.example.ui.viewmodel.FinanceViewModel

@Composable
fun GoalsScreen(
    viewModel: FinanceViewModel,
    modifier: Modifier = Modifier
) {
    val goals by viewModel.allGoals.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val currency = userSettings.currency

    var showAddGoalDialog by remember { mutableStateOf(false) }
    var contributingGoal by remember { mutableStateOf<Goal?>(null) }
    var deletingGoal by remember { mutableStateOf<Goal?>(null) }

    val totalTarget = goals.sumOf { it.targetAmount }
    val totalSaved = goals.sumOf { it.currentAmount }
    val overallProgress = if (totalTarget > 0) (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f) else 0f

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("goals_screen"),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overall Savings Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("goals_summary_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Savings Accumulated",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$currency${formatAmount(totalSaved)}",
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Savings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        FinancialProgressBar(
                            progress = overallProgress,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.height(8.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Target: $currency${formatAmount(totalTarget)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "${(overallProgress * 100).toInt()}% Achieved",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Goals List
            item {
                SectionHeader(
                    title = "Savings Goals & Targets",
                    actionLabel = "+ New Goal",
                    onActionClick = { showAddGoalDialog = true }
                )
            }

            if (goals.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.Savings,
                        title = "No Goals Created Yet",
                        message = "Define what you're saving for (e.g., Vacation, Emergency Fund, Tech Gadget) and track every milestone.",
                        actionButtonText = "Create First Goal",
                        onActionClick = { showAddGoalDialog = true }
                    )
                }
            } else {
                items(goals, key = { it.id }) { goal ->
                    GoalCard(
                        goal = goal,
                        currency = currency,
                        onContribute = { contributingGoal = goal },
                        onDelete = { deletingGoal = goal }
                    )
                }
            }
        }

        // Add Goal Dialog
        if (showAddGoalDialog) {
            AddGoalDialog(
                currency = currency,
                onDismiss = { showAddGoalDialog = false },
                onSave = { newGoal ->
                    viewModel.addGoal(newGoal)
                    showAddGoalDialog = false
                }
            )
        }

        // Contribute Funds Dialog
        if (contributingGoal != null) {
            ContributeGoalDialog(
                goal = contributingGoal!!,
                currency = currency,
                onDismiss = { contributingGoal = null },
                onContribute = { amount ->
                    viewModel.contributeToGoal(contributingGoal!!, amount)
                    contributingGoal = null
                }
            )
        }

        // Delete Confirmation
        if (deletingGoal != null) {
            AlertDialog(
                onDismissRequest = { deletingGoal = null },
                title = { Text("Delete Goal?") },
                text = { Text("Are you sure you want to remove '${deletingGoal!!.title}'?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteGoal(deletingGoal!!)
                            deletingGoal = null
                        }
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingGoal = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun GoalCard(
    goal: Goal,
    currency: String,
    onContribute: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progressPct = (goal.progressPercentage * 100).toInt()
    val isComplete = goal.isCompleted || goal.currentAmount >= goal.targetAmount

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("goal_card_${goal.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isComplete) FinanceGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (isComplete) FinanceGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isComplete) Icons.Default.CheckCircle else Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = if (isComplete) FinanceGreen else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (goal.notes.isNotBlank()) {
                            Text(
                                text = goal.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Progress bar
            FinancialProgressBar(
                progress = goal.progressPercentage,
                color = if (isComplete) FinanceGreen else MaterialTheme.colorScheme.primary,
                modifier = Modifier.height(8.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$currency${formatAmount(goal.currentAmount)} / $currency${formatAmount(goal.targetAmount)}",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isComplete) "Target Reached! 🎉" else "Remaining: $currency${formatAmount(goal.remainingAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isComplete) FinanceGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onContribute,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("contribute_btn_${goal.id}")
                ) {
                    Text("+ Add Funds", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun AddGoalDialog(
    currency: String,
    onDismiss: () -> Unit,
    onSave: (Goal) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var targetAmountText by remember { mutableStateOf("") }
    var currentAmountText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Savings Goal", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Goal Title") },
                    placeholder = { Text("e.g. Vacation, Emergency Fund") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_title_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = targetAmountText,
                    onValueChange = { targetAmountText = it },
                    label = { Text("Target Amount ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_target_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = currentAmountText,
                    onValueChange = { currentAmountText = it },
                    label = { Text("Initial Saved Amount ($currency)") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("goal_initial_amount_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Purpose") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetAmountText.toDoubleOrNull()
                    if (title.isBlank()) {
                        errorMessage = "Please enter a goal title"
                        return@Button
                    }
                    if (target == null || target <= 0) {
                        errorMessage = "Please enter a valid target amount"
                        return@Button
                    }
                    val current = currentAmountText.toDoubleOrNull() ?: 0.0

                    onSave(
                        Goal(
                            title = title.trim(),
                            targetAmount = target,
                            currentAmount = current,
                            notes = notes.trim(),
                            isCompleted = current >= target
                        )
                    )
                },
                modifier = Modifier.testTag("save_new_goal_btn")
            ) {
                Text("Create Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ContributeGoalDialog(
    goal: Goal,
    currency: String,
    onDismiss: () -> Unit,
    onContribute: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Funds to ${goal.title}", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Current saved: $currency${formatAmount(goal.currentAmount)} / $currency${formatAmount(goal.targetAmount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        errorMessage = null
                    },
                    label = { Text("Deposit Amount ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("contribute_amount_input")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.toDoubleOrNull()
                    if (amt == null || amt <= 0) {
                        errorMessage = "Please enter a valid amount"
                        return@Button
                    }
                    onContribute(amt)
                },
                modifier = Modifier.testTag("confirm_contribute_btn")
            ) {
                Text("Deposit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
