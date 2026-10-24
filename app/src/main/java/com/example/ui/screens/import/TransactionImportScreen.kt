package com.example.ui.screens.import

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ImportHistory
import com.example.data.model.TransactionType
import com.example.domain.integration.AccountAggregatorReferenceProvider
import com.example.domain.parser.ParsedTransaction
import com.example.ui.components.EmptyStateView
import com.example.ui.components.SectionHeader
import com.example.ui.components.formatAmount
import com.example.ui.components.resolveCategoryColor
import com.example.ui.components.resolveCategoryIcon
import com.example.ui.screens.transactions.DEFAULT_EXPENSE_CATEGORIES
import com.example.ui.screens.transactions.DEFAULT_INCOME_CATEGORIES
import com.example.ui.theme.FinanceAmber
import com.example.ui.theme.FinanceAmberContainer
import com.example.ui.theme.FinanceGreen
import com.example.ui.theme.FinanceGreenContainer
import com.example.ui.theme.FinanceRed
import com.example.ui.theme.FinanceRedContainer
import com.example.ui.viewmodel.FinanceViewModel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TransactionImportScreen(
    viewModel: FinanceViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Import & Preview, 1: History & Deletion, 2: Bank Connect

    val previewList by viewModel.importedPreviewList.collectAsStateWithLifecycle()
    val importFileName by viewModel.importFileName.collectAsStateWithLifecycle()
    val isParsing by viewModel.isParsing.collectAsStateWithLifecycle()
    val importHistory by viewModel.allImportHistory.collectAsStateWithLifecycle()
    val userSettings by viewModel.userSettings.collectAsStateWithLifecycle()

    var showPasteDialog by remember { mutableStateOf(false) }
    var selectedTypeFilter by remember { mutableStateOf<TransactionType?>(null) }
    var deletingHistoryItem by remember { mutableStateOf<ImportHistory?>(null) }

    // File picker launcher using Android Storage Access Framework
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val content = reader.use { it.readText() }
                val fileName = uri.lastPathSegment?.substringAfterLast("/") ?: "Imported_Statement.csv"
                viewModel.parseStatementContent(content, fileName)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to read file: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("transaction_import_screen")
    ) {
        // Navigation Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.testTag("import_back_btn")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Statement & Bank Import",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "PhonePe, UPI, and Bank Statement Analysis",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Three Mode Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Import & Preview", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_import_preview")
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Import History", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_import_history")
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Bank Connect", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("tab_bank_connect")
            )
        }

        when (selectedTab) {
            0 -> {
                // Tab 0: Import and Preview
                if (previewList.isEmpty()) {
                    ImportLandingView(
                        onPickFile = {
                            filePickerLauncher.launch(arrayOf("*/*", "text/*", "text/comma-separated-values", "application/pdf"))
                        },
                        onPasteText = { showPasteDialog = true },
                        onLoadSamplePhonePe = {
                            viewModel.parseStatementContent(SAMPLE_PHONEPE_STATEMENT, "PhonePe_Statement_Oct2026.csv")
                        },
                        onLoadSampleBank = {
                            viewModel.parseStatementContent(SAMPLE_BANK_CSV_STATEMENT, "Bank_Account_Statement_Oct2026.csv")
                        }
                    )
                } else {
                    ImportPreviewView(
                        previewList = previewList,
                        fileName = importFileName,
                        currency = userSettings.currency,
                        selectedTypeFilter = selectedTypeFilter,
                        onTypeFilterChange = { selectedTypeFilter = it },
                        onToggleSelect = { viewModel.toggleParsedTransactionSelection(it) },
                        onSelectAll = { viewModel.selectAllParsed(it) },
                        onExcludeDuplicates = { viewModel.excludeDuplicatesFromImport() },
                        onCategoryChange = { id, cat -> viewModel.updateParsedCategory(id, cat) },
                        onTypeChange = { id, t -> viewModel.updateParsedType(id, t) },
                        onCancel = { viewModel.clearImportPreview() },
                        onConfirm = {
                            viewModel.confirmImport {
                                onNavigateBack()
                            }
                        }
                    )
                }
            }

            1 -> {
                // Tab 1: Import History and Deletion Controls
                ImportHistoryListView(
                    historyList = importHistory,
                    onDeleteBatch = { deletingHistoryItem = it }
                )
            }

            2 -> {
                // Tab 2: Extensible Authorised Bank Connect Architecture
                BankConnectInfoView()
            }
        }

        // Paste Statement Dialog
        if (showPasteDialog) {
            var pastedText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showPasteDialog = false },
                title = { Text("Paste Statement Text", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = "Paste text copied from your PhonePe statement or bank export:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = pastedText,
                            onValueChange = { pastedText = it },
                            placeholder = { Text("Date, Transaction ID, Details, Type, Amount\n2026-10-24, T1234567, Paid to Swiggy, DEBIT, 240.00") },
                            maxLines = 8,
                            modifier = Modifier.fillMaxWidth().testTag("paste_statement_input")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (pastedText.isNotBlank()) {
                                viewModel.parseStatementContent(pastedText, "Pasted_Statement.txt")
                                showPasteDialog = false
                            }
                        },
                        modifier = Modifier.testTag("parse_pasted_btn")
                    ) {
                        Text("Parse Statement")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showPasteDialog = false }) { Text("Cancel") }
                }
            )
        }

        // Delete Batch Dialog
        if (deletingHistoryItem != null) {
            AlertDialog(
                onDismissRequest = { deletingHistoryItem = null },
                title = { Text("Delete Imported Batch?", fontWeight = FontWeight.Bold) },
                text = {
                    Text("This will remove all ${deletingHistoryItem!!.totalImported} transactions imported from '${deletingHistoryItem!!.fileName}' from your device.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteImportBatch(deletingHistoryItem!!.id)
                            deletingHistoryItem = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = FinanceRed)
                    ) {
                        Text("Delete Batch")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deletingHistoryItem = null }) { Text("Cancel") }
                }
            )
        }
    }
}

