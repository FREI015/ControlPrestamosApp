package com.controlprestamos.util

import java.text.NumberFormat
import java.util.Locale

object CurrencyUtils {
    private val formatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale.US)

    fun usd(amount: Double): String = formatter.format(amount)
}
