package com.controlprestamos.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LoanReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        LoanReminderManager.sendDailySummaryIfNeeded(context)
    }
}
