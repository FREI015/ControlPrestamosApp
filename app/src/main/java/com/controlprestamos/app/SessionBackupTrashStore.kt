package com.controlprestamos.app

import com.controlprestamos.features.backup.*
import com.controlprestamos.features.security.*
import com.controlprestamos.features.settings.*
import com.controlprestamos.features.dashboard.*
import com.controlprestamos.features.people.*
import com.controlprestamos.features.search.*
import com.controlprestamos.features.more.*
import com.controlprestamos.core.format.*
import com.controlprestamos.core.validation.*
import com.controlprestamos.features.profile.*

import com.controlprestamos.features.loans.*

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

class SessionBackupTrashStore(
    private val prefs: SharedPreferences,
    private val onHistory: (String) -> Unit,
    private val normalizeLoanForPersistenceFn: (ManualLoanData) -> ManualLoanData,
    private val upsertLoanSilentlyFn: (ManualLoanData) -> Unit,
    private val saveLoansFn: (List<ManualLoanData>) -> Unit,
    private val readLoansFn: () -> List<ManualLoanData>,
    private val readLoanPaymentHistoryFn: (String) -> List<LoanPaymentRecordData>,
    private val readAllLoanPaymentRecordsFn: () -> List<LoanPaymentRecordData>,
    private val saveLoanPaymentRecordsFn: (List<LoanPaymentRecordData>) -> Unit,
    private val deleteLoanPaymentHistoryForLoanFn: (String) -> Unit,
    private val readActiveLoanIdFn: () -> String,
    private val setActiveLoanIdFn: (String) -> Unit
) {
    companion object {
        private const val STATUS_ACTIVE = "ACTIVO"
        private const val TRASH_RETENTION_DAYS = 30
        private const val MAX_TRASH_ITEMS = 150
    }

    private fun safeArray(raw: String?): JSONArray {
        return try {
            JSONArray(raw ?: "[]")
        } catch (_: Exception) {
            JSONArray()
        }
    }

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

            val loan = normalizeLoanForPersistenceFn(
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
            onHistory("Papelera depurada: $removedCount registro(s) vencido(s)")
        }

        return removedCount
    }

    private fun hasPotentialRestoreDuplicate(candidate: ManualLoanData): Boolean {
        return readLoansFn().any { existing ->
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

        val current = readLoansFn().toMutableList()
        val removed = current.firstOrNull { it.id == id } ?: return false
        val removedPayments = readLoanPaymentHistoryFn(id)

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
        saveLoansFn(current)
        deleteLoanPaymentHistoryForLoanFn(id)

        if (readActiveLoanIdFn() == id) {
            setActiveLoanIdFn("")
        }

        onHistory("Préstamo enviado a papelera: ${removed.fullName}")
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

        val currentLoans = readLoansFn()
        val loanIdAlreadyExists = currentLoans.any { it.id == snapshot.loan.id }
        val restoredLoanId = if (loanIdAlreadyExists) UUID.randomUUID().toString() else snapshot.loan.id
        val restoredLoan = snapshot.loan.copy(id = restoredLoanId)

        val existingPaymentIds = readAllLoanPaymentRecordsFn().map { it.id }.toMutableSet()
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

        upsertLoanSilentlyFn(restoredLoan)

        val mergedPayments = (restoredPayments + readAllLoanPaymentRecordsFn())
            .sortedByDescending { it.createdAt }
            .distinctBy { it.id }

        saveLoanPaymentRecordsFn(mergedPayments)

        deleted.removeAt(index)
        saveDeletedLoanSnapshots(deleted)

        if (loanIdAlreadyExists) {
            onHistory("Préstamo restaurado: ${restoredLoan.fullName} (nuevo identificador)")
            return RestoreDeletedLoanResult(
                success = true,
                message = "Préstamo restaurado correctamente. Se asignó un nuevo identificador interno para evitar conflicto con uno existente."
            )
        }

        onHistory("Préstamo restaurado: ${restoredLoan.fullName}")
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

        onHistory("Préstamo eliminado definitivamente: ${snapshot.loan.fullName}")
        return true
    }
    fun replaceImportedBackupData(
        profile: UserProfileData?,
        loans: List<ManualLoanData>,
        payments: List<LoanPaymentRecordData>,
        blacklist: List<BlacklistRecordData>,
        referrals: List<ReferralRecordData>,
        frequentUsers: List<FrequentUserPaymentData>,
        saveProfileFn: (UserProfileData) -> Unit,
        normalizeLoanForPersistenceWithExistingFn: (ManualLoanData, ManualLoanData?) -> ManualLoanData,
        saveBlacklistFn: (List<BlacklistRecordData>) -> Unit,
        saveReferralsInternalFn: (List<ReferralRecordData>) -> Unit,
        saveFrequentUsersInternalFn: (List<FrequentUserPaymentData>) -> Unit
    ): Int {
        profile?.let { saveProfileFn(it) }

        val normalizedLoans = loans
            .map { normalizeLoanForPersistenceWithExistingFn(it, null) }
            .sortedByDescending { it.createdAt }
            .distinctBy { it.id }

        saveLoansFn(normalizedLoans)

        val validLoanIds = normalizedLoans.map { it.id }.toSet()

        val cleanPayments = payments
            .filter { it.loanId.isNotBlank() && it.loanId in validLoanIds }
            .sortedByDescending { it.createdAt }
            .distinctBy { it.id }

        saveLoanPaymentRecordsFn(cleanPayments)
        saveBlacklistFn(blacklist.sortedByDescending { it.createdAt }.distinctBy { it.id })
        saveReferralsInternalFn(referrals.sortedByDescending { it.createdAt }.distinctBy { it.id })
        saveFrequentUsersInternalFn(frequentUsers.sortedByDescending { it.createdAt }.distinctBy { it.id })

        val activeId = readActiveLoanIdFn()
        if (activeId.isNotBlank() && activeId !in validLoanIds) {
            setActiveLoanIdFn("")
        }

        onHistory("Respaldo restaurado en modo reemplazo")

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
        frequentUsers: List<FrequentUserPaymentData>,
        saveProfileFn: (UserProfileData) -> Unit,
        readProfileFn: () -> UserProfileData,
        mergeProfileDataFn: (UserProfileData, UserProfileData) -> UserProfileData,
        normalizeLoanForPersistenceWithExistingFn: (ManualLoanData, ManualLoanData?) -> ManualLoanData,
        saveBlacklistFn: (List<BlacklistRecordData>) -> Unit,
        readBlacklistFn: () -> List<BlacklistRecordData>,
        saveReferralsInternalFn: (List<ReferralRecordData>) -> Unit,
        readReferralsFn: () -> List<ReferralRecordData>,
        saveFrequentUsersInternalFn: (List<FrequentUserPaymentData>) -> Unit,
        readFrequentUsersFn: () -> List<FrequentUserPaymentData>
    ): Int {
        profile?.let {
            saveProfileFn(mergeProfileDataFn(readProfileFn(), it))
        }

        val mergedLoansBase = (loans + readLoansFn())
            .sortedByDescending { it.createdAt }
            .distinctBy { it.id }

        val normalizedLoans = mergedLoansBase
            .map { normalizeLoanForPersistenceWithExistingFn(it, null) }
            .sortedByDescending { it.createdAt }
            .distinctBy { it.id }

        saveLoansFn(normalizedLoans)

        val validLoanIds = normalizedLoans.map { it.id }.toSet()

        val mergedPayments = (
            payments.filter { it.loanId.isNotBlank() && it.loanId in validLoanIds } +
                readAllLoanPaymentRecordsFn()
            )
            .sortedByDescending { it.createdAt }
            .distinctBy { it.id }

        saveLoanPaymentRecordsFn(mergedPayments)
        saveBlacklistFn((blacklist + readBlacklistFn()).sortedByDescending { it.createdAt }.distinctBy { it.id })
        saveReferralsInternalFn((referrals + readReferralsFn()).sortedByDescending { it.createdAt }.distinctBy { it.id })
        saveFrequentUsersInternalFn((frequentUsers + readFrequentUsersFn()).sortedByDescending { it.createdAt }.distinctBy { it.id })

        onHistory("Respaldo restaurado en modo fusión")

        return loans.distinctBy { it.id }.size +
            payments.distinctBy { it.id }.size +
            blacklist.distinctBy { it.id }.size +
            referrals.distinctBy { it.id }.size +
            frequentUsers.distinctBy { it.id }.size +
            if (profile != null) 1 else 0
    }
}
