package com.example.ui.screens.transactions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import com.example.ui.components.EmptyStateView
import com.example.ui.components.formatAmount
import com.example.ui.components.resolveCategoryColor
import com.example.ui.components.resolveCategoryIcon
import com.example.ui.theme.FinanceGreen
import com.example.ui.theme.FinanceRed
import com.example.ui.viewmodel.FinanceViewModel
import com.example.ui.viewmodel.SortOrder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    viewModel: FinanceViewModel,
    onAddTransaction: () -> Unit,
    onImportStatement: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()
    val selectedCategoryFilter by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()

    var showSortMenu by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var deletingTransaction by remember { mutableStateOf<Transaction?>(null) }

    // Grouping by date
    val dayFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US) }
    val groupedTransactions by remember(transactions) {
        derivedStateOf {
            transactions.groupBy { tx ->
                dayFormat.format(Date(tx.timestamp))
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("transactions_screen")
        ) {
            // Search Bar & Filter Controls Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search transactions, tags, accounts...") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("transaction_search_input")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Import Statement Button
                    IconButton(
                        onClick = onImportStatement,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .testTag("import_statement_shortcut_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = "Import Statement",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Sort Button & Dropdown
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .testTag("sort_menu_btn")
                        ) {
                            Icon(Icons.Default.Sort, contentDescription = "Sort")
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Date (Newest First)") },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.DATE_DESC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Date (Oldest First)") },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.DATE_ASC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Amount (High to Low)") },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.AMOUNT_DESC)
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Amount (Low to High)") },
                                onClick = {
                                    viewModel.setSortOrder(SortOrder.AMOUNT_ASC)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Type Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { viewModel.setTypeFilter(null) },
                        label = { Text("All Types") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("filter_all_types")
                    )

                    TransactionType.values().forEach { t ->
                        val isSelected = selectedTypeFilter == t
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setTypeFilter(if (isSelected) null else t) },
                            label = { Text(t.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.testTag("filter_${t.name.lowercase()}")
                        )
                    }

                    if (selectedCategoryFilter != null) {
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.setCategoryFilter(null) },
                            label = { Text("Category: $selectedCategoryFilter ✕") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )
                    }
                }
            }

            // Transactions Grouped List
            if (transactions.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Default.ReceiptLong,
                    title = "No Transactions Found",
                    message = if (searchQuery.isNotEmpty() || selectedTypeFilter != null) {
                        "Try adjusting your filters or search terms."
                    } else {
                        "Your transaction ledger is empty. Tap the '+' button below to log your first record."
                    },
                    actionButtonText = if (searchQuery.isNotEmpty() || selectedTypeFilter != null) "Clear Filters" else "Add Transaction",
                    onActionClick = {
                        if (searchQuery.isNotEmpty() || selectedTypeFilter != null) {
                            viewModel.setSearchQuery("")
                            viewModel.setTypeFilter(null)
                            viewModel.setCategoryFilter(null)
                        } else {
                            onAddTransaction()
                        }
                    },
                    modifier = Modifier.padding(top = 32.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    groupedTransactions.forEach { (dateHeader, txsInDay) ->
                        val dayTotalExpense = txsInDay.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                        val dayTotalIncome = txsInDay.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }

                        stickyHeader {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = dateHeader,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )

                                if (dayTotalExpense > 0 || dayTotalIncome > 0) {
                                    Text(
                                        text = if (dayTotalExpense > 0) "-${userSettings.currency}${formatAmount(dayTotalExpense)}" else "+${userSettings.currency}${formatAmount(dayTotalIncome)}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (dayTotalExpense > 0) FinanceRed else FinanceGreen
                                    )
                                }
                            }
                        }

                        items(txsInDay, key = { it.id }) { tx ->
                            TransactionItemCard(
                                transaction = tx,
                                currency = userSettings.currency,
                                onEdit = { editingTransaction = tx },
                                onDelete = { deletingTransaction = tx }
                            )
                        }
                    }
                }
            }
        }

        // Edit Dialog
        if (editingTransaction != null) {
            AddEditTransactionDialog(
                initialTransaction = editingTransaction,
                currency = userSettings.currency,
                onDismiss = { editingTransaction = null },
                onSave = { updated ->
                    viewModel.updateTransaction(updated)
                    editingTransaction = null
                }
            )
        }

        // Delete Confirmation Dialog
        if (deletingTransaction != null) {
            AlertDialog(
                onDismissRequest = { deletingTransaction = null },
                title = { Text("Delete Transaction?") },
                text = {
                    Text("Are you sure you want to delete '${deletingTransaction!!.description.ifBlank { deletingTransaction!!.category }}' of ${userSettings.currency}${formatAmount(deletingTransaction!!.amount)}? This action can be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteTransaction(deletingTransaction!!)
                            deletingTransaction = null
                        },
                        modifier = Modifier.testTag("confirm_delete_tx_btn")
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingTransaction = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun TransactionItemCard(
    transaction: Transaction,
    currency: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.US) }
    val isExpense = transaction.type == TransactionType.EXPENSE
    val isIncome = transaction.type == TransactionType.INCOME

    val sign = when {
        isIncome -> "+"
        isExpense -> "-"
        else -> ""
    }

    val amountColor = when {
        isIncome -> FinanceGreen
        isExpense -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("tx_card_${transaction.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                            .background(resolveCategoryColor(transaction.category).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = resolveCategoryIcon(transaction.category),
                            contentDescription = transaction.category,
                            tint = resolveCategoryColor(transaction.category),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = transaction.description.ifBlank { transaction.category },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${transaction.category} • ${transaction.account} • ${timeFormat.format(Date(transaction.timestamp))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "$sign$currency${formatAmount(transaction.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = amountColor
                )
            }

            // Notes / Tags preview if present
            if (transaction.notes.isNotBlank() || transaction.tags.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (transaction.notes.isNotBlank()) {
                        Text(
                            text = "“${transaction.notes}”",
                            style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    if (transaction.tags.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = transaction.tags,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Quick actions: Edit and Delete
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("edit_tx_btn_${transaction.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_tx_btn_${transaction.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
