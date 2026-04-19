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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class LoanReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        LoanReminderManager.sendDailySummaryIfNeeded(context)
    }
}
