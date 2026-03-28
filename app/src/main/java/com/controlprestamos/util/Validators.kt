package com.controlprestamos.util

object Validators {
    private val phoneRegex = Regex("""^[0-9+\-\s()]{7,20}$""")
    private val idRegex = Regex("""^[A-Za-z0-9\-.]{5,20}$""")

    fun validPhone(value: String): Boolean = phoneRegex.matches(value.trim())
    fun validId(value: String): Boolean = idRegex.matches(value.trim())
}
