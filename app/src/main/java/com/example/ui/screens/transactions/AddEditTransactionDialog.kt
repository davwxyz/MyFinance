package com.example.ui.screens.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.components.resolveCategoryColor
import com.example.ui.components.resolveCategoryIcon
import com.example.ui.theme.FinanceAmber
import com.example.ui.theme.FinanceGreen
import com.example.ui.theme.FinanceRed

val DEFAULT_EXPENSE_CATEGORIES = listOf(
    "Food & Dining",
    "Groceries",
    "Housing",
    "Transport",
    "Bills & Utilities",
    "Entertainment",
    "Shopping",
    "Health",
    "Education",
    "Personal Care",
    "General"
)

val DEFAULT_INCOME_CATEGORIES = listOf(
    "Salary",
    "Freelance",
    "Investments",
    "Bonus",
    "Gift",
    "Other Income"
)

val DEFAULT_ACCOUNTS = listOf(
    "Cash",
    "Debit Card",
    "Credit Card",
    "Bank Account",
    "Savings Account"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddEditTransactionDialog(
    initialTransaction: Transaction?,
    currency: String,
    onDismiss: () -> Unit,
    onSave: (Transaction) -> Unit
) {
    var type by remember { mutableStateOf(initialTransaction?.type ?: TransactionType.EXPENSE) }
    var amountText by remember {
        mutableStateOf(
            if (initialTransaction != null && initialTransaction.amount > 0) {
                if (initialTransaction.amount % 1.0 == 0.0) {
                    initialTransaction.amount.toInt().toString()
                } else {
                    initialTransaction.amount.toString()
                }
            } else ""
        )
    }
    var description by remember { mutableStateOf(initialTransaction?.description ?: "") }
    var selectedCategory by remember {
        mutableStateOf(
            initialTransaction?.category ?: (if (type == TransactionType.INCOME) "Salary" else "Food & Dining")
        )
    }
    var selectedAccount by remember { mutableStateOf(initialTransaction?.account ?: "Cash") }
    var notes by remember { mutableStateOf(initialTransaction?.notes ?: "") }
    var tags by remember { mutableStateOf(initialTransaction?.tags ?: "") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
                .clip(RoundedCornerShape(28.dp))
                .testTag("add_edit_transaction_dialog"),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header with close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialTransaction == null) "New Transaction" else "Edit Transaction",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_transaction_dialog_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Type Selector (Expense, Income, Transfer)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TransactionType.values().forEach { t ->
                        val isSelected = type == t
                        val activeColor = when (t) {
                            TransactionType.EXPENSE -> FinanceRed
                            TransactionType.INCOME -> FinanceGreen
                            TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
                            TransactionType.REFUND -> FinanceAmber
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    type = t
                                    if (t == TransactionType.INCOME && selectedCategory in DEFAULT_EXPENSE_CATEGORIES) {
                                        selectedCategory = "Salary"
                                    } else if (t == TransactionType.EXPENSE && selectedCategory in DEFAULT_INCOME_CATEGORIES) {
                                        selectedCategory = "Food & Dining"
                                    }
                                }
                                .testTag("type_btn_${t.name.lowercase()}"),
                            color = if (isSelected) activeColor else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = t.displayName,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*(\.\d{0,2})?$"""))) {
                            amountText = input
                            errorMessage = null
                        }
                    },
                    label = { Text("Amount ($currency)") },
                    placeholder = { Text("0.00") },
                    leadingIcon = {
                        Text(
                            text = currency,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_amount_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Description Field
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        errorMessage = null
                    },
                    label = { Text("Description") },
                    placeholder = { Text("e.g., Grocery shopping, Monthly rent") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_description_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selection
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                val availableCategories = if (type == TransactionType.INCOME) DEFAULT_INCOME_CATEGORIES else DEFAULT_EXPENSE_CATEGORIES

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    availableCategories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            leadingIcon = {
                                Icon(
                                    imageVector = resolveCategoryIcon(cat),
                                    contentDescription = cat,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else resolveCategoryColor(cat),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("cat_chip_$cat")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Account Selection
                Text(
                    text = "Account / Payment Method",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DEFAULT_ACCOUNTS.forEach { acc ->
                        val isSelected = selectedAccount == acc
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedAccount = acc },
                            label = { Text(acc) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier.testTag("acc_chip_$acc")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Additional receipt details or comments") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_notes_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tags Field
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (Optional)") },
                    placeholder = { Text("e.g., #tax-deductible, #trip, #work") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_tags_input")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions: Cancel & Save
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_transaction_btn")
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val parsedAmount = amountText.toDoubleOrNull()
                            if (parsedAmount == null || parsedAmount <= 0.0) {
                                errorMessage = "Please enter a valid amount greater than zero"
                                return@Button
                            }
                            if (description.isBlank()) {
                                description = selectedCategory
                            }

                            val tx = Transaction(
                                id = initialTransaction?.id ?: 0,
                                type = type,
                                amount = parsedAmount,
                                currency = currency,
                                category = selectedCategory,
                                description = description.trim(),
                                timestamp = initialTransaction?.timestamp ?: System.currentTimeMillis(),
                                account = selectedAccount,
                                notes = notes.trim(),
                                tags = tags.trim(),
                                createdAt = initialTransaction?.createdAt ?: System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                            onSave(tx)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("save_transaction_btn")
                    ) {
                        Text(if (initialTransaction == null) "Add Record" else "Save Changes")
                    }
                }
            }
        }
    }
}