@Composable
fun ImportLandingView(
    onPickFile: () -> Unit,
    onPasteText: () -> Unit,
    onLoadSamplePhonePe: () -> Unit,
    onLoadSampleBank: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("import_landing_view"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Privacy Guarantee Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("privacy_guarantee_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = FinanceGreenContainer.copy(alpha = 0.6f)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(FinanceGreen.copy(alpha = 0.4f)))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(FinanceGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = FinanceGreen,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "100% Offline & Private Analysis",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Parsed entirely on your device. We never request UPI PINs, banking passwords, or scrape apps. Zero credentials required.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Upload Actions
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("upload_actions_card"),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Choose Statement Source",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Select an exported statement file (CSV, TXT, or text-based PDF)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onPickFile,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().testTag("pick_statement_file_btn")
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Statement File (CSV / PDF / TXT)")
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onPasteText,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().testTag("paste_statement_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Paste Statement Text")
                    }
                }
            }
        }

        // Instant Demonstration Statements
        item {
            SectionHeader(title = "Instant Interactive Demos")

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("demo_statements_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Try with realistic PhonePe & Bank exports:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onLoadSamplePhonePe,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth().testTag("load_sample_phonepe_btn")
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Load Sample PhonePe Statement (UPI, Swiggy, Refunds)")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = onLoadSampleBank,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("load_sample_bank_btn")
                    ) {
                        Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Load Sample Bank Statement (Salary, NEFT, Groceries)")
                    }
                }
            }
        }
    }
}

