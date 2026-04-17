package com.controlprestamos.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

data class UserProfileData(
    val photoUri: String = "",
    val name: String = "",
    val lastName: String = "",
    val idNumber: String = "",
    val phone: String = "",
    val communicationPhone: String = "",
    val mobilePaymentPhone: String = "",
    val bankName: String = "",
    val bankAccount: String = "",
    val personalizedMessage: String = "Hola, buenos dÃ­as. Le escribo por el vencimiento de su prÃ©stamo. A continuaciÃ³n le comparto los datos de pago. Muchas gracias."
)

data class ManualLoanData(
    val id: String = UUID.randomUUID().toString(),
    val fullName: String = "",
    val idNumber: String = "",
    val phone: String = "",
    val loanAmount: Double = 0.0,
    val percent: Double = 0.0,
    val loanDate: String = "",
    val dueDate: String = "",
    val exchangeRate: String = "",
    val conditions: String = "",
    val paidAmount: Double = 0.0,
    val status: String = "ACTIVO",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun interestAmount(): Double = loanAmount * (percent / 100.0)
    fun totalAmount(): Double = loanAmount + interestAmount()
    fun pendingAmount(): Double = (totalAmount() - paidAmount).coerceAtLeast(0.0)

    fun isOverdue(): Boolean {
        return try {
            status == "ACTIVO" && LocalDate.parse(dueDate).isBefore(LocalDate.now())
        } catch (_: Exception) {
            false
        }
    }
}

data class BlacklistRecordData(
    val id: String = UUID.randomUUID().toString(),
    val fullName: String = "",
    val idNumber: String = "",
    val phone: String = "",
    val reason: String = "",
    val notes: String = "",
    val addedDate: String = LocalDate.now().toString(),
    val createdAt: Long = System.currentTimeMillis()
)

data class ReferralRecordData(
    val id: String = UUID.randomUUID().toString(),
    val referralDate: String = LocalDate.now().toString(),
    val referredClient: String = "",
    val referredBy: String = "",
    val loanAmount: Double = 0.0,
    val commissionPercent: Double = 10.0,
    val status: String = "PENDIENTE",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun commissionAmount(): Double = loanAmount * (commissionPercent / 100.0)
}

