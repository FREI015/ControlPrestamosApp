package com.controlprestamos.app

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LoanReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (LoanReminderManager.isEnabled(context)) {
            LoanReminderManager.ensureChannel(context)
            LoanReminderManager.scheduleDailyReminder(context)
        } else {
            LoanReminderManager.cancelDailyReminder(context)
        }
    }
}
