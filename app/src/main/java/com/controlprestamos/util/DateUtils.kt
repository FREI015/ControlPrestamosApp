package com.controlprestamos.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object DateUtils {
    val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun format(date: LocalDate): String = date.format(formatter)

    fun today(): LocalDate = LocalDate.now()
}
