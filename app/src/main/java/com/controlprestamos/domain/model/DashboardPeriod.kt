package com.controlprestamos.domain.model

enum class DashboardPeriod(val label: String, val daysBack: Long) {
    BIWEEKLY("Quincenal", 15),
    MONTHLY("Mensual", 30),
    QUARTERLY("Trimestral", 90),
    SEMIANNUAL("Semestral", 180)
}
