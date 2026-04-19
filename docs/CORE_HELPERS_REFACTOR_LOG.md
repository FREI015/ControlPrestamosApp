# Core Helpers Refactor Log

Generated: 2026-04-19 17:10:26

## Refactor completed

Extracted formatting and validation helpers out of core.design.AppFields.kt.

## New packages

- com.controlprestamos.core.format
- com.controlprestamos.core.validation

## New files

- core/format/AppFormatters.kt
- core/validation/AppInputValidation.kt

## Updated file

- core/design/AppFields.kt now keeps UI-related date field components only.

## Moved helpers

### core.format

- formatMoney
- formatPercent

### core.validation

- sanitizeDecimalInput
- sanitizeIntegerInput
- sanitizePhoneInput
- normalizeTextInput
- sanitizeDateInput
- parseMoneyOrNull
- parsePercentOrNull
- parseIsoDateOrNull
- isValidIsoDate
- isValidPositiveAmount
- isValidNonNegativePercent
- isDateRangeValid
- validateRequiredText
- validateMinLength
- validateNumericRange
- validateIdNumberOptional
- validatePhoneOptional
- validateBankAccountOptional
- validateTextMaxLength
- validateLoanForm
- validateProfileForm
- validateBlacklistForm
- validateFrequentUserForm
- validateReferralForm

## Validation

- gradlew clean --no-daemon
- gradlew assembleDebug --no-daemon

## Next recommended refactor

- Reduce wildcard imports gradually.
- Move another feature group from app into features, probably profile or backup.
- Keep UI components in core.design; keep non-UI helpers out of design.
