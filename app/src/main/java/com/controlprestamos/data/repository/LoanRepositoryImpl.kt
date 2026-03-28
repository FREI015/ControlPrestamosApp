package com.controlprestamos.data.repository

import com.controlprestamos.data.db.dao.BlacklistDao
import com.controlprestamos.data.db.dao.LoanDao
import com.controlprestamos.data.db.dao.PaymentDao
import com.controlprestamos.data.entity.BlacklistEntity
import com.controlprestamos.data.entity.LoanEntity
import com.controlprestamos.data.entity.PaymentEntity
import com.controlprestamos.domain.model.BlacklistDraft
import com.controlprestamos.domain.model.BlacklistEntry
import com.controlprestamos.domain.model.DashboardPeriod
import com.controlprestamos.domain.model.DashboardSummary
import com.controlprestamos.domain.model.Loan
import com.controlprestamos.domain.model.LoanDraft
import com.controlprestamos.domain.model.LoanStatus
import com.controlprestamos.domain.model.PaymentRecord
import com.controlprestamos.security.CryptoManager
import com.controlprestamos.security.HashUtils
import com.controlprestamos.util.Validators
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max

class LoanRepositoryImpl(
    private val loanDao: LoanDao,
    private val paymentDao: PaymentDao,
    private val blacklistDao: BlacklistDao,
    private val cryptoManager: CryptoManager
) : LoanRepository {

    override fun observeLoans(): Flow<List<Loan>> {
        return combine(
            loanDao.observeAll(),
            paymentDao.observeAll(),
            blacklistDao.observeAllActive()
        ) { loans, payments, blacklist ->
            loans.map { loanEntity ->
                mapLoan(
                    entity = loanEntity,
                    payments = payments.filter { it.loanId == loanEntity.id },
                    blacklist = blacklist
                )
            }
        }
    }

    override fun observeLoan(loanId: Long): Flow<Loan?> {
        return combine(
            loanDao.observeById(loanId),
            paymentDao.observeByLoanId(loanId),
            blacklistDao.observeAllActive()
        ) { loan, payments, blacklist ->
            loan?.let { mapLoan(it, payments, blacklist) }
        }
    }

    override fun observeBlacklist(): Flow<List<BlacklistEntry>> {
        return blacklistDao.observeAllActive().map { entries ->
            entries.map { entity ->
                BlacklistEntry(
                    id = entity.id,
                    customerName = entity.customerName,
                    phone = cryptoManager.decrypt(entity.phoneCipher),
                    idNumber = cryptoManager.decrypt(entity.idNumberCipher),
                    reason = cryptoManager.decrypt(entity.reasonCipher),
                    addedDate = LocalDate.ofEpochDay(entity.addedDateEpochDay),
                    active = entity.active
                )
            }
        }
    }

    override fun observeDashboard(period: DashboardPeriod): Flow<DashboardSummary> {
        return observeLoans().map { loans ->
            val today = LocalDate.now()
            val currentCycleRange = currentBiweeklyRange(today)
            val selectedRange = resolveRange(period, today)
            val currentCycleKey = cycleKeyFor(today)

            val visibleLoans = loans.filter { loan ->
                loan.loanDate in selectedRange.first..selectedRange.second || loan.isOverdue
            }

            val cycleLoans = loans.filter { loan ->
                loan.loanDate in currentCycleRange.first..currentCycleRange.second
            }

            val cyclePayments = loans
                .flatMap { it.payments }
                .filter { payment ->
                    payment.paymentDate in currentCycleRange.first..currentCycleRange.second
                }

            val overdueLoans = loans.filter { it.isOverdue }
            val carryOverOverdues = overdueLoans.filter { it.loanDate.isBefore(currentCycleRange.first) }

            val totalLoaned = cycleLoans.sumOf { it.principalAmount }
            val totalRecovered = cyclePayments.sumOf { it.amount }
            val projectedInterest = cycleLoans.sumOf { it.profitAmount }
            val projectedToCollect = cycleLoans.sumOf { it.totalToRepay }
            val overdueAmount = overdueLoans.sumOf { it.pendingAmount }
            val carryOverOverdueAmount = carryOverOverdues.sumOf { it.pendingAmount }

            val activeCount = visibleLoans.count { it.status == LoanStatus.ACTIVE && !it.isOverdue }
            val collectedCount = loans.count {
                it.status == LoanStatus.COLLECTED &&
                    it.closedAt != null &&
                    it.closedAt in currentCycleRange.first..currentCycleRange.second
            }
            val lostCount = loans.count {
                it.status == LoanStatus.LOST &&
                    it.closedAt != null &&
                    it.closedAt in currentCycleRange.first..currentCycleRange.second
            }
            val archivedCount = loanCountArchivedInCycle(currentCycleKey, loans)

            val progress = if (projectedToCollect > 0.0) {
                (totalRecovered / projectedToCollect).coerceIn(0.0, 1.0).toFloat()
            } else {
                0f
            }

            DashboardSummary(
                label = period.label,
                currentCycleLabel = "Quincena actual: ${formatRange(currentCycleRange.first, currentCycleRange.second)}",
                totalLoaned = totalLoaned,
                totalRecovered = totalRecovered,
                projectedInterest = projectedInterest,
                projectedToCollect = projectedToCollect,
                overdueAmount = overdueAmount,
                overdueCount = overdueLoans.size,
                carryOverOverdueAmount = carryOverOverdueAmount,
                carryOverOverdueCount = carryOverOverdues.size,
                lossesAmount = loans
                    .filter { it.status == LoanStatus.LOST && it.pendingAmount > 0.0 }
                    .sumOf { it.pendingAmount },
                activeCount = activeCount,
                collectedCount = collectedCount,
                lostCount = lostCount,
                archivedCount = archivedCount,
                collectionProgress = progress,
                dashboardMessage = buildDashboardMessage(
                    activeCount = activeCount,
                    overdueCount = overdueLoans.size,
                    carryOverCount = carryOverOverdues.size,
                    collectedCount = collectedCount
                ),
                alerts = buildDashboardAlerts(
                    loans = loans,
                    today = today,
                    currentCycleRange = currentCycleRange
                )
            )
        }
    }

    override suspend fun addLoan(draft: LoanDraft): Result<Unit> = runCatching {
        val customerName = draft.customerName.trim()
        require(customerName.isNotBlank()) { "El nombre es obligatorio." }
        require(Validators.validPhone(draft.phone)) { "Telefono invalido." }
        require(Validators.validId(draft.idNumber)) { "Identificacion invalida." }

        val principal = draft.parsedPrincipal() ?: error("Monto prestado invalido.")
        val interestRate = draft.parsedInterest() ?: error("Porcentaje invalido.")
        val loanDate = draft.parsedLoanDate() ?: error("Fecha de prestamo invalida. Usa YYYY-MM-DD.")
        val dueDate = draft.parsedDueDate() ?: error("Fecha de vencimiento invalida. Usa YYYY-MM-DD.")

        require(principal > 0) { "El monto debe ser mayor a cero." }
        require(interestRate in 0.0..500.0) { "El porcentaje debe estar entre 0 y 500." }
        require(!dueDate.isBefore(loanDate)) { "La fecha de vencimiento no puede ser menor a la de prestamo." }

        val conflict = findBlacklistConflict(draft.idNumber, draft.phone)
        if (conflict != null) {
            error("Cliente bloqueado en lista negra: ${conflict.reason}")
        }

        val profit = principal * (interestRate / 100.0)
        val total = principal + profit
        val now = System.currentTimeMillis()

        loanDao.insert(
            LoanEntity(
                customerName = customerName,
                phoneCipher = cryptoManager.encrypt(draft.phone.trim()),
                phoneHash = shaPhoneOrBlank(draft.phone),
                idNumberCipher = cryptoManager.encrypt(draft.idNumber.trim()),
                idNumberHash = shaIdOrBlank(draft.idNumber),
                principalAmount = principal,
                interestRate = interestRate,
                profitAmount = profit,
                totalToRepay = total,
                loanDateEpochDay = loanDate.toEpochDay(),
                dueDateEpochDay = dueDate.toEpochDay(),
                status = LoanStatus.ACTIVE.name,
                observationsCipher = cryptoManager.encrypt(draft.observations.trim()),
                createdAtMillis = now,
                updatedAtMillis = now,
                originCycleKey = cycleKeyFor(loanDate)
            )
        )
    }

    override suspend fun registerPayment(
        loanId: Long,
        amount: Double,
        paymentDate: LocalDate,
        note: String
    ): Result<Unit> = runCatching {
        require(amount > 0) { "El pago debe ser mayor a cero." }

        val loan = loanDao.getById(loanId) ?: error("No se encontro el prestamo.")
        require(LoanStatus.fromStorage(loan.status) == LoanStatus.ACTIVE) {
            "Solo se pueden pagar prestamos activos."
        }

        val payments = paymentDao.getByLoanId(loanId)
        val totalPaid = payments.sumOf { it.amount }
        val pending = max(0.0, loan.totalToRepay - totalPaid)
        require(amount <= pending) { "El pago no puede superar el saldo pendiente." }

        paymentDao.insert(
            PaymentEntity(
                loanId = loanId,
                amount = amount,
                paymentDateEpochDay = paymentDate.toEpochDay(),
                noteCipher = cryptoManager.encrypt(note.trim()),
                createdAtMillis = System.currentTimeMillis(),
                paymentType = "REGULAR",
                paymentCycleKey = cycleKeyFor(paymentDate)
            )
        )

        finalizeLoanStatusAfterPayment(
            loan = loan,
            paymentDate = paymentDate,
            newPending = max(0.0, pending - amount)
        )
    }

    override suspend fun registerPaymentWithExchange(
        loanId: Long,
        amountUsd: Double,
        exchangeRate: Double,
        paymentDate: LocalDate,
        note: String,
        wasReminderMessageSent: Boolean
    ): Result<Unit> = runCatching {
        require(amountUsd > 0) { "El pago debe ser mayor a cero." }
        require(exchangeRate > 0) { "La tasa debe ser mayor a cero." }

        val loan = loanDao.getById(loanId) ?: error("No se encontro el prestamo.")
        require(LoanStatus.fromStorage(loan.status) == LoanStatus.ACTIVE) {
            "Solo se pueden pagar prestamos activos."
        }

        val payments = paymentDao.getByLoanId(loanId)
        val totalPaid = payments.sumOf { it.amount }
        val pending = max(0.0, loan.totalToRepay - totalPaid)
        require(amountUsd <= pending) { "El pago no puede superar el saldo pendiente." }

        val amountVes = amountUsd * exchangeRate

        paymentDao.insert(
            PaymentEntity(
                loanId = loanId,
                amount = amountUsd,
                paymentDateEpochDay = paymentDate.toEpochDay(),
                noteCipher = cryptoManager.encrypt(note.trim()),
                createdAtMillis = System.currentTimeMillis(),
                exchangeRateUsed = exchangeRate,
                amountVes = amountVes,
                paymentType = "REGULAR",
                wasReminderMessageSent = wasReminderMessageSent,
                paymentCycleKey = cycleKeyFor(paymentDate)
            )
        )

        loanDao.update(
            loan.copy(
                updatedAtMillis = System.currentTimeMillis(),
                manualExchangeRate = exchangeRate,
                calculatedAmountVes = amountVes,
                reminderSent = loan.reminderSent || wasReminderMessageSent,
                lastReminderDateEpochDay = if (wasReminderMessageSent) paymentDate.toEpochDay() else loan.lastReminderDateEpochDay
            )
        )

        finalizeLoanStatusAfterPayment(
            loan = loanDao.getById(loanId) ?: loan,
            paymentDate = paymentDate,
            newPending = max(0.0, pending - amountUsd)
        )
    }

    override suspend fun markLoanAsCollected(
        loanId: Long,
        collectedDate: LocalDate,
        note: String
    ): Result<Unit> = runCatching {
        val loan = loanDao.getById(loanId) ?: error("No se encontro el prestamo.")
        require(LoanStatus.fromStorage(loan.status) == LoanStatus.ACTIVE) {
            "Solo los prestamos activos pueden marcarse como cobrados."
        }

        val payments = paymentDao.getByLoanId(loanId)
        val totalPaid = payments.sumOf { it.amount }
        val pending = max(0.0, loan.totalToRepay - totalPaid)

        if (pending > 0.0001) {
            paymentDao.insert(
                PaymentEntity(
                    loanId = loanId,
                    amount = pending,
                    paymentDateEpochDay = collectedDate.toEpochDay(),
                    noteCipher = cryptoManager.encrypt(
                        if (note.isBlank()) "Pago de cierre generado al marcar como cobrado."
                        else "Pago de cierre: ${note.trim()}"
                    ),
                    createdAtMillis = System.currentTimeMillis(),
                    paymentType = "CLOSING",
                    paymentCycleKey = cycleKeyFor(collectedDate)
                )
            )
        }

        val previousObservations = cryptoManager.decrypt(loan.observationsCipher)
        loanDao.update(
            loan.copy(
                status = LoanStatus.COLLECTED.name,
                updatedAtMillis = System.currentTimeMillis(),
                closedAtEpochDay = collectedDate.toEpochDay(),
                closedCycleKey = cycleKeyFor(collectedDate),
                collectedAtEpochDay = collectedDate.toEpochDay(),
                observationsCipher = cryptoManager.encrypt(
                    listOf(
                        previousObservations,
                        if (note.isBlank()) "Marcado manualmente como cobrado."
                        else "Cobrado manualmente: ${note.trim()}"
                    ).filter { it.isNotBlank() }.joinToString(" | ")
                )
            )
        )
    }

    override suspend fun markLoanAsCollectedWithExchange(
        loanId: Long,
        collectedDate: LocalDate,
        exchangeRate: Double,
        note: String
    ): Result<Unit> = runCatching {
        require(exchangeRate > 0) { "La tasa debe ser mayor a cero." }

        val loan = loanDao.getById(loanId) ?: error("No se encontro el prestamo.")
        require(LoanStatus.fromStorage(loan.status) == LoanStatus.ACTIVE) {
            "Solo los prestamos activos pueden marcarse como cobrados."
        }

        val payments = paymentDao.getByLoanId(loanId)
        val totalPaid = payments.sumOf { it.amount }
        val pending = max(0.0, loan.totalToRepay - totalPaid)
        val amountVes = pending * exchangeRate

        if (pending > 0.0001) {
            paymentDao.insert(
                PaymentEntity(
                    loanId = loanId,
                    amount = pending,
                    paymentDateEpochDay = collectedDate.toEpochDay(),
                    noteCipher = cryptoManager.encrypt(
                        if (note.isBlank()) "Pago de cierre generado al marcar como cobrado."
                        else "Pago de cierre: ${note.trim()}"
                    ),
                    createdAtMillis = System.currentTimeMillis(),
                    exchangeRateUsed = exchangeRate,
                    amountVes = amountVes,
                    paymentType = "CLOSING",
                    paymentCycleKey = cycleKeyFor(collectedDate)
                )
            )
        }

        val previousObservations = cryptoManager.decrypt(loan.observationsCipher)
        loanDao.update(
            loan.copy(
                status = LoanStatus.COLLECTED.name,
                updatedAtMillis = System.currentTimeMillis(),
                closedAtEpochDay = collectedDate.toEpochDay(),
                closedCycleKey = cycleKeyFor(collectedDate),
                collectedAtEpochDay = collectedDate.toEpochDay(),
                manualExchangeRate = exchangeRate,
                calculatedAmountVes = amountVes,
                observationsCipher = cryptoManager.encrypt(
                    listOf(
                        previousObservations,
                        "Cobrado con tasa manual ${exchangeRate.format2()}",
                        if (note.isBlank()) "" else note.trim()
                    ).filter { it.isNotBlank() }.joinToString(" | ")
                )
            )
        )
    }

    override suspend fun markLoanAsLost(loanId: Long, note: String): Result<Unit> = runCatching {
        val loan = loanDao.getById(loanId) ?: error("No se encontro el prestamo.")
        require(LoanStatus.fromStorage(loan.status) == LoanStatus.ACTIVE) {
            "Solo los prestamos activos pueden marcarse como perdidos."
        }

        val previousObservations = cryptoManager.decrypt(loan.observationsCipher)
        val today = LocalDate.now()

        loanDao.update(
            loan.copy(
                status = LoanStatus.LOST.name,
                updatedAtMillis = System.currentTimeMillis(),
                closedAtEpochDay = today.toEpochDay(),
                closedCycleKey = cycleKeyFor(today),
                observationsCipher = cryptoManager.encrypt(
                    listOf(
                        previousObservations,
                        if (note.isBlank()) "" else "Perdida: ${note.trim()}"
                    ).filter { it.isNotBlank() }.joinToString(" | ")
                )
            )
        )
    }

    override suspend fun markReminderSent(
        loanId: Long,
        reminderDate: LocalDate
    ): Result<Unit> = runCatching {
        val loan = loanDao.getById(loanId) ?: error("No se encontro el prestamo.")
        val previousObservations = cryptoManager.decrypt(loan.observationsCipher)

        loanDao.update(
            loan.copy(
                updatedAtMillis = System.currentTimeMillis(),
                reminderSent = true,
                lastReminderDateEpochDay = reminderDate.toEpochDay(),
                observationsCipher = cryptoManager.encrypt(
                    listOf(
                        previousObservations,
                        "Recordatorio de cobro enviado el ${formatDate(reminderDate)}"
                    ).filter { it.isNotBlank() }.joinToString(" | ")
                )
            )
        )
    }

    override suspend fun applyLateFeePercent(
        loanId: Long,
        percent: Double
    ): Result<Unit> = runCatching {
        require(percent > 0) { "El porcentaje debe ser mayor a cero." }

        val loan = loanDao.getById(loanId) ?: error("No se encontro el prestamo.")
        require(LoanStatus.fromStorage(loan.status) == LoanStatus.ACTIVE) {
            "Solo se puede aplicar recargo a prestamos activos."
        }

        val payments = paymentDao.getByLoanId(loanId)
        val totalPaid = payments.sumOf { it.amount }
        val pending = max(0.0, loan.totalToRepay - totalPaid)
        require(pending > 0.0) { "El prestamo no tiene saldo pendiente." }

        val extra = pending * (percent / 100.0)
        val previousObservations = cryptoManager.decrypt(loan.observationsCipher)

        loanDao.update(
            loan.copy(
                totalToRepay = loan.totalToRepay + extra,
                profitAmount = loan.profitAmount + extra,
                lateFeePercent = loan.lateFeePercent + percent,
                updatedAtMillis = System.currentTimeMillis(),
                observationsCipher = cryptoManager.encrypt(
                    listOf(
                        previousObservations,
                        "Recargo por retraso aplicado: ${percent.format2()}%"
                    ).filter { it.isNotBlank() }.joinToString(" | ")
                )
            )
        )
    }

    override suspend fun applyLateFeeFixedAmount(
        loanId: Long,
        amount: Double
    ): Result<Unit> = runCatching {
        require(amount > 0) { "El recargo debe ser mayor a cero." }

        val loan = loanDao.getById(loanId) ?: error("No se encontro el prestamo.")
        require(LoanStatus.fromStorage(loan.status) == LoanStatus.ACTIVE) {
            "Solo se puede aplicar recargo a prestamos activos."
        }

        val previousObservations = cryptoManager.decrypt(loan.observationsCipher)

        loanDao.update(
            loan.copy(
                totalToRepay = loan.totalToRepay + amount,
                profitAmount = loan.profitAmount + amount,
                lateFeeFixedAmount = loan.lateFeeFixedAmount + amount,
                updatedAtMillis = System.currentTimeMillis(),
                observationsCipher = cryptoManager.encrypt(
                    listOf(
                        previousObservations,
                        "Recargo fijo por retraso aplicado: ${amount.format2()} USD"
                    ).filter { it.isNotBlank() }.joinToString(" | ")
                )
            )
        )
    }

    override suspend fun archiveLoan(
        loanId: Long,
        archiveDate: LocalDate
    ): Result<Unit> = runCatching {
        val loan = loanDao.getById(loanId) ?: error("No se encontro el prestamo.")
        val status = LoanStatus.fromStorage(loan.status)
        require(status == LoanStatus.COLLECTED || status == LoanStatus.LOST) {
            "Solo se pueden archivar prestamos cobrados o perdidos."
        }

        loanDao.update(
            loan.copy(
                isArchived = true,
                archivedAtEpochDay = archiveDate.toEpochDay(),
                updatedAtMillis = System.currentTimeMillis(),
                closedCycleKey = loan.closedCycleKey ?: cycleKeyFor(archiveDate)
            )
        )
    }

    override suspend fun addBlacklist(draft: BlacklistDraft): Result<Unit> = runCatching {
        val name = draft.customerName.trim()
        require(name.isNotBlank()) { "El nombre es obligatorio." }
        require(Validators.validPhone(draft.phone)) { "Telefono invalido." }
        require(Validators.validId(draft.idNumber)) { "Identificacion invalida." }
        require(draft.reason.trim().isNotBlank()) { "El motivo es obligatorio." }
        val addedDate = draft.parsedAddedDate() ?: error("Fecha invalida. Usa YYYY-MM-DD.")

        blacklistDao.insert(
            BlacklistEntity(
                customerName = name,
                phoneCipher = cryptoManager.encrypt(draft.phone.trim()),
                phoneHash = shaPhoneOrBlank(draft.phone),
                idNumberCipher = cryptoManager.encrypt(draft.idNumber.trim()),
                idNumberHash = shaIdOrBlank(draft.idNumber),
                reasonCipher = cryptoManager.encrypt(draft.reason.trim()),
                addedDateEpochDay = addedDate.toEpochDay(),
                active = true
            )
        )
    }

    override suspend fun deactivateBlacklist(id: Long) {
        blacklistDao.setActive(id, false)
    }

    override suspend fun findBlacklistConflict(idNumber: String, phone: String): BlacklistEntry? {
        val idHash = shaIdOrBlank(idNumber)
        val phoneHash = shaPhoneOrBlank(phone)

        return blacklistDao.getAllActive()
            .firstOrNull { entity ->
                (idHash.isNotBlank() && entity.idNumberHash == idHash) ||
                    (phoneHash.isNotBlank() && entity.phoneHash == phoneHash)
            }
            ?.let { entity ->
                BlacklistEntry(
                    id = entity.id,
                    customerName = entity.customerName,
                    phone = cryptoManager.decrypt(entity.phoneCipher),
                    idNumber = cryptoManager.decrypt(entity.idNumberCipher),
                    reason = cryptoManager.decrypt(entity.reasonCipher),
                    addedDate = LocalDate.ofEpochDay(entity.addedDateEpochDay),
                    active = entity.active
                )
            }
    }

    override suspend fun seedIfEmpty() {
        if (loanDao.count() > 0 || blacklistDao.count() > 0) return

        val now = System.currentTimeMillis()

        fun enc(value: String): String = cryptoManager.encrypt(value)

        suspend fun insertBlacklist(
            name: String,
            reason: String,
            idNumber: String = "",
            phone: String = "",
            addedDate: String = "2026-03-20"
        ) {
            blacklistDao.insert(
                BlacklistEntity(
                    customerName = name.trim(),
                    phoneCipher = enc(phone),
                    phoneHash = shaPhoneOrBlank(phone),
                    idNumberCipher = enc(idNumber),
                    idNumberHash = shaIdOrBlank(idNumber),
                    reasonCipher = enc(reason.trim()),
                    addedDateEpochDay = LocalDate.parse(addedDate).toEpochDay(),
                    active = true
                )
            )
        }

        suspend fun insertLoan(
            customerName: String,
            principalAmount: Double,
            interestAmount: Double,
            totalToRepay: Double,
            loanDate: String,
            dueDate: String,
            lender: String,
            status: String = "PENDIENTE",
            observations: String = "",
            idNumber: String = "",
            phone: String = ""
        ) {
            val interestRate =
                if (principalAmount > 0.0) (interestAmount / principalAmount) * 100.0 else 0.0

            val loanLocalDate = LocalDate.parse(loanDate)
            val dueLocalDate = LocalDate.parse(dueDate)

            loanDao.insert(
                LoanEntity(
                    customerName = customerName.trim(),
                    phoneCipher = enc(phone),
                    phoneHash = shaPhoneOrBlank(phone),
                    idNumberCipher = enc(idNumber),
                    idNumberHash = shaIdOrBlank(idNumber),
                    principalAmount = principalAmount,
                    interestRate = interestRate,
                    profitAmount = interestAmount,
                    totalToRepay = totalToRepay,
                    loanDateEpochDay = loanLocalDate.toEpochDay(),
                    dueDateEpochDay = dueLocalDate.toEpochDay(),
                    status = when (status.trim().uppercase()) {
                        "COBRADO" -> LoanStatus.COLLECTED.name
                        "PERDIDO" -> LoanStatus.LOST.name
                        else -> LoanStatus.ACTIVE.name
                    },
                    observationsCipher = enc(
                        buildString {
                            append("Prestamista: $lender")
                            append(" | Estado original Excel: ${status.trim()}")
                            if (observations.isNotBlank()) {
                                append(" | ")
                                append(observations.trim())
                            }
                        }
                    ),
                    createdAtMillis = now,
                    updatedAtMillis = now,
                    originCycleKey = cycleKeyFor(loanLocalDate),
                    closedAtEpochDay = when (status.trim().uppercase()) {
                        "COBRADO", "PERDIDO" -> dueLocalDate.toEpochDay()
                        else -> null
                    },
                    closedCycleKey = when (status.trim().uppercase()) {
                        "COBRADO", "PERDIDO" -> cycleKeyFor(dueLocalDate)
                        else -> null
                    },
                    collectedAtEpochDay = when (status.trim().uppercase()) {
                        "COBRADO" -> dueLocalDate.toEpochDay()
                        else -> null
                    }
                )
            )
        }

        insertBlacklist("EMERSON SOJO", "PENDIENTE CEDULA")
        insertBlacklist("CAP CASTRO", "PENDIENTE CEDULA")
        insertBlacklist("RAFAEL STUARD", "PENDIENTE CEDULA")
        insertBlacklist("AVILA FERNANDEZ", "PENDIENTE CEDULA")
        insertBlacklist("JULIO SEGOVIA", "PENDIENTE CEDULA")
        insertBlacklist("SAMARY TERRASA BAR", "PENDIENTE CEDULA")
        insertBlacklist("RODRIGUEZ PETIT", "PENDIENTE CEDULA")
        insertBlacklist("TAIMAR CABELLO", "PENDIENTE CEDULA")
        insertBlacklist("SARACUAL TTE DGCIM", "PENDIENTE CEDULA")
        insertBlacklist("BAEZ FRANYER BANGU", "PENDIENTE CEDULA")
        insertBlacklist("RUMION CARRION BANGU", "PENDIENTE CEDULA")
        insertBlacklist("EZEQUIEL FUENTES BANGU", "PENDIENTE CEDULA")
        insertBlacklist("MARCANO RUMION BANGU", "PENDIENTE CEDULA")
        insertBlacklist("TN HERRERA ESTAFADOR DE BANGU", "PENDIENTE CEDULA")
        insertBlacklist("DUBRASKA PAGODA", "PENDIENTE CEDULA")

        insertLoan(
            customerName = "JAVI EJERCITO",
            principalAmount = 100.0,
            interestAmount = 50.0,
            totalToRepay = 150.0,
            loanDate = "2026-03-15",
            dueDate = "2026-03-31",
            lender = "Freilinyer",
            status = "PENDIENTE",
            observations = "Hoja origen: FREILINYER"
        )

        insertLoan(
            customerName = "LIVIA",
            principalAmount = 60.0,
            interestAmount = 30.0,
            totalToRepay = 90.0,
            loanDate = "2026-03-15",
            dueDate = "2026-04-02",
            lender = "Freilinyer",
            status = "PENDIENTE",
            observations = "Hoja origen: FREILINYER | Fecha de reembolso corregida"
        )

        insertLoan(
            customerName = "MARLON",
            principalAmount = 40.0,
            interestAmount = 20.0,
            totalToRepay = 60.0,
            loanDate = "2026-03-15",
            dueDate = "2026-04-02",
            lender = "Freilinyer",
            status = "PENDIENTE",
            observations = "Hoja origen: FREILINYER | Fecha de reembolso corregida"
        )

        insertLoan(
            customerName = "PEREZ ITURRIAGO",
            principalAmount = 100.0,
            interestAmount = 50.0,
            totalToRepay = 150.0,
            loanDate = "2026-03-15",
            dueDate = "2026-03-31",
            lender = "Freilinyer",
            status = "PENDIENTE",
            observations = "Hoja origen: FREILINYER"
        )

        insertLoan(
            customerName = "AMAYRUS",
            principalAmount = 50.0,
            interestAmount = 10.0,
            totalToRepay = 60.0,
            loanDate = "2026-03-15",
            dueDate = "2026-03-31",
            lender = "Freilinyer",
            status = "PENDIENTE",
            observations = "Hoja origen: FREILINYER"
        )

        insertLoan(
            customerName = "CAP CASTRO",
            principalAmount = 90.0,
            interestAmount = 45.0,
            totalToRepay = 135.0,
            loanDate = "2026-03-18",
            dueDate = "2026-03-31",
            lender = "Freilinyer",
            status = "PENDIENTE",
            observations = "Hoja origen: FREILINYER"
        )

        insertLoan(
            customerName = "MONTEIRO",
            principalAmount = 80.0,
            interestAmount = 40.0,
            totalToRepay = 120.0,
            loanDate = "2026-03-18",
            dueDate = "2026-03-31",
            lender = "Freilinyer",
            status = "PENDIENTE",
            observations = "Hoja origen: FREILINYER"
        )

        insertLoan(
            customerName = "DIAZ",
            principalAmount = 100.0,
            interestAmount = 40.0,
            totalToRepay = 40.0,
            loanDate = "2026-03-18",
            dueDate = "2026-03-31",
            lender = "Freilinyer",
            status = "PENDIENTE",
            observations = "Hoja origen: FREILINYER | Total a recibir conservado tal como aparece en el Excel"
        )

        insertLoan(
            customerName = "CAP ARIAS",
            principalAmount = 75.0,
            interestAmount = 51.0,
            totalToRepay = 126.0,
            loanDate = "2026-03-18",
            dueDate = "2026-03-31",
            lender = "Freilinyer",
            status = "PENDIENTE",
            observations = "Hoja origen: FREILINYER"
        )

        insertLoan(
            customerName = "RICHAR",
            principalAmount = 500.0,
            interestAmount = 200.0,
            totalToRepay = 700.0,
            loanDate = "2026-02-25",
            dueDate = "2026-03-15",
            lender = "Freilinyer",
            status = "PENDIENTE",
            observations = "Hoja origen: FREILINYER"
        )

        insertLoan(
            customerName = "ALIS DUEÑO",
            principalAmount = 100.0,
            interestAmount = 50.0,
            totalToRepay = 150.0,
            loanDate = "2026-03-20",
            dueDate = "2026-03-31",
            lender = "Freilinyer",
            status = "PENDIENTE",
            observations = "Hoja origen: FREILINYER"
        )

        insertLoan(
            customerName = "ALVARO GIL",
            principalAmount = 100.0,
            interestAmount = 75.0,
            totalToRepay = 175.0,
            loanDate = "2026-01-30",
            dueDate = "2026-02-28",
            lender = "Freilinyer",
            status = "PENDIENTE",
            observations = "Hoja origen: FREILINYER"
        )

        insertLoan(
            customerName = "DARLING",
            principalAmount = 59.0,
            interestAmount = 0.0,
            totalToRepay = 59.0,
            loanDate = "2026-03-31",
            dueDate = "2026-03-31",
            lender = "Freilinyer",
            status = "PENDIENTE",
            observations = "Hoja origen: FREILINYER | Retraso reportado en el Excel: 59"
        )

        insertLoan(
            customerName = "RONDON",
            principalAmount = 150.0,
            interestAmount = 75.0,
            totalToRepay = 120.0,
            loanDate = "2026-01-27",
            dueDate = "2026-02-15",
            lender = "Freilinyer",
            status = "PENDIENTE",
            observations = "Hoja origen: FREILINYER | Retraso reportado en el Excel: 120 | Total a recibir conservado tal como aparece en el Excel"
        )

        insertLoan(
            customerName = "SANCHES HEREDIA",
            principalAmount = 10.0,
            interestAmount = 5.0,
            totalToRepay = 15.0,
            loanDate = "2026-03-01",
            dueDate = "2026-03-15",
            lender = "Daniela",
            status = "PENDIENTE",
            observations = "Hoja origen: DANIELA"
        )
    }

    private suspend fun finalizeLoanStatusAfterPayment(
        loan: LoanEntity,
        paymentDate: LocalDate,
        newPending: Double
    ) {
        if (newPending <= 0.0001) {
            loanDao.update(
                loan.copy(
                    status = LoanStatus.COLLECTED.name,
                    updatedAtMillis = System.currentTimeMillis(),
                    closedAtEpochDay = paymentDate.toEpochDay(),
                    closedCycleKey = cycleKeyFor(paymentDate),
                    collectedAtEpochDay = paymentDate.toEpochDay()
                )
            )
        } else {
            loanDao.update(
                loan.copy(
                    updatedAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    private fun mapLoan(
        entity: LoanEntity,
        payments: List<PaymentEntity>,
        blacklist: List<BlacklistEntity>
    ): Loan {
        val mappedPayments = payments.map {
            PaymentRecord(
                id = it.id,
                loanId = it.loanId,
                amount = it.amount,
                paymentDate = LocalDate.ofEpochDay(it.paymentDateEpochDay),
                note = cryptoManager.decrypt(it.noteCipher)
            )
        }

        val totalPaid = mappedPayments.sumOf { it.amount }
        val pending = max(0.0, entity.totalToRepay - totalPaid)
        val dueDate = LocalDate.ofEpochDay(entity.dueDateEpochDay)
        val status = LoanStatus.fromStorage(entity.status)
        val overdue = status == LoanStatus.ACTIVE && pending > 0.0 && dueDate.isBefore(LocalDate.now())

        val blacklisted = blacklist.any {
            (entity.idNumberHash.isNotBlank() && it.idNumberHash == entity.idNumberHash) ||
                (entity.phoneHash.isNotBlank() && it.phoneHash == entity.phoneHash)
        }

        return Loan(
            id = entity.id,
            customerName = entity.customerName,
            phone = cryptoManager.decrypt(entity.phoneCipher),
            idNumber = cryptoManager.decrypt(entity.idNumberCipher),
            principalAmount = entity.principalAmount,
            interestRate = entity.interestRate,
            profitAmount = entity.profitAmount,
            totalToRepay = entity.totalToRepay,
            loanDate = LocalDate.ofEpochDay(entity.loanDateEpochDay),
            dueDate = dueDate,
            status = status,
            observations = cryptoManager.decrypt(entity.observationsCipher),
            payments = mappedPayments.sortedByDescending { it.paymentDate },
            totalPaid = totalPaid,
            pendingAmount = pending,
            isOverdue = overdue,
            daysOverdue = if (overdue) ChronoUnit.DAYS.between(dueDate, LocalDate.now()) else 0L,
            isBlacklisted = blacklisted,
            closedAt = entity.closedAtEpochDay?.let(LocalDate::ofEpochDay)
        )
    }

    private fun resolveRange(period: DashboardPeriod, today: LocalDate): Pair<LocalDate, LocalDate> {
        return if (period == DashboardPeriod.BIWEEKLY) {
            currentBiweeklyRange(today)
        } else {
            today.minusDays(period.daysBack - 1) to today
        }
    }

    private fun currentBiweeklyRange(date: LocalDate): Pair<LocalDate, LocalDate> {
        return if (date.dayOfMonth <= 15) {
            date.withDayOfMonth(1) to date.withDayOfMonth(15)
        } else {
            val endOfMonth = YearMonth.from(date).atEndOfMonth()
            date.withDayOfMonth(16) to endOfMonth
        }
    }

    private fun cycleKeyFor(date: LocalDate): String {
        val half = if (date.dayOfMonth <= 15) "Q1" else "Q2"
        return "${date.year}-${date.monthValue.toString().padStart(2, '0')}-$half"
    }

    private fun formatRange(start: LocalDate, end: LocalDate): String {
        return "${formatDate(start)} - ${formatDate(end)}"
    }

    private fun formatDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    }

    private fun loanCountArchivedInCycle(cycleKey: String, loans: List<Loan>): Int {
        return loans.count { loan ->
            loan.closedAt != null &&
                cycleKeyFor(loan.closedAt) == cycleKey &&
                loan.status in listOf(LoanStatus.COLLECTED, LoanStatus.LOST)
        }
    }

    private fun buildDashboardMessage(
        activeCount: Int,
        overdueCount: Int,
        carryOverCount: Int,
        collectedCount: Int
    ): String {
        return when {
            overdueCount > 0 && carryOverCount > 0 ->
                "La quincena actual tiene retrasos activos y además arrastra $carryOverCount retraso(s) de quincenas anteriores."
            overdueCount > 0 ->
                "Hay pagos retrasados que requieren seguimiento inmediato en la quincena actual."
            activeCount > 0 ->
                "La cartera sigue en movimiento. Hay préstamos activos en seguimiento durante esta quincena."
            collectedCount > 0 ->
                "Se registran cobros completados en la quincena actual."
            else ->
                "Aún no hay suficiente movimiento en esta quincena para mostrar una lectura más amplia."
        }
    }

    private fun buildDashboardAlerts(
        loans: List<Loan>,
        today: LocalDate,
        currentCycleRange: Pair<LocalDate, LocalDate>
    ): List<String> {
        val dueToday = loans.count {
            it.status == LoanStatus.ACTIVE && it.pendingAmount > 0.0 && it.dueDate == today
        }
        val dueTomorrow = loans.count {
            it.status == LoanStatus.ACTIVE && it.pendingAmount > 0.0 && it.dueDate == today.plusDays(1)
        }
        val overdueCarry = loans.count {
            it.isOverdue && it.loanDate.isBefore(currentCycleRange.first)
        }

        return buildList {
            if (dueToday > 0) add("Hoy debes gestionar $dueToday cobro(s).")
            if (dueTomorrow > 0) add("Mañana vencen $dueTomorrow préstamo(s).")
            if (overdueCarry > 0) add("Tienes $overdueCarry retraso(s) arrastrado(s) de quincenas anteriores.")
        }
    }

    private fun shaPhoneOrBlank(value: String): String {
        val clean = value.trim()
        return if (clean.isBlank()) "" else HashUtils.sha256(HashUtils.normalizePhone(clean))
    }

    private fun shaIdOrBlank(value: String): String {
        val clean = value.trim()
        return if (clean.isBlank()) "" else HashUtils.sha256(HashUtils.normalizeId(clean))
    }

    private fun Double.format2(): String = String.format("%.2f", this)
}