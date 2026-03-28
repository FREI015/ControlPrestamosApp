package com.controlprestamos.data

import android.content.Context
import android.content.SharedPreferences
import com.controlprestamos.domain.model.SarmEntry
import org.json.JSONArray
import org.json.JSONObject

class SarmPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sarm_prefs", Context.MODE_PRIVATE)

    fun getEntries(): List<SarmEntry> {
        val raw = prefs.getString("entries", "[]") ?: "[]"
        val array = JSONArray(raw)
        val result = mutableListOf<SarmEntry>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                SarmEntry(
                    id = obj.optLong("id"),
                    date = obj.optString("date"),
                    clientName = obj.optString("clientName"),
                    amountUsd = obj.optDouble("amountUsd"),
                    status = obj.optString("status"),
                    notes = obj.optString("notes")
                )
            )
        }
        return result.sortedByDescending { it.id }
    }

    fun saveEntries(entries: List<SarmEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject()
            obj.put("id", entry.id)
            obj.put("date", entry.date)
            obj.put("clientName", entry.clientName)
            obj.put("amountUsd", entry.amountUsd)
            obj.put("status", entry.status)
            obj.put("notes", entry.notes)
            array.put(obj)
        }
        prefs.edit().putString("entries", array.toString()).apply()
    }
}
