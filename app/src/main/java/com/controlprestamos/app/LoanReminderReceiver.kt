package com.controlprestamos.app

import com.controlprestamos.features.profile.*

import com.controlprestamos.features.loans.*

import com.controlprestamos.core.navigation.*

import com.controlprestamos.core.design.*

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LoanReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        LoanReminderManager.sendDailySummaryIfNeeded(context)
    }
}