@Composable
fun ImportPreviewView(
    previewList: List<ParsedTransaction>,
    fileName: String,
    currency: String,
    selectedTypeFilter: TransactionType?,
    onTypeFilterChange: (TransactionType?) -> Unit,
    onToggleSelect: (String) -> Unit,
    onSelectAll: (Boolean) -> Unit,
    onExcludeDuplicates: () -> Unit,
    onCategoryChange: (String, String) -> Unit,
    onTypeChange: (String, TransactionType) -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedCount = previewList.count { it.isSelected }
    val duplicateCount = previewList.count { it.isDuplicate }
    val selectedAmount = previewList.filter { it.isSelected }.sumOf {
        if (it.type == TransactionType.INCOME || it.type == TransactionType.REFUND) it.amount else -it.amount
    }

    val filteredList = if (selectedTypeFilter != null) {
        previewList.filter { it.type == selectedTypeFilter }
    } else {
        previewList
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("import_preview_view")
    ) {
        // Summary & Actions Header Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("preview_summary_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fileName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${previewList.size} parsed • $selectedCount selected to import",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (duplicateCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = FinanceAmberContainer
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = FinanceAmber, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "$duplicateCount Duplicates",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = FinanceAmber
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick Filter & Selection Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onSelectAll(true) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).testTag("select_all_btn")
                    ) {
                        Text("Select All", style = MaterialTheme.typography.labelSmall)
                    }

                    if (duplicateCount > 0) {
                        Button(
                            onClick = onExcludeDuplicates,
                            colors = ButtonDefaults.buttonColors(containerColor = FinanceAmber),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.weight(1.2f).testTag("exclude_duplicates_btn")
                        ) {
                            Text("Exclude Duplicates", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    OutlinedButton(
                        onClick = { onSelectAll(false) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f).testTag("deselect_all_btn")
                    ) {
                        Text("Clear All", style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Type filter chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedTypeFilter == null,
                        onClick = { onTypeFilterChange(null) },
                        label = { Text("All (${previewList.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )

                    TransactionType.values().forEach { t ->
                        val count = previewList.count { it.type == t }
                        if (count > 0) {
                            FilterChip(
                                selected = selectedTypeFilter == t,
                                onClick = { onTypeFilterChange(if (selectedTypeFilter == t) null else t) },
                                label = { Text("${t.displayName} ($count)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }
            }
        }

        // Preview Items List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            items(filteredList, key = { it.tempId }) { item ->
                ParsedTransactionCard(
                    item = item,
                    currency = currency,
                    onToggleSelect = { onToggleSelect(item.tempId) },
                    onCategoryChange = { onCategoryChange(item.tempId, it) },
                    onTypeChange = { onTypeChange(item.tempId, it) }
                )
            }
        }

        // Bottom Action Confirmation Bar
        Surface(
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.testTag("discard_import_btn")
                ) {
                    Text("Discard")
                }

                Button(
                    onClick = onConfirm,
                    enabled = selectedCount > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("confirm_import_btn")
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Confirm & Save ($selectedCount)")
                }
            }
        }
    }
}

@Composable
fun ParsedTransactionCard(
    item: ParsedTransaction,
    currency: String,
    onToggleSelect: () -> Unit,
    onCategoryChange: (String) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCategoryMenu by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.US) }
    val isCredit = item.type == TransactionType.INCOME || item.type == TransactionType.REFUND
    val sign = if (isCredit) "+" else "-"
    val amountColor = when (item.type) {
        TransactionType.INCOME -> FinanceGreen
        TransactionType.REFUND -> FinanceAmber
        TransactionType.TRANSFER -> MaterialTheme.colorScheme.primary
        TransactionType.EXPENSE -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("parsed_card_${item.tempId}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isDuplicate) FinanceAmberContainer.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (item.isDuplicate) FinanceAmber.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isSelected,
                    onCheckedChange = { onToggleSelect() },
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("checkbox_${item.tempId}")
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.cleanDescription,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${dateFormat.format(Date(item.timestamp))} • ${item.account}${if (item.referenceId.isNotBlank()) " • Ref: ${item.referenceId}" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "$sign$currency${formatAmount(item.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = amountColor
                )
            }

            // Duplicate Alert badge
            if (item.isDuplicate) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = FinanceAmberContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = FinanceAmber, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = item.duplicateReason ?: "Potential duplicate entry",
                            style = MaterialTheme.typography.labelSmall,
                            color = FinanceAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Inline Category & Type correction controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category correction chip
                Box {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showCategoryMenu = true }
                            .testTag("change_category_btn_${item.tempId}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(resolveCategoryColor(item.category))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${item.category} ▾",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        val categories = DEFAULT_EXPENSE_CATEGORIES + DEFAULT_INCOME_CATEGORIES
                        categories.distinct().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    onCategoryChange(cat)
                                    showCategoryMenu = false
                                }
                            )
                        }
                    }
                }

                // Type correction chip
                Box {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = amountColor.copy(alpha = 0.12f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showTypeMenu = true }
                            .testTag("change_type_btn_${item.tempId}")
                    ) {
                        Text(
                            text = "${item.type.displayName} ▾",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = amountColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showTypeMenu,
                        onDismissRequest = { showTypeMenu = false }
                    ) {
                        TransactionType.values().forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t.displayName) },
                                onClick = {
                                    onTypeChange(t)
                                    showTypeMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportHistoryListView(
    historyList: List<ImportHistory>,
    onDeleteBatch: (ImportHistory) -> Unit,
    modifier: Modifier = Modifier
) {
    if (historyList.isEmpty()) {
        EmptyStateView(
            icon = Icons.Default.History,
            title = "No Import History Yet",
            message = "When you import PhonePe statements or bank CSVs, the records and batches are logged here so you can review or rollback anytime.",
            modifier = modifier.padding(top = 32.dp)
        )
    } else {
        val dateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US) }
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SectionHeader(title = "Past Import Batches")
            }

            items(historyList, key = { it.id }) { history ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_item_${history.id}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
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
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (history.source.contains("pdf", true)) Icons.Default.PictureAsPdf else Icons.Default.AccountBalance,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = history.fileName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${history.source} • ${dateFormat.format(Date(history.importedAt))}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${history.totalImported} imported • ${history.totalDuplicates} duplicates filtered",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = FinanceGreen
                                )
                            }
                        }

                        IconButton(
                            onClick = { onDeleteBatch(history) },
                            modifier = Modifier.testTag("delete_batch_btn_${history.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Batch",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BankConnectInfoView(modifier: Modifier = Modifier) {
    val provider = remember { AccountAggregatorReferenceProvider() }
    val accounts = remember { mutableStateOf<List<com.example.domain.integration.BankAccountProfile>>(emptyList()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("bank_connect_view"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status & Compliance Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Authorised Bank Connect (RBI AA)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "My Finance implements the extensible interface for India's RBI Account Aggregator (AA) framework. Direct app scraping and requesting UPI PINs or netbanking passwords are strictly prohibited by security standards.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "Status: Authorised Integration Interface Provisioned (Sandbox Mode)",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Available AA Bank Profiles
        item {
            SectionHeader(title = "Sample Account Aggregator Profiles")

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Pair("State Bank of India", "Savings Account •••• 4892"),
                    Pair("HDFC Bank", "Salary Account •••• 9104"),
                    Pair("ICICI Bank", "Current Account •••• 1278")
                ).forEach { (bank, account) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(bank, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                                    Text(account, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = FinanceGreenContainer
                            ) {
                                Text(
                                    text = "Ready",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = FinanceGreen,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sample Data for instant demonstration
private val SAMPLE_PHONEPE_STATEMENT = """
Date,Transaction ID,Details,Type,Amount,UTR
2026-10-24,T261024123456,Paid to Swiggy,DEBIT,340.00,429810234561
2026-10-23,T261023987654,Refund from Swiggy for Order #123,CREDIT,120.00,429710987654
2026-10-22,T261022456789,Paid to Blinkit Grocery,DEBIT,485.50,429610456789
2026-10-21,T261021789012,Paid to Uber India,DEBIT,185.00,429510789012
2026-10-20,T261020345678,Paid to BESCOM Electricity Bill,DEBIT,720.00,429410345678
2026-10-19,T261019654321,Transfer to Self SBI Account,DEBIT,1500.00,429310654321
2026-10-18,T261018901234,Cashback from PhonePe,CREDIT,25.00,429210901234
2026-10-17,T261017567890,Paid to Netflix Subscription,DEBIT,649.00,429110567890
2026-10-16,T261016234567,Paid to Starbucks Coffee,DEBIT,290.00,429010234567
""".trimIndent()

private val SAMPLE_BANK_CSV_STATEMENT = """
Txn Date,Narration,Chq/Ref No,Withdrawal,Deposit,Balance
2026-10-24,SALARY CR/OCTOBER/TECH CORP,REF89410294,,55000.00,72450.00
2026-10-23,POS/AMAZON INDIA RETAIL/BANGALORE,429810456,1890.00,,70560.00
2026-10-22,UPI/APOLLO PHARMACY/MEDICINES,429710123,430.00,,70130.00
2026-10-21,UPI/REVERSAL FAILED TRANSACTION,REF328109,,430.00,70560.00
2026-10-20,NEFT/APARTMENT RENT/OCTOBER,NEFT981203,15000.00,,55560.00
2026-10-19,POS/SHELL FUEL STATION/PETROL,429510890,1200.00,,54360.00
2026-10-18,UPI/INSTAMART GROCERIES/BANGALORE,429410654,620.00,,53740.00
""".trimIndent()
