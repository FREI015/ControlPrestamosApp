package com.controlprestamos.domain.model

data class DashboardSummary(
    val label: String,

    // Etiqueta visible de la quincena actual
    val currentCycleLabel: String = "",

    // Totales principales del dashboard
    val totalLoaned: Double = 0.0,
    val totalRecovered: Double = 0.0,
    val projectedInterest: Double = 0.0,
    val projectedToCollect: Double = 0.0,

    // Retrasos: este bloque será clave porque sí debe arrastrarse
    // desde quincenas anteriores si sigue pendiente
    val overdueAmount: Double = 0.0,
    val overdueCount: Int = 0,
    val carryOverOverdueAmount: Double = 0.0,
    val carryOverOverdueCount: Int = 0,

    // Se deja por compatibilidad por si algo existente aún lo usa,
    // pero ya no debería ser la tarjeta principal del dashboard
    val lossesAmount: Double = 0.0,

    // Estados operativos
    val activeCount: Int = 0,
    val collectedCount: Int = 0,
    val lostCount: Int = 0,
    val archivedCount: Int = 0,

    // Progreso
    val collectionProgress: Float = 0f,

    // Texto de lectura rápida del estado de la cartera
    val dashboardMessage: String = "",

    // Preparado para alertas rápidas del panel
    val alerts: List<String> = emptyList()
)