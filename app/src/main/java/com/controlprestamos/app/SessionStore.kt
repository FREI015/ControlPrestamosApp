package com.controlprestamos.app

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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

private data class HistoryRecordData(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val message: String = ""
)

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("ControlPrestamosPrefs", Context.MODE_PRIVATE)

    companion object {
        private const val STATUS_ACTIVE = "ACTIVO"
        private const val STATUS_COLLECTED = "COBRADO"
        private const val STATUS_LOST = "PERDIDO"
        private const val TRASH_RETENTION_DAYS = 30
        private const val MAX_TRASH_ITEMS = 150
        private val historyDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        private val allowedHistoryPrefixes = listOf(
            "Perfil actualizado",
            "Préstamo registrado",
            "Préstamo actualizado",
            "Préstamo cobrado",
            "Préstamo perdido",
            "Préstamo eliminado",
            "Préstamo enviado a papelera",
            "Préstamo restaurado",
            "Papelera depurada",
            "Pago registrado",
            "Lista negra agregada",
            "Lista negra actualizada",
            "Lista negra eliminada",
            "Referido guardado",
            "Referido actualizado",
            "Referido eliminado",
            "Usuario frecuente guardado",
            "Usuario frecuente actualizado",
            "Usuario frecuente eliminado",
            "PIN inicial creado",
            "PIN actualizado",
            "PIN eliminado",
            "Bloqueo automático",
            "Bloqueo automático",
            "Bloqueo automático"
        )
    }

    fun isDarkMode(): Boolean = prefs.getBoolean("isDarkMode", false)
    fun setDarkMode(isDark: Boolean) = prefs.edit().putBoolean("isDarkMode", isDark).apply()

    fun hasPin(): Boolean {
        val hashedPin = prefs.getString("pin_hash", "") ?: ""
        val legacyPin = prefs.getString("pin", "") ?: ""
        return hashedPin.isNotBlank() || legacyPin.isNotBlank()
    }

    fun savePin(pin: String) {
        val normalized = pin.filter(Char::isDigit)
        require(normalized.length in 4..6) { "El PIN debe tener entre 4 y 6 dígitos." }

        val salt = randomSalt()
        val hash = hashPin(normalized, salt)

        prefs.edit()
            .remove("pin")
            .putString("pin_salt", salt)
            .putString("pin_hash", hash)
            .putInt("pin_failed_attempts", 0)
            .putLong("pin_lockout_until", 0L)
            .apply()
    }

    fun validatePin(pin: String): Boolean {
        val normalized = pin.filter(Char::isDigit)
        if (normalized.isBlank()) return false

        val storedHash = prefs.getString("pin_hash", "") ?: ""
        val storedSalt = prefs.getString("pin_salt", "") ?: ""

        if (storedHash.isNotBlank() && storedSalt.isNotBlank()) {
            return hashPin(normalized, storedSalt) == storedHash
        }

        val legacyPin = prefs.getString("pin", "") ?: ""
        val matchesLegacy = legacyPin.isNotBlank() && legacyPin == normalized
        if (matchesLegacy) {
            savePin(normalized)
        }
        return matchesLegacy
    }

    fun clearPin() {
        prefs.edit()
            .remove("pin")
            .remove("pin_salt")
            .remove("pin_hash")
            .putInt("pin_failed_attempts", 0)
            .putLong("pin_lockout_until", 0L)
            .putBoolean("unlocked", false)
            .apply()
    }

    fun isUnlocked(): Boolean = prefs.getBoolean("unlocked", false)
    fun setUnlocked(value: Boolean) = prefs.edit().putBoolean("unlocked", value).apply()

    fun isPinTemporarilyLocked(): Boolean {
        val lockUntil = prefs.getLong("pin_lockout_until", 0L)
        return lockUntil > System.currentTimeMillis()
    }

    fun getRemainingPinLockSeconds(): Long {
        val remainingMillis = (prefs.getLong("pin_lockout_until", 0L) - System.currentTimeMillis()).coerceAtLeast(0L)
        return if (remainingMillis == 0L) 0L else ((remainingMillis + 999L) / 1000L)
    }

    fun registerFailedPinAttempt(): Long {
        if (isPinTemporarilyLocked()) return getRemainingPinLockSeconds()

        val attempts = prefs.getInt("pin_failed_attempts", 0) + 1
        return if (attempts >= 5) {
            val lockUntil = System.currentTimeMillis() + 60_000L
            prefs.edit()
                .putInt("pin_failed_attempts", 0)
                .putLong("pin_lockout_until", lockUntil)
                .apply()
            getRemainingPinLockSeconds()
        } else {
            prefs.edit()
                .putInt("pin_failed_attempts", attempts)
                .apply()
            0L
        }
    }

    fun clearPinFailures() {
        prefs.edit()
            .putInt("pin_failed_attempts", 0)
            .putLong("pin_lockout_until", 0L)
            .apply()
    }

    private fun randomSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun hashPin(pin: String, saltBase64: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
        digest.update(salt)
        return Base64.encodeToString(digest.digest(pin.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    fun saveProfile(data: UserProfileData) {
        prefs.edit()
            .remove("photoUri")
            .putString("name", data.name)
            .putString("lastName", data.lastName)
            .putString("idNumber", data.idNumber)
            .putString("phone", data.phone)
            .putString("communicationPhone", data.communicationPhone)
            .putString("mobilePaymentPhone", data.mobilePaymentPhone)
            .putString("bankName", data.bankName)
            .putString("bankAccount", data.bankAccount)
            .putString("personalizedMessage", data.personalizedMessage)
            .apply()

        appendHistory("Perfil actualizado")
    }

    fun readProfile(): UserProfileData {
        return UserProfileData(
            photoUri = "",
            name = prefs.getString("name", "") ?: "",
            lastName = prefs.getString("lastName", "") ?: "",
            idNumber = prefs.getString("idNumber", "") ?: "",
            phone = prefs.getString("phone", "") ?: "",
            communicationPhone = prefs.getString("communicationPhone", "") ?: "",
            mobilePaymentPhone = prefs.getString("mobilePaymentPhone", "") ?: "",
            bankName = prefs.getString("bankName", "") ?: "",
            bankAccount = prefs.getString("bankAccount", "") ?: "",
            personalizedMessage = prefs.getString("personalizedMessage", UserProfileData().personalizedMessage)
                ?: UserProfileData().personalizedMessage
        )
    }

    fun logout() {
        prefs.edit().putBoolean("unlocked", false).apply()
    }

    fun appendHistory(item: String) {
        val clean = item.trim()
        if (clean.isBlank()) return

        val current = readHistoryRecords().toMutableList()
        current.add(0, HistoryRecordData(message = clean))
        saveHistoryRecords(current.take(300))
    }

    fun readOperationalHistory(): List<String> {
        migrateLegacyHistoryIfNeeded()

        return readHistoryRecords()
            .sortedByDescending { it.createdAt }
            .filter { record -> allowedHistoryPrefixes.any { prefix -> record.message.startsWith(prefix) } }
            .map { record -> "${formatHistoryTimestamp(record.createdAt)} · ${record.message}" }
    }

    private fun historyKey(): String = "history_records_json"
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

    private fun formatHistoryTimestamp(value: Long): String {
        return try {
            Instant.ofEpochMilli(value)
                .atZone(ZoneId.systemDefault())
                .format(historyDateFormatter)
        } catch (_: Exception) {
            "Sin fecha"
        }
    }

    private fun saveHistoryRecords(records: List<HistoryRecordData>) {
        val array = JSONArray()
        records.forEach { record ->
            array.put(
                JSONObject().apply {
                    put("id", record.id)
                    put("createdAt", record.createdAt)
                    put("message", record.message)
                }
            )
        }
        prefs.edit().putString(historyKey(), array.toString()).apply()
    }

    private fun readHistoryRecords(): List<HistoryRecordData> {
        val array = safeArray(prefs.getString(historyKey(), "[]"))
        val result = mutableListOf<HistoryRecordData>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val message = obj.optString("message").trim()
            if (message.isBlank()) continue

            result.add(
                HistoryRecordData(
                    id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    message = message
                )
            )
        }

        return result
            .sortedByDescending { it.createdAt }
            .distinctBy { it.id }
    }

    private fun migrateLegacyHistoryIfNeeded() {
        val legacy = prefs.getStringSet("history_items", emptySet()) ?: emptySet()
        if (legacy.isEmpty()) return

        val migrated = readHistoryRecords().toMutableList()
        legacy.forEach { entry ->
            val timestamp = entry.substringBefore("|", "0").toLongOrNull() ?: System.currentTimeMillis()
            val message = entry.substringAfter("|", "").trim()
            if (message.isNotBlank()) {
                migrated.add(HistoryRecordData(createdAt = timestamp, message = message))
            }
        }

        saveHistoryRecords(
            migrated
                .sortedByDescending { it.createdAt }
                .take(300)
        )

        prefs.edit().remove("history_items").apply()
    }

    private fun normalizeStatus(pendingAmount: Double, explicitLost: Boolean): String {
        if (explicitLost && pendingAmount > 0.0) return STATUS_LOST
        return if (pendingAmount <= 0.0) STATUS_COLLECTED else STATUS_ACTIVE
    }

    private fun normalizeLoanForPersistence(loan: ManualLoanData, existing: ManualLoanData? = null): ManualLoanData {
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

    private fun saveLoans(loans: List<ManualLoanData>) {
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

    private fun upsertLoanSilently(loan: ManualLoanData) {
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
            appendHistory("Préstamo actualizado: ${normalized.fullName}")
        } else {
            current.add(0, normalized)
            appendHistory("Préstamo registrado: ${normalized.fullName}")
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

        appendHistory("Préstamo eliminado: ${removed.fullName}")
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

    private fun saveLoanPaymentRecords(records: List<LoanPaymentRecordData>) {
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

    private fun readAllLoanPaymentRecords(): List<LoanPaymentRecordData> {
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

    private fun saveLoanPaymentRecord(record: LoanPaymentRecordData) {
        val current = readAllLoanPaymentRecords().toMutableList()
        current.add(0, record)
        saveLoanPaymentRecords(
            current
                .sortedByDescending { it.createdAt }
                .distinctBy { it.id }
        )
    }

    private fun deleteLoanPaymentHistoryForLoan(loanId: String) {
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
                    "Se aplicó solo el pendiente restante."
                } else {
                    ""
                }
            )
        )

        appendHistory(
            "Pago registrado: ${loan.fullName} abonó ${"%.2f".format(java.util.Locale.US, appliedAmount)} USD. Pendiente: ${"%.2f".format(java.util.Locale.US, newPending)} USD"
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
        appendHistory("Préstamo cobrado: ${loan.fullName}")
        return true
    }

    fun markLoanAsLost(loanId: String): Boolean {
        val loan = readLoanById(loanId) ?: return false
        if (loan.status == STATUS_LOST) return false

        upsertLoanSilently(loan.copy(status = STATUS_LOST))
        appendHistory("Préstamo perdido: ${loan.fullName}")
        return true
    }

    private fun saveBlacklist(records: List<BlacklistRecordData>) {
        val array = JSONArray()
        records.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("fullName", item.fullName)
                    put("idNumber", item.idNumber)
                    put("phone", item.phone)
                    put("reason", item.reason)
                    put("notes", item.notes)
                    put("addedDate", item.addedDate)
                    put("createdAt", item.createdAt)
                }
            )
        }
        prefs.edit().putString(blacklistKey(), array.toString()).apply()
    }

    fun readBlacklist(): List<BlacklistRecordData> {
        val array = safeArray(prefs.getString(blacklistKey(), "[]"))
        val result = mutableListOf<BlacklistRecordData>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            result.add(
                BlacklistRecordData(
                    id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                    fullName = obj.optString("fullName").trim(),
                    idNumber = obj.optString("idNumber").trim(),
                    phone = obj.optString("phone").trim(),
                    reason = obj.optString("reason").trim(),
                    notes = obj.optString("notes").trim(),
                    addedDate = obj.optString("addedDate", LocalDate.now().toString()).trim(),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        return result
            .sortedByDescending { it.createdAt }
            .distinctBy { it.id }
    }

    fun saveBlacklistRecord(item: BlacklistRecordData) {
        val current = readBlacklist().toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        val normalized = item.copy(
            fullName = item.fullName.trim(),
            idNumber = item.idNumber.trim(),
            phone = item.phone.trim(),
            reason = item.reason.trim(),
            notes = item.notes.trim(),
            addedDate = item.addedDate.trim().ifBlank { LocalDate.now().toString() }
        )

        if (index >= 0) {
            current[index] = normalized
            appendHistory("Lista negra actualizada: ${normalized.fullName}")
        } else {
            current.add(0, normalized)
            appendHistory("Lista negra agregada: ${normalized.fullName}")
        }

        saveBlacklist(current)
    }

    fun deleteBlacklistRecord(id: String) {
        val current = readBlacklist().toMutableList()
        val removed = current.firstOrNull { it.id == id } ?: return
        current.removeAll { it.id == id }
        saveBlacklist(current)
        appendHistory("Lista negra eliminada: ${removed.fullName}")
    }

    private fun saveReferralsInternal(records: List<ReferralRecordData>) {
        val array = JSONArray()
        records.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("referralDate", item.referralDate)
                    put("referredClient", item.referredClient)
                    put("referredBy", item.referredBy)
                    put("loanAmount", item.loanAmount)
                    put("commissionPercent", item.commissionPercent)
                    put("status", item.status)
                    put("notes", item.notes)
                    put("createdAt", item.createdAt)
                }
            )
        }
        prefs.edit().putString(referralsKey(), array.toString()).apply()
    }

    fun readReferrals(): List<ReferralRecordData> {
        val array = safeArray(prefs.getString(referralsKey(), "[]"))
        val result = mutableListOf<ReferralRecordData>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            result.add(
                ReferralRecordData(
                    id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                    referralDate = obj.optString("referralDate", LocalDate.now().toString()).trim(),
                    referredClient = obj.optString("referredClient").trim(),
                    referredBy = obj.optString("referredBy").trim(),
                    loanAmount = obj.optDouble("loanAmount", 0.0).coerceAtLeast(0.0),
                    commissionPercent = obj.optDouble("commissionPercent", 10.0).coerceAtLeast(0.0),
                    status = obj.optString("status", "PENDIENTE").trim().ifBlank { "PENDIENTE" },
                    notes = obj.optString("notes").trim(),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        return result
            .sortedByDescending { it.createdAt }
            .distinctBy { it.id }
    }

    fun saveReferral(item: ReferralRecordData) {
        val current = readReferrals().toMutableList()
        val index = current.indexOfFirst { it.id == item.id }
        val normalized = item.copy(
            referralDate = item.referralDate.trim().ifBlank { LocalDate.now().toString() },
            referredClient = item.referredClient.trim(),
            referredBy = item.referredBy.trim(),
            loanAmount = item.loanAmount.coerceAtLeast(0.0),
            commissionPercent = item.commissionPercent.coerceAtLeast(0.0),
            status = item.status.trim().uppercase().ifBlank { "PENDIENTE" },
            notes = item.notes.trim()
        )

        if (index >= 0) {
            current[index] = normalized
            appendHistory("Referido actualizado: ${normalized.referredClient}")
        } else {
            current.add(0, normalized)
            appendHistory("Referido guardado: ${normalized.referredClient}")
        }

        saveReferralsInternal(current)
    }

    fun deleteReferral(id: String) {
        val current = readReferrals().toMutableList()
        val removed = current.firstOrNull { it.id == id } ?: return
        current.removeAll { it.id == id }
        saveReferralsInternal(current)
        appendHistory("Referido eliminado: ${removed.referredClient}")
    }

    private fun saveFrequentUsersInternal(users: List<FrequentUserPaymentData>) {
        val array = JSONArray()
        users.forEach { user ->
            array.put(
                JSONObject().apply {
                    put("id", user.id)
                    put("fullName", user.fullName)
                    put("idNumber", user.idNumber)
                    put("phone", user.phone)
                    put("bankName", user.bankName)
                    put("bankAccount", user.bankAccount)
                    put("mobilePaymentPhone", user.mobilePaymentPhone)
                    put("paymentAlias", user.paymentAlias)
                    put("notes", user.notes)
                    put("createdAt", user.createdAt)
                }
            )
        }
        prefs.edit().putString(frequentUsersKey(), array.toString()).apply()
    }

    fun readFrequentUsers(): List<FrequentUserPaymentData> {
        val array = safeArray(prefs.getString(frequentUsersKey(), "[]"))
        val result = mutableListOf<FrequentUserPaymentData>()

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            result.add(
                FrequentUserPaymentData(
                    id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                    fullName = obj.optString("fullName").trim(),
                    idNumber = obj.optString("idNumber").trim(),
                    phone = obj.optString("phone").trim(),
                    bankName = obj.optString("bankName").trim(),
                    bankAccount = obj.optString("bankAccount").trim(),
                    mobilePaymentPhone = obj.optString("mobilePaymentPhone").trim(),
                    paymentAlias = obj.optString("paymentAlias").trim(),
                    notes = obj.optString("notes").trim(),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        return result
            .sortedByDescending { it.createdAt }
            .distinctBy { it.id }
    }

    fun saveFrequentUser(user: FrequentUserPaymentData) {
        val current = readFrequentUsers().toMutableList()
        val index = current.indexOfFirst { it.id == user.id }
        val normalized = user.copy(
            fullName = user.fullName.trim(),
            idNumber = user.idNumber.trim(),
            phone = user.phone.trim(),
            bankName = user.bankName.trim(),
            bankAccount = user.bankAccount.trim(),
            mobilePaymentPhone = user.mobilePaymentPhone.trim(),
            paymentAlias = user.paymentAlias.trim(),
            notes = user.notes.trim()
        )

        if (index >= 0) {
            current[index] = normalized
            appendHistory("Usuario frecuente actualizado: ${normalized.fullName}")
        } else {
            current.add(0, normalized)
            appendHistory("Usuario frecuente guardado: ${normalized.fullName}")
        }

        saveFrequentUsersInternal(current)
    }

    fun deleteFrequentUser(id: String) {
        val current = readFrequentUsers().toMutableList()
        val removed = current.firstOrNull { it.id == id } ?: return
        current.removeAll { it.id == id }
        saveFrequentUsersInternal(current)
        appendHistory("Usuario frecuente eliminado: ${removed.fullName}")
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