data class FrequentUserPaymentData(
    val id: String = UUID.randomUUID().toString(),
    val fullName: String = "",
    val idNumber: String = "",
    val phone: String = "",
    val bankName: String = "",
    val bankAccount: String = "",
    val mobilePaymentPhone: String = "",
    val paymentAlias: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class LoanPaymentRecordData(
    val id: String = UUID.randomUUID().toString(),
    val loanId: String = "",
    val clientName: String = "",
    val amount: Double = 0.0,
    val paymentDate: String = LocalDate.now().toString(),
    val paymentType: String = "ABONO",
    val previousPaidAmount: Double = 0.0,
    val newPaidAmount: Double = 0.0,
    val previousPendingAmount: Double = 0.0,
    val newPendingAmount: Double = 0.0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)


data class DeletedLoanSnapshotData(
    val trashId: String = UUID.randomUUID().toString(),
    val deletedAt: Long = System.currentTimeMillis(),
    val loan: ManualLoanData = ManualLoanData(),
    val payments: List<LoanPaymentRecordData> = emptyList()
)

data class RestoreDeletedLoanResult(
    val success: Boolean = false,
    val message: String = ""
)

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("ControlPrestamosPrefs", Context.MODE_PRIVATE)

    private val securityStore = SessionSecurityStore(prefs)
    private val profileHistoryStore = SessionProfileHistoryStore(prefs)
    private val catalogStore = SessionCatalogStore(prefs) { appendHistory(it) }
    private val loanStore = SessionLoanStore(prefs) { appendHistory(it) }
    private val backupTrashStore = SessionBackupTrashStore(
        prefs = prefs,
        onHistory = { appendHistory(it) },
        normalizeLoanForPersistenceFn = { loan -> normalizeLoanForPersistence(loan, null) },
        upsertLoanSilentlyFn = { loan -> upsertLoanSilently(loan) },
        saveLoansFn = { loans -> saveLoans(loans) },
        readLoansFn = { readLoans() },
        readLoanPaymentHistoryFn = { loanId -> readLoanPaymentHistory(loanId) },
        readAllLoanPaymentRecordsFn = { readAllLoanPaymentRecords() },
        saveLoanPaymentRecordsFn = { records -> saveLoanPaymentRecords(records) },
        deleteLoanPaymentHistoryForLoanFn = { loanId -> deleteLoanPaymentHistoryForLoan(loanId) },
        readActiveLoanIdFn = { readActiveLoanId() },
        setActiveLoanIdFn = { id -> setActiveLoanId(id) }
    )

    companion object {
        private const val STATUS_ACTIVE = "ACTIVO"
        private const val STATUS_COLLECTED = "COBRADO"
        private const val STATUS_LOST = "PERDIDO"
        private const val TRASH_RETENTION_DAYS = 30
        private const val MAX_TRASH_ITEMS = 150
    }
    fun isDarkMode(): Boolean = securityStore.isDarkMode()
    fun setDarkMode(isDark: Boolean) = securityStore.setDarkMode(isDark)

    fun hasPin(): Boolean = securityStore.hasPin()
    fun savePin(pin: String) = securityStore.savePin(pin)
    fun validatePin(pin: String): Boolean = securityStore.validatePin(pin)
    fun clearPin() = securityStore.clearPin()

    fun isUnlocked(): Boolean = securityStore.isUnlocked()
    fun setUnlocked(value: Boolean) = securityStore.setUnlocked(value)
    fun isPinTemporarilyLocked(): Boolean = securityStore.isPinTemporarilyLocked()
    fun getRemainingPinLockSeconds(): Long = securityStore.getRemainingPinLockSeconds()
    fun registerFailedPinAttempt(): Long = securityStore.registerFailedPinAttempt()
    fun clearPinFailures() = securityStore.clearPinFailures()
    fun saveProfile(data: UserProfileData) = profileHistoryStore.saveProfile(data)
    fun readProfile(): UserProfileData = profileHistoryStore.readProfile()
    fun logout() = profileHistoryStore.logout()
    fun appendHistory(item: String) = profileHistoryStore.appendHistory(item)
    fun readOperationalHistory(): List<String> = profileHistoryStore.readOperationalHistory()
    private fun loansKey(): String = "manual_loans_json"
    private fun loanPaymentsKey(): String = "loan_payment_records_json"
    private fun blacklistKey(): String = "blacklist_records_json"
    private fun referralsKey(): String = "referrals_json"
    private fun frequentUsersKey(): String = "frequent_users_json"

    private fun safeArray(raw: String?): JSONArray {
        return try {
            JSONArray(raw ?: "[]")
        } catch (_: Exception) {
            JSONArray()
        }
    }


    private fun normalizeStatus(pendingAmount: Double, explicitLost: Boolean): String =
        loanStore.normalizeStatus(pendingAmount, explicitLost)

    private fun normalizeLoanForPersistence(
        loan: ManualLoanData,
        existing: ManualLoanData? = null
    ): ManualLoanData = loanStore.normalizeLoanForPersistence(loan, existing)

    private fun saveLoans(loans: List<ManualLoanData>) = loanStore.saveLoans(loans)
    fun readLoans(): List<ManualLoanData> = loanStore.readLoans()
    private fun upsertLoanSilently(loan: ManualLoanData) = loanStore.upsertLoanSilently(loan)
    fun saveLoan(loan: ManualLoanData) = loanStore.saveLoan(loan)
    fun readLoanById(id: String): ManualLoanData? = loanStore.readLoanById(id)
    fun deleteLoan(id: String) = loanStore.deleteLoan(id)
    fun setActiveLoanId(id: String) = loanStore.setActiveLoanId(id)
    fun readActiveLoanId(): String = loanStore.readActiveLoanId()
    fun readActiveLoan(): ManualLoanData? = loanStore.readActiveLoan()

    private fun saveLoanPaymentRecords(records: List<LoanPaymentRecordData>) =
        loanStore.saveLoanPaymentRecords(records)

    private fun readAllLoanPaymentRecords(): List<LoanPaymentRecordData> =
        loanStore.readAllLoanPaymentRecords()

    fun readLoanPaymentHistory(loanId: String): List<LoanPaymentRecordData> =
        loanStore.readLoanPaymentHistory(loanId)

    private fun saveLoanPaymentRecord(record: LoanPaymentRecordData) =
        loanStore.saveLoanPaymentRecord(record)

    private fun deleteLoanPaymentHistoryForLoan(loanId: String) =
        loanStore.deleteLoanPaymentHistoryForLoan(loanId)

    fun registerPayment(loanId: String, paymentAmount: Double): Boolean =
        loanStore.registerPayment(loanId, paymentAmount)

    fun markLoanAsCollected(loanId: String): Boolean =
        loanStore.markLoanAsCollected(loanId)

    fun markLoanAsLost(loanId: String): Boolean =
        loanStore.markLoanAsLost(loanId)
    private fun saveBlacklist(records: List<BlacklistRecordData>) = catalogStore.saveBlacklist(records)
    fun readBlacklist(): List<BlacklistRecordData> = catalogStore.readBlacklist()
    fun saveBlacklistRecord(item: BlacklistRecordData) = catalogStore.saveBlacklistRecord(item)
    fun deleteBlacklistRecord(id: String) = catalogStore.deleteBlacklistRecord(id)

    private fun saveReferralsInternal(records: List<ReferralRecordData>) = catalogStore.saveReferralsInternal(records)
    fun readReferrals(): List<ReferralRecordData> = catalogStore.readReferrals()
    fun saveReferral(item: ReferralRecordData) = catalogStore.saveReferral(item)
    fun deleteReferral(id: String) = catalogStore.deleteReferral(id)

    private fun saveFrequentUsersInternal(users: List<FrequentUserPaymentData>) = catalogStore.saveFrequentUsersInternal(users)
    fun readFrequentUsers(): List<FrequentUserPaymentData> = catalogStore.readFrequentUsers()
    fun saveFrequentUser(user: FrequentUserPaymentData) = catalogStore.saveFrequentUser(user)
    fun deleteFrequentUser(id: String) = catalogStore.deleteFrequentUser(id)
    fun trashRetentionDays(): Int = backupTrashStore.trashRetentionDays()

    fun readDeletedLoans(): List<DeletedLoanSnapshotData> =
        backupTrashStore.readDeletedLoans()

    fun softDeleteLoan(id: String): Boolean =
        backupTrashStore.softDeleteLoan(id)

    fun restoreDeletedLoanDetailed(trashId: String): RestoreDeletedLoanResult =
        backupTrashStore.restoreDeletedLoanDetailed(trashId)

    fun restoreDeletedLoan(trashId: String): Boolean =
        backupTrashStore.restoreDeletedLoan(trashId)

    fun permanentlyDeleteTrashedLoan(trashId: String): Boolean =
        backupTrashStore.permanentlyDeleteTrashedLoan(trashId)
    fun replaceImportedBackupData(
        profile: UserProfileData?,
        loans: List<ManualLoanData>,
        payments: List<LoanPaymentRecordData>,
        blacklist: List<BlacklistRecordData>,
        referrals: List<ReferralRecordData>,
        frequentUsers: List<FrequentUserPaymentData>
    ): Int = backupTrashStore.replaceImportedBackupData(
        profile = profile,
        loans = loans,
        payments = payments,
        blacklist = blacklist,
        referrals = referrals,
        frequentUsers = frequentUsers,
        saveProfileFn = { saveProfile(it) },
        normalizeLoanForPersistenceWithExistingFn = { loan, existing -> normalizeLoanForPersistence(loan, existing) },
        saveBlacklistFn = { saveBlacklist(it) },
        saveReferralsInternalFn = { saveReferralsInternal(it) },
        saveFrequentUsersInternalFn = { saveFrequentUsersInternal(it) }
    )

    fun mergeImportedBackupData(
        profile: UserProfileData?,
        loans: List<ManualLoanData>,
        payments: List<LoanPaymentRecordData>,
        blacklist: List<BlacklistRecordData>,
        referrals: List<ReferralRecordData>,
        frequentUsers: List<FrequentUserPaymentData>
    ): Int = backupTrashStore.mergeImportedBackupData(
        profile = profile,
        loans = loans,
        payments = payments,
        blacklist = blacklist,
        referrals = referrals,
        frequentUsers = frequentUsers,
        saveProfileFn = { saveProfile(it) },
        readProfileFn = { readProfile() },
        mergeProfileDataFn = { current, incoming -> mergeProfileData(current, incoming) },
        normalizeLoanForPersistenceWithExistingFn = { loan, existing -> normalizeLoanForPersistence(loan, existing) },
        saveBlacklistFn = { saveBlacklist(it) },
        readBlacklistFn = { readBlacklist() },
        saveReferralsInternalFn = { saveReferralsInternal(it) },
        readReferralsFn = { readReferrals() },
        saveFrequentUsersInternalFn = { saveFrequentUsersInternal(it) },
        readFrequentUsersFn = { readFrequentUsers() }
    )
    private fun mergeProfileData(
        current: UserProfileData,
        incoming: UserProfileData
    ): UserProfileData {
        return current.copy(
            photoUri = incoming.photoUri.ifBlank { current.photoUri },
            name = incoming.name.ifBlank { current.name },
            lastName = incoming.lastName.ifBlank { current.lastName },
            idNumber = incoming.idNumber.ifBlank { current.idNumber },
            phone = incoming.phone.ifBlank { current.phone },
            communicationPhone = incoming.communicationPhone.ifBlank { current.communicationPhone },
            mobilePaymentPhone = incoming.mobilePaymentPhone.ifBlank { current.mobilePaymentPhone },
            bankName = incoming.bankName.ifBlank { current.bankName },
            bankAccount = incoming.bankAccount.ifBlank { current.bankAccount },
            personalizedMessage = incoming.personalizedMessage.ifBlank { current.personalizedMessage }
        )
    }
}