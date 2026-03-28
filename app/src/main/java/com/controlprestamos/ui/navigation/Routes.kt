package com.controlprestamos.ui.navigation

sealed class Route(val route: String) {
    data object Dashboard : Route("dashboard")
    data object Loans : Route("loans")
    data object NewLoan : Route("new_loan")
    data object Profile : Route("profile")
    data object Sarm : Route("sarm")
    data object TenPercent : Route("ten_percent")

    data object LoanDetail : Route("loan_detail/{loanId}") {
        fun create(loanId: Long): String = "loan_detail/$loanId"
    }

    data object CollectionNotice : Route("collection_notice/{loanId}") {
        fun create(loanId: Long): String = "collection_notice/$loanId"
    }

    data object DashboardDetail : Route("dashboard_detail/{detailType}") {
        fun create(detailType: String): String = "dashboard_detail/$detailType"
    }

    data object Blacklist : Route("blacklist")
    data object History : Route("history")
}
