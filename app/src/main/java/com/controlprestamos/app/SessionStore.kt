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
    val personalizedMessage: String = "Hola, buenos días. Le escribo por el vencimiento de su préstamo. A continuación le comparto los datos de pago. Muchas gracias."
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
    private fun deletedLoansKey(): String = "deleted_loans_json"

    private fun saveDeletedLoanSnapshots(records: List<DeletedLoanSnapshotData>) {
        val array = JSONArray()

        records.forEach { record ->
            val paymentsArray = JSONArray()
            record.payments.forEach { payment ->
                paymentsArray.put(
                    JSONObject().apply {
                        put("id", payment.id)
                        put("loanId", payment.loanId)
                        put("clientName", payment.clientName)
                        put("amount", payment.amount)
                        put("paymentDate", payment.paymentDate)
                        put("paymentType", payment.paymentType)
                        put("previousPaidAmount", payment.previousPaidAmount)
                        put("newPaidAmount", payment.newPaidAmount)
                        put("previousPendingAmount", payment.previousPendingAmount)
                        put("newPendingAmount", payment.newPendingAmount)
                        put("note", payment.note)
                        put("createdAt", payment.createdAt)
                    }
                )
            }

            val loan = record.loan

            array.put(
                JSONObject().apply {
                    put("trashId", record.trashId)
                    put("deletedAt", record.deletedAt)
                    put(
                        "loan",
                        JSONObject().apply {
                            put("id", loan.id)
                            put("fullName", loan.fullName)
                            put("idNumber", loan.idNumber)
                            put("phone", loan.phone)
                            put("loanAmount", loan.loanAmount)
                            put("percent", loan.percent)
                            put("loanDate", loan.loanDate)
                            put("dueDate", loan.dueDate)
                            put("exchangeRate", loan.exchangeRate)
                            put("conditions", loan.conditions)
                            put("paidAmount", loan.paidAmount)
                            put("status", loan.status)
                            put("createdAt", loan.createdAt)
                        }
                    )
                    put("payments", paymentsArray)
                }
            )
        }

        prefs.edit().putString(deletedLoansKey(), array.toString()).apply()
    }

    private fun readDeletedLoanSnapshots(): List<DeletedLoanSnapshotData> {
        val array = safeArray(prefs.getString(deletedLoansKey(), "[]"))
        val result = mutableListOf<DeletedLoanSnapshotData>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val loanObj = obj.optJSONObject("loan") ?: JSONObject()
            val paymentsArray = obj.optJSONArray("payments") ?: JSONArray()

            val payments = mutableListOf<LoanPaymentRecordData>()
            for (j in 0 until paymentsArray.length()) {
                val paymentObj = paymentsArray.optJSONObject(j) ?: continue
                payments.add(
                    LoanPaymentRecordData(
                        id = paymentObj.optString("id").ifBlank { UUID.randomUUID().toString() },
                        loanId = paymentObj.optString("loanId"),
                        clientName = paymentObj.optString("clientName"),
                        amount = paymentObj.optDouble("amount", 0.0),
                        paymentDate = paymentObj.optString("paymentDate", LocalDate.now().toString()),
                        paymentType = paymentObj.optString("paymentType", "ABONO").ifBlank { "ABONO" },
                        previousPaidAmount = paymentObj.optDouble("previousPaidAmount", 0.0),
                        newPaidAmount = paymentObj.optDouble("newPaidAmount", 0.0),
                        previousPendingAmount = paymentObj.optDouble("previousPendingAmount", 0.0),
                        newPendingAmount = paymentObj.optDouble("newPendingAmount", 0.0),
                        note = paymentObj.optString("note"),
                        createdAt = paymentObj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }

            val loan = normalizeLoanForPersistence(
                ManualLoanData(
                    id = loanObj.optString("id").ifBlank { UUID.randomUUID().toString() },
                    fullName = loanObj.optString("fullName"),
                    idNumber = loanObj.optString("idNumber"),
                    phone = loanObj.optString("phone"),
                    loanAmount = loanObj.optDouble("loanAmount", 0.0),
                    percent = loanObj.optDouble("percent", 0.0),
                    loanDate = loanObj.optString("loanDate"),
                    dueDate = loanObj.optString("dueDate"),
                    exchangeRate = loanObj.optString("exchangeRate"),
                    conditions = loanObj.optString("conditions"),
                    paidAmount = loanObj.optDouble("paidAmount", 0.0),
                    status = loanObj.optString("status", STATUS_ACTIVE),
                    createdAt = loanObj.optLong("createdAt", System.currentTimeMillis())
                )
            )

            result.add(
                DeletedLoanSnapshotData(
                    trashId = obj.optString("trashId").ifBlank { UUID.randomUUID().toString() },
                    deletedAt = obj.optLong("deletedAt", System.currentTimeMillis()),
                    loan = loan,
                    payments = payments.sortedByDescending { it.createdAt }.distinctBy { it.id }
                )
            )
        }

        return result
            .sortedByDescending { it.deletedAt }
            .distinctBy { it.trashId }
    }

    fun trashRetentionDays(): Int = TRASH_RETENTION_DAYS

    private fun purgeExpiredDeletedLoans(): Int {
        val current = readDeletedLoanSnapshots()
        if (current.isEmpty()) return 0

        val retentionMillis = TRASH_RETENTION_DAYS.toLong() * 24L * 60L * 60L * 1000L
        val cutoff = System.currentTimeMillis() - retentionMillis

        val kept = current.filter { it.deletedAt >= cutoff }
        val removedCount = current.size - kept.size

        if (removedCount > 0) {
            saveDeletedLoanSnapshots(kept)
            appendHistory("Papelera depurada: $removedCount registro(s) vencido(s)")
        }

        return removedCount
    }

    private fun hasPotentialRestoreDuplicate(candidate: ManualLoanData): Boolean {
        return readLoans().any { existing ->
            existing.id != candidate.id &&
                existing.loanDate == candidate.loanDate &&
                existing.dueDate == candidate.dueDate &&
                existing.loanAmount == candidate.loanAmount &&
                existing.percent == candidate.percent &&
                (
                    existing.fullName.equals(candidate.fullName, ignoreCase = true) ||
                    (candidate.idNumber.isNotBlank() && existing.idNumber == candidate.idNumber) ||
                    (candidate.phone.isNotBlank() && existing.phone == candidate.phone)
                )
        }
    }

    fun readDeletedLoans(): List<DeletedLoanSnapshotData> {
        purgeExpiredDeletedLoans()
        return readDeletedLoanSnapshots()
    }

    fun softDeleteLoan(id: String): Boolean {
        purgeExpiredDeletedLoans()

        val current = readLoans().toMutableList()
        val removed = current.firstOrNull { it.id == id } ?: return false
        val removedPayments = readLoanPaymentHistory(id)

        val deleted = readDeletedLoanSnapshots()
            .filterNot { it.loan.id == id }
            .toMutableList()

        deleted.add(
            0,
            DeletedLoanSnapshotData(
                loan = removed,
                payments = removedPayments
            )
        )

        saveDeletedLoanSnapshots(deleted.take(MAX_TRASH_ITEMS))

        current.removeAll { it.id == id }
        saveLoans(current)
        deleteLoanPaymentHistoryForLoan(id)

        if (readActiveLoanId() == id) {
            setActiveLoanId("")
        }

        appendHistory("Préstamo enviado a papelera: ${removed.fullName}")
        return true
    }

    fun restoreDeletedLoanDetailed(trashId: String): RestoreDeletedLoanResult {
        purgeExpiredDeletedLoans()

        val deleted = readDeletedLoanSnapshots().toMutableList()
        val index = deleted.indexOfFirst { it.trashId == trashId }
        if (index < 0) {
            return RestoreDeletedLoanResult(
                success = false,
                message = "El registro ya no existe en la papelera o venció su tiempo de retención."
            )
        }

        val snapshot = deleted[index]

        if (hasPotentialRestoreDuplicate(snapshot.loan)) {
            return RestoreDeletedLoanResult(
                success = false,
                message = "Ya existe un préstamo muy parecido activo. Revisa la cartera antes de restaurar para evitar duplicados."
            )
        }

        val currentLoans = readLoans()
        val loanIdAlreadyExists = currentLoans.any { it.id == snapshot.loan.id }
        val restoredLoanId = if (loanIdAlreadyExists) UUID.randomUUID().toString() else snapshot.loan.id
        val restoredLoan = snapshot.loan.copy(id = restoredLoanId)

        val existingPaymentIds = readAllLoanPaymentRecords().map { it.id }.toMutableSet()
        val restoredPayments = snapshot.payments.map { payment ->
            var paymentId = payment.id
            while (existingPaymentIds.contains(paymentId)) {
                paymentId = UUID.randomUUID().toString()
            }
            existingPaymentIds.add(paymentId)

            payment.copy(
                id = paymentId,
                loanId = restoredLoanId
            )
        }

        upsertLoanSilently(restoredLoan)

        val mergedPayments = (restoredPayments + readAllLoanPaymentRecords())
            .sortedByDescending { it.createdAt }
            .distinctBy { it.id }

        saveLoanPaymentRecords(mergedPayments)

        deleted.removeAt(index)
        saveDeletedLoanSnapshots(deleted)

        if (loanIdAlreadyExists) {
            appendHistory("Préstamo restaurado: ${restoredLoan.fullName} (nuevo identificador)")
            return RestoreDeletedLoanResult(
                success = true,
                message = "Préstamo restaurado correctamente. Se asignó un nuevo identificador interno para evitar conflicto con uno existente."
            )
        }

        appendHistory("Préstamo restaurado: ${restoredLoan.fullName}")
        return RestoreDeletedLoanResult(
            success = true,
            message = "Préstamo restaurado correctamente."
        )
    }

    fun restoreDeletedLoan(trashId: String): Boolean {
        return restoreDeletedLoanDetailed(trashId).success
    }

    fun permanentlyDeleteTrashedLoan(trashId: String): Boolean {
        purgeExpiredDeletedLoans()

        val deleted = readDeletedLoanSnapshots().toMutableList()
        val index = deleted.indexOfFirst { it.trashId == trashId }
        if (index < 0) return false

        val snapshot = deleted[index]
        deleted.removeAt(index)
        saveDeletedLoanSnapshots(deleted)

        appendHistory("Préstamo eliminado definitivamente: ${snapshot.loan.fullName}")
        return true
    }

    fun replaceImportedBackupData(
        profile: UserProfileData?,
        loans: List<ManualLoanData>,
        payments: List<LoanPaymentRecordData>,
        blacklist: List<BlacklistRecordData>,
        referrals: List<ReferralRecordData>,
        frequentUsers: List<FrequentUserPaymentData>
    ): Int {
        profile?.let { saveProfile(it) }

        val normalizedLoans = loans
            .map { normalizeLoanForPersistence(it) }
            .sortedByDescending { it.createdAt }
            .distinctBy { it.id }

        saveLoans(normalizedLoans)

        val validLoanIds = normalizedLoans.map { it.id }.toSet()

        val cleanPayments = payments
            .filter { it.loanId.isNotBlank() && it.loanId in validLoanIds }
            .sortedByDescending { it.createdAt }
            .distinctBy { it.id }

        saveLoanPaymentRecords(cleanPayments)
        saveBlacklist(blacklist.sortedByDescending { it.createdAt }.distinctBy { it.id })
        saveReferralsInternal(referrals.sortedByDescending { it.createdAt }.distinctBy { it.id })
        saveFrequentUsersInternal(frequentUsers.sortedByDescending { it.createdAt }.distinctBy { it.id })

        val activeId = readActiveLoanId()
        if (activeId.isNotBlank() && activeId !in validLoanIds) {
            setActiveLoanId("")
        }

        appendHistory("Respaldo restaurado en modo reemplazo")

        return normalizedLoans.size +
            cleanPayments.size +
            blacklist.distinctBy { it.id }.size +
            referrals.distinctBy { it.id }.size +
            frequentUsers.distinctBy { it.id }.size +
            if (profile != null) 1 else 0
    }

    fun mergeImportedBackupData(
        profile: UserProfileData?,
        loans: List<ManualLoanData>,
        payments: List<LoanPaymentRecordData>,
        blacklist: List<BlacklistRecordData>,
        referrals: List<ReferralRecordData>,
        frequentUsers: List<FrequentUserPaymentData>
    ): Int {
        profile?.let {
            saveProfile(mergeProfileData(readProfile(), it))
        }

        val mergedLoansBase = (loans + readLoans())
            .sortedByDescending { it.createdAt }
            .distinctBy { it.id }

        val normalizedLoans = mergedLoansBase
            .map { normalizeLoanForPersistence(it) }
            .sortedByDescending { it.createdAt }
            .distinctBy { it.id }

        saveLoans(normalizedLoans)

        val validLoanIds = normalizedLoans.map { it.id }.toSet()

        val mergedPayments = (
            payments.filter { it.loanId.isNotBlank() && it.loanId in validLoanIds } +
                readAllLoanPaymentRecords()
            )
            .sortedByDescending { it.createdAt }
            .distinctBy { it.id }

        saveLoanPaymentRecords(mergedPayments)
        saveBlacklist((blacklist + readBlacklist()).sortedByDescending { it.createdAt }.distinctBy { it.id })
        saveReferralsInternal((referrals + readReferrals()).sortedByDescending { it.createdAt }.distinctBy { it.id })
        saveFrequentUsersInternal((frequentUsers + readFrequentUsers()).sortedByDescending { it.createdAt }.distinctBy { it.id })

        appendHistory("Respaldo restaurado en modo fusión")

        return loans.distinctBy { it.id }.size +
            payments.distinctBy { it.id }.size +
            blacklist.distinctBy { it.id }.size +
            referrals.distinctBy { it.id }.size +
            frequentUsers.distinctBy { it.id }.size +
            if (profile != null) 1 else 0
    }

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
