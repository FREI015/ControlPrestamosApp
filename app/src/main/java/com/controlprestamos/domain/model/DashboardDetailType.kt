package com.controlprestamos.domain.model

enum class DashboardDetailType(
    val routeValue: String,
    val title: String
) {
    TOTAL_LOANED("total_loaned", "Detalle de total prestado"),
    PROJECTED_INTEREST("projected_interest", "Detalle de intereses proyectados"),
    TOTAL_TO_COLLECT("total_to_collect", "Detalle de total a cobrar"),
    OVERDUE_PAYMENTS("overdue_payments", "Detalle de pagos retrasados");

    companion object {
        fun fromRouteValue(value: String): DashboardDetailType {
            return entries.firstOrNull { it.routeValue == value } ?: TOTAL_LOANED
        }
    }
}