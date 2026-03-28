package com.controlprestamos.security

import java.security.MessageDigest

object HashUtils {

    fun normalizePhone(phone: String): String = phone.filter { it.isDigit() }

    fun normalizeId(id: String): String = id.trim().lowercase().replace(" ", "")

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
