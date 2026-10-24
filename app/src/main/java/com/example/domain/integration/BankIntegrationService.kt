package com.example.domain.integration

import com.example.domain.parser.ParsedTransaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

enum class BankConnectionStatus {
    DISCONNECTED,
    CONSENT_REQUESTED,
    CONSENT_APPROVED,
    SYNCING,
    SYNC_COMPLETED,
    ERROR
}

data class BankAccountProfile(
    val bankName: String,
    val maskedAccountNumber: String,
    val accountType: String,
    val ifscPrefix: String
)

data class ConsentHandle(
    val handleId: String,
    val providerName: String,
    val consentUrl: String,
    val status: String,
    val expiresAt: Long
)

/**
 * Extensible interface for future authorised bank-data integrations.
 * Follows the official RBI Account Aggregator (AA) and Open Banking framework.
 *
 * Rules:
 * - NEVER scrapes banking portals or third-party apps like PhonePe.
 * - NEVER asks for or stores UPI PINs, net banking passwords, or OTPs.
 * - Transactions are accessed solely through user-consented, cryptographically signed AA data flows.
 */
interface BankIntegrationService {
    val providerName: String
    val isLiveIntegrationActive: Boolean
    val connectionStatus: StateFlow<BankConnectionStatus>

    suspend fun getLinkedAccounts(): List<BankAccountProfile>

    suspend fun initiateConsentRequest(
        userVpaOrMobile: String
    ): Result<ConsentHandle>

    suspend fun checkConsentStatus(handleId: String): Result<Boolean>

    suspend fun fetchAuthorisedTransactions(
        handleId: String,
        fromDate: Long,
        toDate: Long
    ): Result<List<ParsedTransaction>>
}

/**
 * Extensible reference provider following the Account Aggregator specification.
 * In development / offline mode, it explicitly reports that live sync is not claimed
 * and demonstrates the safe consent flow architecture.
 */
class AccountAggregatorReferenceProvider : BankIntegrationService {

    override val providerName: String = "RBI Account Aggregator (AA Framework)"

    // Honestly declare that live sync is inactive without production licensed AA gateway
    override val isLiveIntegrationActive: Boolean = false

    private val _connectionStatus = MutableStateFlow(BankConnectionStatus.DISCONNECTED)
    override val connectionStatus: StateFlow<BankConnectionStatus> = _connectionStatus.asStateFlow()

    override suspend fun getLinkedAccounts(): List<BankAccountProfile> {
        return listOf(
            BankAccountProfile(
                bankName = "State Bank of India",
                maskedAccountNumber = "•••• 4892",
                accountType = "Savings",
                ifscPrefix = "SBIN"
            ),
            BankAccountProfile(
                bankName = "HDFC Bank",
                maskedAccountNumber = "•••• 9104",
                accountType = "Salary",
                ifscPrefix = "HDFC"
            )
        )
    }

    override suspend fun initiateConsentRequest(userVpaOrMobile: String): Result<ConsentHandle> {
        _connectionStatus.value = BankConnectionStatus.CONSENT_REQUESTED
        val handle = ConsentHandle(
            handleId = UUID.randomUUID().toString(),
            providerName = providerName,
            consentUrl = "https://anumati.accountaggregator.in/consent/${UUID.randomUUID()}",
            status = "PENDING_USER_APPROVAL",
            expiresAt = System.currentTimeMillis() + 15 * 60 * 1000
        )
        return Result.success(handle)
    }

    override suspend fun checkConsentStatus(handleId: String): Result<Boolean> {
        return Result.success(true)
    }

    override suspend fun fetchAuthorisedTransactions(
        handleId: String,
        fromDate: Long,
        toDate: Long
    ): Result<List<ParsedTransaction>> {
        _connectionStatus.value = BankConnectionStatus.SYNC_COMPLETED
        return Result.success(emptyList())
    }
}
