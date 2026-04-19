package com.controlprestamos.app

import com.controlprestamos.features.profile.*

import com.controlprestamos.features.loans.*

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.UUID

class SessionCatalogStore(
    private val prefs: SharedPreferences,
    private val onHistory: (String) -> Unit
) {
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

    fun saveBlacklist(records: List<BlacklistRecordData>) {
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
            onHistory("Lista negra actualizada: ${normalized.fullName}")
        } else {
            current.add(0, normalized)
            onHistory("Lista negra agregada: ${normalized.fullName}")
        }

        saveBlacklist(current)
    }

    fun deleteBlacklistRecord(id: String) {
        val current = readBlacklist().toMutableList()
        val removed = current.firstOrNull { it.id == id } ?: return
        current.removeAll { it.id == id }
        saveBlacklist(current)
        onHistory("Lista negra eliminada: ${removed.fullName}")
    }

    fun saveReferralsInternal(records: List<ReferralRecordData>) {
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
            onHistory("Referido actualizado: ${normalized.referredClient}")
        } else {
            current.add(0, normalized)
            onHistory("Referido guardado: ${normalized.referredClient}")
        }

        saveReferralsInternal(current)
    }

    fun deleteReferral(id: String) {
        val current = readReferrals().toMutableList()
        val removed = current.firstOrNull { it.id == id } ?: return
        current.removeAll { it.id == id }
        saveReferralsInternal(current)
        onHistory("Referido eliminado: ${removed.referredClient}")
    }

    fun saveFrequentUsersInternal(users: List<FrequentUserPaymentData>) {
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
            onHistory("Usuario frecuente actualizado: ${normalized.fullName}")
        } else {
            current.add(0, normalized)
            onHistory("Usuario frecuente guardado: ${normalized.fullName}")
        }

        saveFrequentUsersInternal(current)
    }

    fun deleteFrequentUser(id: String) {
        val current = readFrequentUsers().toMutableList()
        val removed = current.firstOrNull { it.id == id } ?: return
        current.removeAll { it.id == id }
        saveFrequentUsersInternal(current)
        onHistory("Usuario frecuente eliminado: ${removed.fullName}")
    }

}
