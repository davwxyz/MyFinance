package com.example.domain.parser

import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class ParsedTransaction(
    val tempId: String = UUID.randomUUID().toString(),
    val timestamp: Long,
    val rawDescription: String,
    val cleanDescription: String,
    val amount: Double,
    val type: TransactionType,
    val category: String,
    val account: String,
    val referenceId: String,
    val source: String,
    val isDuplicate: Boolean = false,
    val duplicateReason: String? = null,
    val isSelected: Boolean = true
) {
    fun toTransaction(currency: String, batchId: Long): Transaction {
        return Transaction(
            type = type,
            amount = amount,
            currency = currency,
            category = category,
            description = cleanDescription,
            timestamp = timestamp,
            account = account,
            referenceId = referenceId,
            source = source,
            importBatchId = batchId,
            notes = if (rawDescription != cleanDescription) "Original: $rawDescription" else "",
            tags = "#${source.lowercase().replace(" ", "")}"
        )
    }
}

object StatementParser {

    private val DATE_FORMATS = listOf(
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US),
        SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US),
        SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.US),
        SimpleDateFormat("yyyy-MM-dd", Locale.US),
        SimpleDateFormat("dd/MM/yyyy", Locale.US),
        SimpleDateFormat("dd-MM-yyyy", Locale.US),
        SimpleDateFormat("dd MMM yyyy", Locale.US),
        SimpleDateFormat("MMM dd, yyyy", Locale.US),
        SimpleDateFormat("yyyy/MM/dd", Locale.US)
    )

    fun parseStatement(
        content: String,
        fileName: String,
        existingTransactions: List<Transaction>,
        defaultCurrency: String = "$"
    ): List<ParsedTransaction> {
        val lines = content.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return emptyList()

        val lowerFirstLine = lines.first().lowercase()
        val isPhonePe = lowerFirstLine.contains("phonepe") ||
                fileName.lowercase().contains("phonepe") ||
                lines.any { it.contains("PhonePe", ignoreCase = true) || it.contains("UTR", ignoreCase = true) }

        val isStandardBankCsv = lowerFirstLine.contains("narration") ||
                lowerFirstLine.contains("withdrawal") ||
                lowerFirstLine.contains("deposit") ||
                lowerFirstLine.contains("chq/ref no")

        val rawParsed = when {
            isPhonePe -> parsePhonePeFormat(lines, fileName)
            isStandardBankCsv -> parseBankCsvFormat(lines, fileName)
            else -> parseGenericCsvOrText(lines, fileName)
        }

        // Run duplicate detection against existing transactions
        return detectDuplicates(rawParsed, existingTransactions)
    }

    /**
     * Parse PhonePe Statement exports
     * Expected CSV headers or text format:
     * Date,Transaction ID,Description,Type,Amount,UTR
     */
    private fun parsePhonePeFormat(lines: List<String>, fileName: String): List<ParsedTransaction> {
        val results = mutableListOf<ParsedTransaction>()
        var headerIndex = -1

        for (i in lines.indices) {
            val lineLower = lines[i].lowercase()
            if ((lineLower.contains("date") && lineLower.contains("amount")) || lineLower.contains("transaction id")) {
                headerIndex = i
                break
            }
        }

        val dataLines = if (headerIndex >= 0) lines.drop(headerIndex + 1) else lines

        for (line in dataLines) {
            val tokens = splitCsvLine(line)
            if (tokens.size >= 4) {
                // Typical PhonePe format: [Date, TxnId, Description, Type, Amount, UTR]
                // or [Date, Description, Type, Amount]
                val dateStr = tokens[0].trim()
                val parsedTime = parseDate(dateStr)

                var txnId = ""
                var desc = ""
                var typeStr = ""
                var amountVal = 0.0

                if (tokens.size >= 6) {
                    val candidateTxnId = tokens[1].trim()
                    desc = tokens[2].trim()
                    typeStr = tokens[3].trim()
                    amountVal = cleanAmount(tokens[4])
                    val utr = tokens[5].trim()
                    txnId = utr.ifBlank { candidateTxnId }
                } else if (tokens.size == 5) {
                    desc = tokens[1].trim()
                    typeStr = tokens[2].trim()
                    amountVal = cleanAmount(tokens[3])
                    txnId = tokens[4].trim()
                } else {
                    desc = tokens[1].trim()
                    typeStr = tokens[2].trim()
                    amountVal = cleanAmount(tokens[3])
                }

                if (amountVal > 0) {
                    val catResult = TransactionCategorizer.categorize(desc, typeStr, amountVal)
                    results.add(
                        ParsedTransaction(
                            timestamp = parsedTime,
                            rawDescription = desc,
                            cleanDescription = catResult.cleanDescription,
                            amount = amountVal,
                            type = catResult.type,
                            category = catResult.category,
                            account = "PhonePe UPI",
                            referenceId = txnId,
                            source = "PhonePe Statement"
                        )
                    )
                }
            } else if (line.contains("₹") || line.contains("Rs.") || line.contains("Paid to") || line.contains("Received from")) {
                // Text statement fallback
                val parsed = parsePhonePeTextLine(line)
                if (parsed != null) results.add(parsed)
            }
        }

        return results
    }

    /**
     * Parse standard Bank Statement CSV (SBI, HDFC, ICICI, Axis, etc.)
     * Columns: [Txn Date, Narration, Ref No, Withdrawal (Debit), Deposit (Credit), Balance]
     */
    private fun parseBankCsvFormat(lines: List<String>, fileName: String): List<ParsedTransaction> {
        val results = mutableListOf<ParsedTransaction>()
        var headerIndex = 0

        for (i in lines.indices) {
            val lineLower = lines[i].lowercase()
            if (lineLower.contains("narration") || lineLower.contains("description") || lineLower.contains("particulars")) {
                headerIndex = i
                break
            }
        }

        val dataLines = lines.drop(headerIndex + 1)

        for (line in dataLines) {
            val tokens = splitCsvLine(line)
            if (tokens.size >= 5) {
                val dateStr = tokens[0].trim()
                val parsedTime = parseDate(dateStr)
                val narration = tokens[1].trim()
                val refNo = tokens.getOrNull(2)?.trim() ?: ""

                val debitVal = cleanAmount(tokens.getOrNull(3) ?: "")
                val creditVal = cleanAmount(tokens.getOrNull(4) ?: "")

                val isDebit = debitVal > 0
                val amountVal = if (isDebit) debitVal else creditVal

                if (amountVal > 0) {
                    val rawType = if (isDebit) "DEBIT" else "CREDIT"
                    val catResult = TransactionCategorizer.categorize(narration, rawType, amountVal)

                    results.add(
                        ParsedTransaction(
                            timestamp = parsedTime,
                            rawDescription = narration,
                            cleanDescription = catResult.cleanDescription,
                            amount = amountVal,
                            type = catResult.type,
                            category = catResult.category,
                            account = "Bank Account",
                            referenceId = refNo,
                            source = "Bank Statement"
                        )
                    )
                }
            }
        }

        return results
    }

    /**
     * Parse generic CSV or pasted statement text
     */
    private fun parseGenericCsvOrText(lines: List<String>, fileName: String): List<ParsedTransaction> {
        val results = mutableListOf<ParsedTransaction>()

        val isHeaderPresent = lines.first().lowercase().let {
            it.contains("amount") || it.contains("type") || it.contains("date")
        }
        val dataLines = if (isHeaderPresent) lines.drop(1) else lines

        for (line in dataLines) {
            val tokens = splitCsvLine(line)
            if (tokens.size >= 3) {
                // Check if line looks like: Date, Description, Amount
                val dateStr = tokens[0].trim()
                val parsedTime = parseDate(dateStr)
                var desc = tokens[1].trim()
                var amt = cleanAmount(tokens[2])
                var typeStr = ""
                var cat = "General"
                var ref = ""

                if (tokens.size >= 5) {
                    // Type, Amount, Category, Desc
                    if (tokens[1].equals("EXPENSE", true) || tokens[1].equals("INCOME", true) || tokens[1].equals("TRANSFER", true) || tokens[1].equals("REFUND", true)) {
                        typeStr = tokens[1]
                        amt = cleanAmount(tokens[2])
                        cat = tokens.getOrNull(4) ?: "General"
                        desc = tokens.getOrNull(5) ?: tokens[1]
                        ref = tokens.getOrNull(6) ?: ""
                    }
                }

                if (amt > 0) {
                    val catResult = TransactionCategorizer.categorize(desc, typeStr, amt)
                    results.add(
                        ParsedTransaction(
                            timestamp = parsedTime,
                            rawDescription = desc,
                            cleanDescription = catResult.cleanDescription,
                            amount = amt,
                            type = catResult.type,
                            category = if (cat != "General") cat else catResult.category,
                            account = "Imported",
                            referenceId = ref,
                            source = if (fileName.contains(".pdf", true)) "PDF Statement" else "CSV Statement"
                        )
                    )
                }
            }
        }

        return results
    }

    private fun parsePhonePeTextLine(line: String): ParsedTransaction? {
        try {
            // Match pattern like: "Paid to Swiggy ₹ 340 on 24 Oct 2026 UTR: 12345678"
            val amtRegex = Regex("""(?:₹|Rs\.?)\s*([\d,]+(?:\.\d{1,2})?)""")
            val amtMatch = amtRegex.find(line) ?: return null
            val amt = amtMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null

            val utrRegex = Regex("""(?:UTR|Txn ID|Ref):\s*([A-Za-z0-9]+)""")
            val utr = utrRegex.find(line)?.groupValues?.get(1) ?: ""

            val isCredit = line.contains("Received from", true) || line.contains("Refund from", true) || line.contains("Cashback", true)
            val desc = line.substringBefore("₹").substringBefore("Rs.").trim()

            val catResult = TransactionCategorizer.categorize(desc, if (isCredit) "CREDIT" else "DEBIT", amt)

            return ParsedTransaction(
                timestamp = System.currentTimeMillis(),
                rawDescription = desc,
                cleanDescription = catResult.cleanDescription,
                amount = amt,
                type = catResult.type,
                category = catResult.category,
                account = "PhonePe UPI",
                referenceId = utr,
                source = "PhonePe Statement"
            )
        } catch (_: Exception) {
            return null
        }
    }

    /**
     * Duplicate detection:
     * 1. Matches referenceId if non-blank.
     * 2. Matches exact amount + type within a 24-hour timestamp window.
     */
    fun detectDuplicates(
        parsedList: List<ParsedTransaction>,
        existingList: List<Transaction>
    ): List<ParsedTransaction> {
        val windowMillis = 24L * 60 * 60 * 1000

        return parsedList.map { parsed ->
            var isDuplicate = false
            var reason: String? = null

            // 1. Check referenceId
            if (parsed.referenceId.isNotBlank()) {
                val match = existingList.firstOrNull { it.referenceId == parsed.referenceId }
                if (match != null) {
                    isDuplicate = true
                    reason = "Duplicate UTR/Reference ID: ${parsed.referenceId}"
                }
            }

            // 2. Check amount + timestamp window
            if (!isDuplicate) {
                val match = existingList.firstOrNull { existing ->
                    val sameAmount = Math.abs(existing.amount - parsed.amount) < 0.01
                    val sameType = existing.type == parsed.type
                    val closeTime = Math.abs(existing.timestamp - parsed.timestamp) <= windowMillis
                    val descSimilar = existing.description.equals(parsed.cleanDescription, ignoreCase = true) ||
                            existing.description.contains(parsed.cleanDescription, ignoreCase = true) ||
                            parsed.cleanDescription.contains(existing.description, ignoreCase = true)

                    sameAmount && sameType && (closeTime || descSimilar)
                }

                if (match != null) {
                    isDuplicate = true
                    reason = "Matches existing record '${match.description}' ($${match.amount})"
                }
            }

            parsed.copy(
                isDuplicate = isDuplicate,
                duplicateReason = reason,
                isSelected = !isDuplicate // default unselected if duplicate!
            )
        }
    }

    private fun splitCsvLine(line: String): List<String> {
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
            .map { it.trim().trim('\"') }
    }

    private fun cleanAmount(raw: String): Double {
        val cleaned = raw.replace("₹", "").replace("$", "").replace("Rs.", "").replace(",", "").trim()
        return cleaned.toDoubleOrNull() ?: 0.0
    }

    private fun parseDate(dateStr: String): Long {
        if (dateStr.isBlank()) return System.currentTimeMillis()
        for (format in DATE_FORMATS) {
            try {
                val d = format.parse(dateStr)
                if (d != null) return d.time
            } catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }
}
