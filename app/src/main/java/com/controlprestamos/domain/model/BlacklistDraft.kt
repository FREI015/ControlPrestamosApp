package com.controlprestamos.domain.model

import java.time.LocalDate

data class BlacklistDraft(
    val customerName: String = "",
    val phone: String = "",
    val idNumber: String = "",
    val reason: String = "",
    val addedDate: String = ""
) {
    fun parsedAddedDate(): LocalDate? = runCatching { LocalDate.parse(addedDate) }.getOrNull()
}
