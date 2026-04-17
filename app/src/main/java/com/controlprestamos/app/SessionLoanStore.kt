package com.controlprestamos.app

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

class SessionLoanStore(
    private val prefs: SharedPreferences,
    private val onHistory: (String) -> Unit
) {
    companion object {
        private const val STATUS_ACTIVE = "ACTIVO"
        private const val STATUS_COLLECTED = "COBRADO"
        private const val STATUS_LOST = "PERDIDO"
    }

    private fun loansKey(): String = "manual_loans_json"
    private fun loanPaymentsKey(): String = "loan_payment_records_json"

    private fun safeArray(raw: String?): JSONArray {
        return try {
            JSONArray(raw ?: "[]")
        } catch (_: Exception) {
            JSONArray()
        }
    }

    fun normalizeStatus(pendingAmount: Double, explicitLost: Boolean): String {
        if (explicitLost && pendingAmount > 0.0) return STATUS_LOST
        return if (pendingAmount <= 0.0) STATUS_COLLECTED else STATUS_ACTIVE
    }

    fun normalizeLoanForPersistence(loan: ManualLoanData, existing: ManualLoanData? = null): ManualLoanData {
        val safeAmount = loan.loanAmount.coerceAtLeast(0.0)
        val safePercent = loan.percent.coerceAtLeast(0.0)
        val provisional = loan.copy(
            fullName = loan.fullName.trim(),
            idNumber = loan.idNumber.trim(),
            phone = loan.phone.trim(),
            loanAmount = safeAmount,
            percent = safePercent,
            loanDate = loan.loanDate.trim(),
            dueDate = loan.dueDate.trim(),
            exchangeRate = loan.exchangeRate.trim(),
            conditions = loan.conditions.trim(),
            createdAt = existing?.createdAt ?: loan.createdAt
        )

        val safePaid = provisional.paidAmount.coerceIn(0.0, provisional.totalAmount())
        val explicitLost = provisional.status.uppercase() == STATUS_LOST
        val normalizedStatus = normalizeStatus(
            pendingAmount = (provisional.totalAmount() - safePaid).coerceAtLeast(0.0),
            explicitLost = explicitLost
        )

        return provisional.copy(
            paidAmount = safePaid,
            status = normalizedStatus
        )
    }

    fun saveLoans(loans: List<ManualLoanData>) {
        val array = JSONArray()
        loans.forEach { loan ->
            array.put(
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
        }
        prefs.edit().putString(loansKey(), array.toString()).apply()
    }

    fun readLoans(): List<ManualLoanData> {
        val array = safeArray(prefs.getString(loansKey(), "[]"))
        val result = mutableListOf<ManualLoanData>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val loan = normalizeLoanForPersistence(
                ManualLoanData(
                    id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                    fullName = obj.optString("fullName"),
                    idNumber = obj.optString("idNumber"),
                    phone = obj.optString("phone"),
                    loanAmount = obj.optDouble("loanAmount", 0.0),
                    percent = obj.optDouble("percent", 0.0),
                    loanDate = obj.optString("loanDate"),
                    dueDate = obj.optString("dueDate"),
                    exchangeRate = obj.optString("exchangeRate"),
                    conditions = obj.optString("conditions"),
                    paidAmount = obj.optDouble("paidAmount", 0.0),
                    status = obj.optString("status", STATUS_ACTIVE),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
            result.add(loan)
        }

        return result
            .sortedByDescending { it.createdAt }
            .distinctBy { it.id }
    }

    fun upsertLoanSilently(loan: ManualLoanData) {
        val current = readLoans().toMutableList()
        val index = current.indexOfFirst { it.id == loan.id }
        val existing = current.getOrNull(index)
        val normalized = normalizeLoanForPersistence(loan, existing)

        if (index >= 0) {
            current[index] = normalized
        } else {
            current.add(0, normalized)
        }

        saveLoans(current)
    }

    fun saveLoan(loan: ManualLoanData) {
        val current = readLoans().toMutableList()
        val index = current.indexOfFirst { it.id == loan.id }
        val existing = current.getOrNull(index)
        val normalized = normalizeLoanForPersistence(loan, existing)

        if (index >= 0) {
            current[index] = normalized
            onHistory("PrÃƒÂ©stamo actualizado: ${normalized.fullName}")
        } else {
            current.add(0, normalized)
            onHistory("PrÃƒÂ©stamo registrado: ${normalized.fullName}")
        }

        saveLoans(current)
    }

    fun readLoanById(id: String): ManualLoanData? {
        return readLoans().firstOrNull { it.id == id }
    }

    fun deleteLoan(id: String) {
        val current = readLoans().toMutableList()
        val removed = current.firstOrNull { it.id == id } ?: return

        current.removeAll { it.id == id }
        saveLoans(current)
        deleteLoanPaymentHistoryForLoan(id)

        if (readActiveLoanId() == id) {
            setActiveLoanId("")
        }

        onHistory("PrÃƒÂ©stamo eliminado: ${removed.fullName}")
    }

    fun setActiveLoanId(id: String) {
        if (id.isBlank()) {
            prefs.edit().remove("active_loan_id").apply()
            return
        }

        val exists = readLoans().any { it.id == id }
        if (exists) {
            prefs.edit().putString("active_loan_id", id).apply()
        } else {
            prefs.edit().remove("active_loan_id").apply()
        }
    }

    fun readActiveLoanId(): String {
        val storedId = prefs.getString("active_loan_id", "") ?: ""
        if (storedId.isBlank()) return ""

        val exists = readLoans().any { it.id == storedId }
        if (!exists) {
            prefs.edit().remove("active_loan_id").apply()
            return ""
        }

        return storedId
    }

    fun readActiveLoan(): ManualLoanData? {
        val id = readActiveLoanId()
        if (id.isBlank()) return null
        return readLoanById(id)
    }

    fun saveLoanPaymentRecords(records: List<LoanPaymentRecordData>) {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject().apply {
                    put("id", record.id)
                    put("loanId", record.loanId)
                    put("clientName", record.clientName)
                    put("amount", record.amount)
                    put("paymentDate", record.paymentDate)
                    put("paymentType", record.paymentType)
                    put("previousPaidAmount", record.previousPaidAmount)
                    put("newPaidAmount", record.newPaidAmount)
                    put("previousPendingAmount", record.previousPendingAmount)
                    put("newPendingAmount", record.newPendingAmount)
                    put("note", record.note)
                    put("createdAt", record.createdAt)
                }
            )
        }
        prefs.edit().putString(loanPaymentsKey(), array.toString()).apply()
    }

    fun readAllLoanPaymentRecords(): List<LoanPaymentRecordData> {
        val array = safeArray(prefs.getString(loanPaymentsKey(), "[]"))
        val result = mutableListOf<LoanPaymentRecordData>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            result.add(
                LoanPaymentRecordData(
                    id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                    loanId = obj.optString("loanId"),
                    clientName = obj.optString("clientName"),
                    amount = obj.optDouble("amount", 0.0),
                    paymentDate = obj.optString("paymentDate", LocalDate.now().toString()),
                    paymentType = obj.optString("paymentType", "ABONO").ifBlank { "ABONO" },
                    previousPaidAmount = obj.optDouble("previousPaidAmount", 0.0),
                    newPaidAmount = obj.optDouble("newPaidAmount", 0.0),
                    previousPendingAmount = obj.optDouble("previousPendingAmount", 0.0),
                    newPendingAmount = obj.optDouble("newPendingAmount", 0.0),
                    note = obj.optString("note"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        return result
            .sortedByDescending { it.createdAt }
            .distinctBy { it.id }
    }

    fun readLoanPaymentHistory(loanId: String): List<LoanPaymentRecordData> {
        return readAllLoanPaymentRecords()
            .filter { it.loanId == loanId }
            .sortedByDescending { it.createdAt }
    }

    fun saveLoanPaymentRecord(record: LoanPaymentRecordData) {
        val current = readAllLoanPaymentRecords().toMutableList()
        current.add(0, record)
        saveLoanPaymentRecords(
            current
                .sortedByDescending { it.createdAt }
                .distinctBy { it.id }
        )
    }

    fun deleteLoanPaymentHistoryForLoan(loanId: String) {
        val remaining = readAllLoanPaymentRecords().filterNot { it.loanId == loanId }
        saveLoanPaymentRecords(remaining)
    }

    fun registerPayment(loanId: String, paymentAmount: Double): Boolean {
        val loan = readLoanById(loanId) ?: return false
        if (paymentAmount <= 0.0) return false

        val total = loan.totalAmount()
        val previousPaid = loan.paidAmount.coerceAtLeast(0.0)
        val previousPending = (total - previousPaid).coerceAtLeast(0.0)
        if (previousPending <= 0.0) return false

        val appliedAmount = paymentAmount.coerceAtMost(previousPending)
        if (appliedAmount <= 0.0) return false

        val newPaid = (previousPaid + appliedAmount).coerceAtMost(total)
        val newPending = (total - newPaid).coerceAtLeast(0.0)
        val updatedStatus = when {
            newPending <= 0.0 -> STATUS_COLLECTED
            loan.status.uppercase() == STATUS_LOST -> STATUS_LOST
            else -> STATUS_ACTIVE
        }

        upsertLoanSilently(
            loan.copy(
                paidAmount = newPaid,
                status = updatedStatus
            )
        )

        saveLoanPaymentRecord(
            LoanPaymentRecordData(
                loanId = loan.id,
                clientName = loan.fullName,
                amount = appliedAmount,
                paymentDate = LocalDate.now().toString(),
                paymentType = "ABONO",
                previousPaidAmount = previousPaid,
                newPaidAmount = newPaid,
                previousPendingAmount = previousPending,
                newPendingAmount = newPending,
                note = if (paymentAmount > appliedAmount) {
                    "Se aplicÃƒÂ³ solo el pendiente restante."
                } else {
                    ""
                }
            )
        )

        onHistory(
            "Pago registrado: ${loan.fullName} abonÃƒÂ³ ${"%.2f".format(java.util.Locale.US, appliedAmount)} USD. Pendiente: ${"%.2f".format(java.util.Locale.US, newPending)} USD"
        )

        return true
    }

    fun markLoanAsCollected(loanId: String): Boolean {
        val loan = readLoanById(loanId) ?: return false
        if (loan.status == STATUS_COLLECTED && loan.pendingAmount() <= 0.0) return false

        upsertLoanSilently(
            loan.copy(
                paidAmount = loan.totalAmount(),
                status = STATUS_COLLECTED
            )
        )
        onHistory("PrÃƒÂ©stamo cobrado: ${loan.fullName}")
        return true
    }

    fun markLoanAsLost(loanId: String): Boolean {
        val loan = readLoanById(loanId) ?: return false
        if (loan.status == STATUS_LOST) return false

        upsertLoanSilently(loan.copy(status = STATUS_LOST))
        onHistory("PrÃƒÂ©stamo perdido: ${loan.fullName}")
        return true
    }


}