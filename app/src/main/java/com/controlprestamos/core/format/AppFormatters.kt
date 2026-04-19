package com.controlprestamos.core.format

import java.util.Locale

fun formatMoney(value: Double, currencySymbol: String = "$"): String {
    val safeValue = if (value.isFinite()) value else 0.0
    return currencySymbol + String.format(Locale.US, "%.2f", safeValue)
}

fun formatPercent(value: Double): String {
    val safeValue = if (value.isFinite()) value else 0.0
    return String.format(Locale.US, "%.2f%%", safeValue)
}
