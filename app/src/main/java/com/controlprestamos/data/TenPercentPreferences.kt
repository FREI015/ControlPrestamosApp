package com.controlprestamos.data

import android.content.Context
import android.content.SharedPreferences
import com.controlprestamos.domain.model.TenPercentEntry
import org.json.JSONArray
import org.json.JSONObject

class TenPercentPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ten_percent_prefs", Context.MODE_PRIVATE)

    fun getEntries(): List<TenPercentEntry> {
        val raw = prefs.getString("entries", "[]") ?: "[]"
        val array = JSONArray(raw)
        val result = mutableListOf<TenPercentEntry>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                TenPercentEntry(
                    id = obj.optLong("id"),
                    date = obj.optString("date"),
                    clientName = obj.optString("clientName"),
                    referredTo = obj.optString("referredTo"),
                    loanAmountUsd = obj.optDouble("loanAmountUsd"),
                    percent = obj.optDouble("percent"),
                    commissionUsd = obj.optDouble("commissionUsd"),
                    status = obj.optString("status"),
                    notes = obj.optString("notes")
                )
            )
        }
        return result.sortedByDescending { it.id }
    }

    fun saveEntries(entries: List<TenPercentEntry>) {
        val array = JSONArray()
        entries.forEach { entry ->
            val obj = JSONObject()
            obj.put("id", entry.id)
            obj.put("date", entry.date)
            obj.put("clientName", entry.clientName)
            obj.put("referredTo", entry.referredTo)
            obj.put("loanAmountUsd", entry.loanAmountUsd)
            obj.put("percent", entry.percent)
            obj.put("commissionUsd", entry.commissionUsd)
            obj.put("status", entry.status)
            obj.put("notes", entry.notes)
            array.put(obj)
        }
        prefs.edit().putString("entries", array.toString()).apply()
    }
}
