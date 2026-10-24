package com.example.ui.screens.more

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Bill
import com.example.data.model.FinancialNote
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatAmount
import com.example.ui.theme.FinanceGreen
import com.example.ui.theme.FinanceRed
import com.example.ui.viewmodel.FinanceViewModel

val SUPPORTED_CURRENCIES = listOf("$", "€", "£", "₹", "¥", "C$", "A$", "CHF", "R$")

@Composable
fun MoreScreen(
    viewModel: FinanceViewModel,
    onNavigateToImport: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Workspace & Bills, 1: Settings & Data
    val context = LocalContext.current

    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()
    val bills by viewModel.allBills.collectAsStateWithLifecycle()
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()

    var showAddBillDialog by remember { mutableStateOf(false) }
    var showAddNoteDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("more_screen")
    ) {
        // Tab Navigation inside More
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Workspace & Bills", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_workspace")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Settings & Data", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_settings")
            )
        }

        if (selectedTab == 0) {
            // Workspace & Scheduled Bills View
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Scheduled Bills Section
                item {
                    SectionHeader(
                        title = "Recurring Bills & Commitments",
                        actionLabel = "+ Add Bill",
                        onActionClick = { showAddBillDialog = true }
                    )
                }

                if (bills.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.Receipt,
                            title = "No Recurring Bills Added",
                            message = "Keep track of rent, subscriptions, internet, and utilities before they're due.",
                            actionButtonText = "Add Recurring Bill",
                            onActionClick = { showAddBillDialog = true }
                        )
                    }
                } else {
                    items(bills, key = { it.id }) { bill ->
                        BillItemCard(
                            bill = bill,
                            currency = userSettings.currency,
                            onTogglePaid = { viewModel.toggleBillPaid(bill) },
                            onDelete = { viewModel.deleteBill(bill) }
                        )
                    }
                }

                // Financial Content & Planning Workspace
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader(
                        title = "Financial Notes & Strategy Workspace",
                        actionLabel = "+ New Note",
                        onActionClick = { showAddNoteDialog = true }
                    )
                }

                if (notes.isEmpty()) {
                    item {
                        EmptyStateView(
                            icon = Icons.Default.PushPin,
                            title = "Workspace Empty",
                            message = "Store your 50/30/20 budget strategies, financial checklists, or debt payoff goals.",
                            actionButtonText = "Create Note",
                            onActionClick = { showAddNoteDialog = true }
                        )
                    }
                } else {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onDelete = { viewModel.deleteNote(note) }
                        )
                    }
                }
            }
        } else {
            // Settings & Data Management View
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Currency Selector
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("currency_settings_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Currency Symbol",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Current: ${userSettings.currency}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SUPPORTED_CURRENCIES.take(5).forEach { sym ->
                                    val isSelected = userSettings.currency == sym
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updateCurrency(sym) },
                                        label = { Text(sym) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        modifier = Modifier.testTag("currency_chip_$sym")
                                    )
                                }
                            }
                        }
                    }
                }

                // Budget & Starting Balance Configuration
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("budget_settings_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                    ) {
                        var monthlyBudgetInput by remember { mutableStateOf(userSettings.monthlyBudget.toInt().toString()) }
                        var startingBalanceInput by remember { mutableStateOf(userSettings.startingBalance.toInt().toString()) }

                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Budget & Starting Balance",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = monthlyBudgetInput,
                                onValueChange = { monthlyBudgetInput = it },
                                label = { Text("Monthly Budget Limit (${userSettings.currency})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("setting_budget_input")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = startingBalanceInput,
                                onValueChange = { startingBalanceInput = it },
                                label = { Text("Initial Account Balance (${userSettings.currency})") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("setting_balance_input")
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    val b = monthlyBudgetInput.toDoubleOrNull()
                                    val s = startingBalanceInput.toDoubleOrNull()
                                    if (b != null && b > 0) viewModel.updateMonthlyBudget(b)
                                    if (s != null) viewModel.updateStartingBalance(s)
                                    Toast.makeText(context, "Settings updated", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth().testTag("save_settings_btn")
                            ) {
                                Text("Update Budget Parameters")
                            }
                        }
                    }
                }

                // PhonePe & Bank Statement Import Feature Card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("statement_import_feature_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "PhonePe & Bank Statement Import",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Parse CSV / PDF / Text with duplicate detection & categorization",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = onNavigateToImport,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth().testTag("launch_import_screen_btn")
                            ) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open Statement & Bank Import Tool")
                            }
                        }
                    }
                }

                // CSV Data Management
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("csv_data_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Data Export & Import",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Export your financial records to CSV or import transactions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { showExportDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.weight(1f).testTag("export_csv_btn")
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export CSV")
                                }

                                Button(
                                    onClick = { showImportDialog = true },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    modifier = Modifier.weight(1f).testTag("import_csv_btn")
                                ) {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Import CSV")
                                }
                            }
                        }
                    }
                }

                // Sample Data & Reset
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sample_reset_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "Demo & Reset Actions",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Button(
                                onClick = {
                                    viewModel.seedSampleData()
                                    Toast.makeText(context, "Populated realistic financial data!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth().testTag("seed_sample_data_btn")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Load Realistic Sample Data", color = MaterialTheme.colorScheme.onSurface)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { showResetDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = FinanceRed.copy(alpha = 0.12f)),
                                modifier = Modifier.fillMaxWidth().testTag("reset_all_data_btn")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = FinanceRed)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Clear All Records", color = FinanceRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Add Bill Dialog
        if (showAddBillDialog) {
            AddBillDialog(
                currency = userSettings.currency,
                onDismiss = { showAddBillDialog = false },
                onSave = { newBill ->
                    viewModel.addBill(newBill)
                    showAddBillDialog = false
                }
            )
        }

        // Add Note Dialog
        if (showAddNoteDialog) {
            AddNoteDialog(
                onDismiss = { showAddNoteDialog = false },
                onSave = { newNote ->
                    viewModel.addNote(newNote)
                    showAddNoteDialog = false
                }
            )
        }

        // Export Dialog
        if (showExportDialog) {
            val csvText = viewModel.exportToCsv()
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                title = { Text("CSV Ledger Export", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Your CSV formatted records are ready to copy:")
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = csvText,
                            onValueChange = {},
                            readOnly = true,
                            maxLines = 8,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("My Finance CSV", csvText))
                            Toast.makeText(context, "Copied CSV to clipboard", Toast.LENGTH_SHORT).show()
                            showExportDialog = false
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy to Clipboard")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showExportDialog = false }) { Text("Close") }
                }
            )
        }

        // Import Dialog
        if (showImportDialog) {
            var importInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                title = { Text("Import CSV Records", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Paste CSV data lines formatted as: Type,Amount,Currency,Category,Description,Account,Date")
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = importInput,
                            onValueChange = { importInput = it },
                            placeholder = { Text("EXPENSE,24.50,$,Food & Dining,Lunch,Cash,2026-10-24") },
                            maxLines = 6,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (importInput.isNotBlank()) {
                                viewModel.importFromCsv(importInput) { result ->
                                    Toast.makeText(context, "Imported ${result.first} rows", Toast.LENGTH_LONG).show()
                                    showImportDialog = false
                                }
                            }
                        }
                    ) {
                        Text("Parse & Import")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Reset Dialog
        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                title = { Text("Clear All Financial Data?", fontWeight = FontWeight.Bold) },
                text = { Text("This will permanently remove all transactions, budgets, goals, and bills from your device.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.clearAllData()
                            showResetDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FinanceRed)
                    ) {
                        Text("Clear Everything")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun BillItemCard(
    bill: Bill,
    currency: String,
    onTogglePaid: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bill_card_${bill.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (bill.isPaidThisMonth) FinanceGreen.copy(alpha = 0.4f) else FinanceRed.copy(alpha = 0.3f)
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(onClick = onTogglePaid) {
                    Icon(
                        imageVector = if (bill.isPaidThisMonth) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline,
                        contentDescription = "Status",
                        tint = if (bill.isPaidThisMonth) FinanceGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column {
                    Text(
                        text = bill.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Due day ${bill.dueDateDay} • ${bill.category} • ${if (bill.isPaidThisMonth) "Paid" else "Pending"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (bill.isPaidThisMonth) FinanceGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "$currency${formatAmount(bill.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NoteCard(
    note: FinancialNote,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("note_card_${note.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = note.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AddBillDialog(
    currency: String,
    onDismiss: () -> Unit,
    onSave: (Bill) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dueDayText by remember { mutableStateOf("1") }
    var category by remember { mutableStateOf("Bills & Utilities") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Recurring Commitment", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Bill / Service Title") },
                    placeholder = { Text("e.g. Fiber Internet, Gym, Rent") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bill_title_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bill_amount_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = dueDayText,
                    onValueChange = { dueDayText = it },
                    label = { Text("Due Day of Month (1 - 31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bill_due_day_input")
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
                    val day = dueDayText.toIntOrNull()
                    if (title.isBlank()) {
                        errorMessage = "Please enter bill title"
                        return@Button
                    }
                    if (amt == null || amt <= 0) {
                        errorMessage = "Please enter valid amount"
                        return@Button
                    }
                    if (day == null || day !in 1..31) {
                        errorMessage = "Due day must be between 1 and 31"
                        return@Button
                    }
                    onSave(
                        Bill(
                            title = title.trim(),
                            amount = amt,
                            category = category,
                            dueDateDay = day
                        )
                    )
                }
            ) {
                Text("Add Bill")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onSave: (FinancialNote) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Strategy Note", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Note Title") },
                    placeholder = { Text("e.g. 50/30/20 Plan, Debt Payoff") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("note_title_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Plan Details / Bullet Points") },
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth().testTag("note_content_input")
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
                    if (title.isBlank() || content.isBlank()) {
                        errorMessage = "Please fill in title and content"
                        return@Button
                    }
                    onSave(FinancialNote(title = title.trim(), content = content.trim()))
                }
            ) {
                Text("Save Note")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
