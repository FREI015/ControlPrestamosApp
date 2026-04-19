package com.controlprestamos.app

import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

private data class HistoryRecordData(
    val id: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val message: String = ""
)

class SessionProfileHistoryStore(
    private val prefs: SharedPreferences
) {
    companion object {
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
            "Bloqueo automático"
        )
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
}
