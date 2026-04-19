# Architecture Audit - ControlPrestamosApp

Generated: 2026-04-19 14:49:21

## Executive Summary

This audit reviews the current Kotlin/Android project structure without moving functional code.

- Kotlin files analyzed: **42**
- Total Kotlin lines: **11646**
- Average lines per Kotlin file: **277.29**
- Large files >= 250 lines: **28**
- Critical files >= 500 lines: **2**
- Screen files detected: **26**
- Store files detected: **6**

## Package Concentration

| Package | Files |
|---|---:|
| `com.controlprestamos.app` | 40 |
| `com.controlprestamos` | 2 |

## Responsibility Categories

| Category | Files |
|---|---:|
| `ui-screen` | 26 |
| `data-store` | 6 |
| `manager` | 2 |
| `app-entry` | 2 |
| `ui-core` | 2 |
| `navigation` | 2 |
| `android-receiver` | 2 |

## Feature Guess

| Feature Area | Files |
|---|---:|
| `loans` | 16 |
| `clients-profile` | 5 |
| `uncategorized` | 5 |
| `security-reminders` | 4 |
| `backup-trash` | 4 |
| `analytics-dashboard` | 2 |
| `navigation` | 2 |
| `core-ui` | 2 |
| `app` | 1 |
| `search` | 1 |

## Largest Kotlin Files

| Severity | Lines | File | Category | Feature Guess |
|---|---:|---|---|---|
| CRITICAL | 574 | `app/src/main/java/com/controlprestamos/app/LoansScreen.kt` | ui-screen | loans |
| CRITICAL | 545 | `app/src/main/java/com/controlprestamos/app/ReferralsScreen.kt` | ui-screen | clients-profile |
| HIGH | 473 | `app/src/main/java/com/controlprestamos/app/BackupExportScreen.kt` | ui-screen | backup-trash |
| HIGH | 467 | `app/src/main/java/com/controlprestamos/app/LoanDetailScreen.kt` | ui-screen | loans |
| HIGH | 461 | `app/src/main/java/com/controlprestamos/app/AppDesign.kt` | ui-core | core-ui |
| HIGH | 455 | `app/src/main/java/com/controlprestamos/app/LoanCollectionNoticeScreen.kt` | ui-screen | loans |
| HIGH | 421 | `app/src/main/java/com/controlprestamos/app/SessionBackupTrashStore.kt` | data-store | backup-trash |
| HIGH | 420 | `app/src/main/java/com/controlprestamos/app/RestoreBackupScreen.kt` | ui-screen | backup-trash |
| HIGH | 416 | `app/src/main/java/com/controlprestamos/app/AppFields.kt` | ui-core | core-ui |
| HIGH | 398 | `app/src/main/java/com/controlprestamos/app/FrequentUsersScreen.kt` | ui-screen | clients-profile |
| HIGH | 363 | `app/src/main/java/com/controlprestamos/app/EditLoanScreen.kt` | ui-screen | loans |
| HIGH | 354 | `app/src/main/java/com/controlprestamos/app/SessionLoanStore.kt` | data-store | loans |
| MEDIUM | 338 | `app/src/main/java/com/controlprestamos/app/DashboardScreen.kt` | ui-screen | analytics-dashboard |
| MEDIUM | 338 | `app/src/main/java/com/controlprestamos/app/NewLoanScreen.kt` | ui-screen | loans |
| MEDIUM | 314 | `app/src/main/java/com/controlprestamos/app/SessionStore.kt` | data-store | uncategorized |
| MEDIUM | 312 | `app/src/main/java/com/controlprestamos/app/BlacklistScreen.kt` | ui-screen | clients-profile |
| MEDIUM | 307 | `app/src/main/java/com/controlprestamos/app/WeeklyViewScreen.kt` | ui-screen | loans |
| MEDIUM | 296 | `app/src/main/java/com/controlprestamos/app/MonthlyViewScreen.kt` | ui-screen | loans |
| MEDIUM | 287 | `app/src/main/java/com/controlprestamos/app/LoanReminderManager.kt` | manager | loans |
| MEDIUM | 283 | `app/src/main/java/com/controlprestamos/app/MoreScreen.kt` | ui-screen | uncategorized |
| MEDIUM | 280 | `app/src/main/java/com/controlprestamos/app/GlobalSearchScreen.kt` | ui-screen | search |
| MEDIUM | 273 | `app/src/main/java/com/controlprestamos/app/DailySummaryScreen.kt` | ui-screen | loans |
| MEDIUM | 271 | `app/src/main/java/com/controlprestamos/app/CollectionAgendaScreen.kt` | ui-screen | loans |
| MEDIUM | 271 | `app/src/main/java/com/controlprestamos/app/TrashScreen.kt` | ui-screen | backup-trash |
| MEDIUM | 268 | `app/src/main/java/com/controlprestamos/app/NavGraph.kt` | navigation | navigation |

## Screen Files

| Lines | File | Composables | Functions | SessionStore Mentions | Nav Mentions |
|---:|---|---:|---:|---:|---:|
| 574 | `app/src/main/java/com/controlprestamos/app/LoansScreen.kt` | 6 | 19 | 9 | 6 |
| 545 | `app/src/main/java/com/controlprestamos/app/ReferralsScreen.kt` | 4 | 10 | 6 | 0 |
| 473 | `app/src/main/java/com/controlprestamos/app/BackupExportScreen.kt` | 2 | 15 | 20 | 5 |
| 467 | `app/src/main/java/com/controlprestamos/app/LoanDetailScreen.kt` | 4 | 6 | 12 | 10 |
| 455 | `app/src/main/java/com/controlprestamos/app/LoanCollectionNoticeScreen.kt` | 3 | 12 | 4 | 8 |
| 420 | `app/src/main/java/com/controlprestamos/app/RestoreBackupScreen.kt` | 1 | 12 | 4 | 5 |
| 398 | `app/src/main/java/com/controlprestamos/app/FrequentUsersScreen.kt` | 1 | 6 | 6 | 5 |
| 363 | `app/src/main/java/com/controlprestamos/app/EditLoanScreen.kt` | 1 | 4 | 6 | 8 |
| 338 | `app/src/main/java/com/controlprestamos/app/NewLoanScreen.kt` | 1 | 4 | 4 | 7 |
| 338 | `app/src/main/java/com/controlprestamos/app/DashboardScreen.kt` | 2 | 9 | 4 | 0 |
| 312 | `app/src/main/java/com/controlprestamos/app/BlacklistScreen.kt` | 1 | 5 | 6 | 0 |
| 307 | `app/src/main/java/com/controlprestamos/app/WeeklyViewScreen.kt` | 3 | 3 | 10 | 11 |
| 296 | `app/src/main/java/com/controlprestamos/app/MonthlyViewScreen.kt` | 3 | 3 | 10 | 11 |
| 283 | `app/src/main/java/com/controlprestamos/app/MoreScreen.kt` | 6 | 4 | 5 | 19 |
| 280 | `app/src/main/java/com/controlprestamos/app/GlobalSearchScreen.kt` | 1 | 5 | 8 | 7 |
| 273 | `app/src/main/java/com/controlprestamos/app/DailySummaryScreen.kt` | 4 | 7 | 6 | 7 |
| 271 | `app/src/main/java/com/controlprestamos/app/CollectionAgendaScreen.kt` | 3 | 3 | 11 | 13 |
| 271 | `app/src/main/java/com/controlprestamos/app/TrashScreen.kt` | 3 | 7 | 7 | 5 |
| 261 | `app/src/main/java/com/controlprestamos/app/ReminderSettingsScreen.kt` | 1 | 2 | 0 | 5 |
| 255 | `app/src/main/java/com/controlprestamos/app/HistoryScreen.kt` | 3 | 4 | 8 | 0 |
| 227 | `app/src/main/java/com/controlprestamos/app/EditProfileScreen.kt` | 1 | 1 | 4 | 6 |
| 224 | `app/src/main/java/com/controlprestamos/app/LockScreen.kt` | 1 | 1 | 19 | 0 |
| 218 | `app/src/main/java/com/controlprestamos/app/ReportsScreen.kt` | 2 | 2 | 5 | 5 |
| 188 | `app/src/main/java/com/controlprestamos/app/ProfileScreen.kt` | 2 | 2 | 3 | 6 |
| 176 | `app/src/main/java/com/controlprestamos/app/SecuritySettingsScreen.kt` | 1 | 1 | 18 | 5 |
| 109 | `app/src/main/java/com/controlprestamos/app/PrivacyPolicyScreen.kt` | 1 | 1 | 0 | 5 |

## Store / Data Files

| Lines | File | Functions | Classes | Objects |
|---:|---|---:|---:|---:|
| 421 | `app/src/main/java/com/controlprestamos/app/SessionBackupTrashStore.kt` | 14 | 1 | 0 |
| 354 | `app/src/main/java/com/controlprestamos/app/SessionLoanStore.kt` | 22 | 1 | 0 |
| 314 | `app/src/main/java/com/controlprestamos/app/SessionStore.kt` | 68 | 9 | 0 |
| 260 | `app/src/main/java/com/controlprestamos/app/SessionCatalogStore.kt` | 16 | 1 | 0 |
| 183 | `app/src/main/java/com/controlprestamos/app/SessionProfileHistoryStore.kt` | 11 | 1 | 0 |
| 124 | `app/src/main/java/com/controlprestamos/app/SessionSecurityStore.kt` | 14 | 1 | 0 |

## Files Coupled to Navigation

| Nav Mentions | File | Category |
|---:|---|---|
| 52 | `app/src/main/java/com/controlprestamos/app/NavGraph.kt` | navigation |
| 19 | `app/src/main/java/com/controlprestamos/app/MoreScreen.kt` | ui-screen |
| 13 | `app/src/main/java/com/controlprestamos/app/CollectionAgendaScreen.kt` | ui-screen |
| 11 | `app/src/main/java/com/controlprestamos/app/MonthlyViewScreen.kt` | ui-screen |
| 11 | `app/src/main/java/com/controlprestamos/app/WeeklyViewScreen.kt` | ui-screen |
| 10 | `app/src/main/java/com/controlprestamos/app/LoanDetailScreen.kt` | ui-screen |
| 10 | `app/src/main/java/com/controlprestamos/app/AppDesign.kt` | ui-core |
| 8 | `app/src/main/java/com/controlprestamos/app/LoanCollectionNoticeScreen.kt` | ui-screen |
| 8 | `app/src/main/java/com/controlprestamos/app/EditLoanScreen.kt` | ui-screen |
| 7 | `app/src/main/java/com/controlprestamos/app/GlobalSearchScreen.kt` | ui-screen |
| 7 | `app/src/main/java/com/controlprestamos/app/DailySummaryScreen.kt` | ui-screen |
| 7 | `app/src/main/java/com/controlprestamos/app/NewLoanScreen.kt` | ui-screen |
| 6 | `app/src/main/java/com/controlprestamos/app/ProfileScreen.kt` | ui-screen |
| 6 | `app/src/main/java/com/controlprestamos/app/LoansScreen.kt` | ui-screen |
| 6 | `app/src/main/java/com/controlprestamos/app/EditProfileScreen.kt` | ui-screen |
| 5 | `app/src/main/java/com/controlprestamos/app/SecuritySettingsScreen.kt` | ui-screen |
| 5 | `app/src/main/java/com/controlprestamos/app/TrashScreen.kt` | ui-screen |
| 5 | `app/src/main/java/com/controlprestamos/MainActivity.kt` | app-entry |
| 5 | `app/src/main/java/com/controlprestamos/app/BackupExportScreen.kt` | ui-screen |
| 5 | `app/src/main/java/com/controlprestamos/app/RestoreBackupScreen.kt` | ui-screen |
| 5 | `app/src/main/java/com/controlprestamos/app/PrivacyPolicyScreen.kt` | ui-screen |
| 5 | `app/src/main/java/com/controlprestamos/app/FrequentUsersScreen.kt` | ui-screen |
| 5 | `app/src/main/java/com/controlprestamos/app/ReportsScreen.kt` | ui-screen |
| 5 | `app/src/main/java/com/controlprestamos/app/ReminderSettingsScreen.kt` | ui-screen |

## Files Heavily Coupled to SessionStore

| SessionStore Mentions | File | Category | Feature Guess |
|---:|---|---|---|
| 57 | `app/src/main/java/com/controlprestamos/app/NavGraph.kt` | navigation | navigation |
| 20 | `app/src/main/java/com/controlprestamos/app/BackupExportScreen.kt` | ui-screen | backup-trash |
| 19 | `app/src/main/java/com/controlprestamos/app/LockScreen.kt` | ui-screen | security-reminders |
| 18 | `app/src/main/java/com/controlprestamos/app/SecuritySettingsScreen.kt` | ui-screen | security-reminders |
| 12 | `app/src/main/java/com/controlprestamos/app/LoanDetailScreen.kt` | ui-screen | loans |
| 11 | `app/src/main/java/com/controlprestamos/app/CollectionAgendaScreen.kt` | ui-screen | loans |
| 10 | `app/src/main/java/com/controlprestamos/app/WeeklyViewScreen.kt` | ui-screen | loans |
| 10 | `app/src/main/java/com/controlprestamos/app/MonthlyViewScreen.kt` | ui-screen | loans |
| 9 | `app/src/main/java/com/controlprestamos/MainActivity.kt` | app-entry | app |
| 9 | `app/src/main/java/com/controlprestamos/app/LoansScreen.kt` | ui-screen | loans |
| 8 | `app/src/main/java/com/controlprestamos/app/HistoryScreen.kt` | ui-screen | loans |
| 8 | `app/src/main/java/com/controlprestamos/app/GlobalSearchScreen.kt` | ui-screen | search |
| 7 | `app/src/main/java/com/controlprestamos/app/TrashScreen.kt` | ui-screen | backup-trash |
| 6 | `app/src/main/java/com/controlprestamos/app/FrequentUsersScreen.kt` | ui-screen | clients-profile |
| 6 | `app/src/main/java/com/controlprestamos/app/BlacklistScreen.kt` | ui-screen | clients-profile |
| 6 | `app/src/main/java/com/controlprestamos/app/DailySummaryScreen.kt` | ui-screen | loans |
| 6 | `app/src/main/java/com/controlprestamos/app/EditLoanScreen.kt` | ui-screen | loans |
| 6 | `app/src/main/java/com/controlprestamos/app/ReferralsScreen.kt` | ui-screen | clients-profile |

## TODO / FIXME / HACK Markers

| Markers | File |
|---:|---|
| 6 | `app/src/main/java/com/controlprestamos/app/ReferralsScreen.kt` |
| 5 | `app/src/main/java/com/controlprestamos/app/LoansScreen.kt` |
| 1 | `app/src/main/java/com/controlprestamos/app/HistoryScreen.kt` |

## Recommended Target Architecture

Suggested long-term structure:

```text
com.controlprestamos
|-- LoanApp.kt
|-- MainActivity.kt
|-- core
|   |-- design
|   |-- formatting
|   |-- navigation
|   |-- permissions
|-- data
|   |-- local
|   |-- repository
|   |-- backup
|-- domain
|   |-- model
|   |-- usecase
|-- features
    |-- dashboard
    |-- loans
    |-- clients
    |-- reports
    |-- backup
    |-- security
    |-- reminders
    |-- search
```

## Refactor Plan

### Phase 1 - Safe Packaging

- Move navigation files to core/navigation.
- Move design/theme helpers to core/design.
- Move each screen file into a feature package without changing behavior.
- Update imports and compile after every small group.

### Phase 2 - Split Large Screens

- Break large screen files into smaller composables.
- Extract UI sections, cards, dialogs, form states, and formatters.
- Keep screen entry functions thin.

### Phase 3 - Data Boundary

- Reduce direct SessionStore usage inside screens.
- Introduce feature-level state holders or ViewModels.
- Keep data access behind repository-like classes.

### Phase 4 - Domain Layer

- Move calculations, validation, and business rules out of UI.
- Add use cases for loan calculations, collection summaries, reports, and reminders.

## First Safe Refactor Candidate

Recommended first move:

1. Move AppRoutes.kt and NavGraph.kt to com.controlprestamos.core.navigation.
2. Move AppDesign.kt and AppFields.kt to com.controlprestamos.core.design.
3. Compile.
4. Commit.

This is low risk because it changes package organization, not behavior.

## Full File Inventory

| Lines | File | Package | Category | Feature Guess | Composables | Functions | Classes | Objects |
|---:|---|---|---|---|---:|---:|---:|---:|
| 461 | `app/src/main/java/com/controlprestamos/app/AppDesign.kt` | `com.controlprestamos.app` | ui-core | core-ui | 16 | 15 | 0 | 2 |
| 416 | `app/src/main/java/com/controlprestamos/app/AppFields.kt` | `com.controlprestamos.app` | ui-core | core-ui | 1 | 28 | 0 | 0 |
| 102 | `app/src/main/java/com/controlprestamos/app/AppRoutes.kt` | `com.controlprestamos.app` | navigation | navigation | 0 | 0 | 0 | 1 |
| 473 | `app/src/main/java/com/controlprestamos/app/BackupExportScreen.kt` | `com.controlprestamos.app` | ui-screen | backup-trash | 2 | 15 | 0 | 0 |
| 312 | `app/src/main/java/com/controlprestamos/app/BlacklistScreen.kt` | `com.controlprestamos.app` | ui-screen | clients-profile | 1 | 5 | 0 | 0 |
| 271 | `app/src/main/java/com/controlprestamos/app/CollectionAgendaScreen.kt` | `com.controlprestamos.app` | ui-screen | loans | 3 | 3 | 0 | 0 |
| 273 | `app/src/main/java/com/controlprestamos/app/DailySummaryScreen.kt` | `com.controlprestamos.app` | ui-screen | loans | 4 | 7 | 0 | 0 |
| 338 | `app/src/main/java/com/controlprestamos/app/DashboardScreen.kt` | `com.controlprestamos.app` | ui-screen | analytics-dashboard | 2 | 9 | 0 | 0 |
| 363 | `app/src/main/java/com/controlprestamos/app/EditLoanScreen.kt` | `com.controlprestamos.app` | ui-screen | loans | 1 | 4 | 0 | 0 |
| 227 | `app/src/main/java/com/controlprestamos/app/EditProfileScreen.kt` | `com.controlprestamos.app` | ui-screen | clients-profile | 1 | 1 | 0 | 0 |
| 398 | `app/src/main/java/com/controlprestamos/app/FrequentUsersScreen.kt` | `com.controlprestamos.app` | ui-screen | clients-profile | 1 | 6 | 0 | 0 |
| 280 | `app/src/main/java/com/controlprestamos/app/GlobalSearchScreen.kt` | `com.controlprestamos.app` | ui-screen | search | 1 | 5 | 0 | 0 |
| 255 | `app/src/main/java/com/controlprestamos/app/HistoryScreen.kt` | `com.controlprestamos.app` | ui-screen | loans | 3 | 4 | 0 | 0 |
| 455 | `app/src/main/java/com/controlprestamos/app/LoanCollectionNoticeScreen.kt` | `com.controlprestamos.app` | ui-screen | loans | 3 | 12 | 0 | 0 |
| 467 | `app/src/main/java/com/controlprestamos/app/LoanDetailScreen.kt` | `com.controlprestamos.app` | ui-screen | loans | 4 | 6 | 0 | 0 |
| 17 | `app/src/main/java/com/controlprestamos/app/LoanReminderBootReceiver.kt` | `com.controlprestamos.app` | android-receiver | loans | 0 | 0 | 1 | 0 |
| 287 | `app/src/main/java/com/controlprestamos/app/LoanReminderManager.kt` | `com.controlprestamos.app` | manager | loans | 0 | 18 | 0 | 1 |
| 12 | `app/src/main/java/com/controlprestamos/app/LoanReminderReceiver.kt` | `com.controlprestamos.app` | android-receiver | loans | 0 | 0 | 1 | 0 |
| 574 | `app/src/main/java/com/controlprestamos/app/LoansScreen.kt` | `com.controlprestamos.app` | ui-screen | loans | 6 | 19 | 0 | 0 |
| 224 | `app/src/main/java/com/controlprestamos/app/LockScreen.kt` | `com.controlprestamos.app` | ui-screen | security-reminders | 1 | 1 | 0 | 1 |
| 296 | `app/src/main/java/com/controlprestamos/app/MonthlyViewScreen.kt` | `com.controlprestamos.app` | ui-screen | loans | 3 | 3 | 0 | 0 |
| 283 | `app/src/main/java/com/controlprestamos/app/MoreScreen.kt` | `com.controlprestamos.app` | ui-screen | uncategorized | 6 | 4 | 0 | 0 |
| 268 | `app/src/main/java/com/controlprestamos/app/NavGraph.kt` | `com.controlprestamos.app` | navigation | navigation | 1 | 3 | 0 | 0 |
| 338 | `app/src/main/java/com/controlprestamos/app/NewLoanScreen.kt` | `com.controlprestamos.app` | ui-screen | loans | 1 | 4 | 0 | 0 |
| 109 | `app/src/main/java/com/controlprestamos/app/PrivacyPolicyScreen.kt` | `com.controlprestamos.app` | ui-screen | uncategorized | 1 | 1 | 0 | 0 |
| 188 | `app/src/main/java/com/controlprestamos/app/ProfileScreen.kt` | `com.controlprestamos.app` | ui-screen | clients-profile | 2 | 2 | 0 | 0 |
| 545 | `app/src/main/java/com/controlprestamos/app/ReferralsScreen.kt` | `com.controlprestamos.app` | ui-screen | clients-profile | 4 | 10 | 0 | 0 |
| 261 | `app/src/main/java/com/controlprestamos/app/ReminderSettingsScreen.kt` | `com.controlprestamos.app` | ui-screen | security-reminders | 1 | 2 | 0 | 0 |
| 218 | `app/src/main/java/com/controlprestamos/app/ReportsScreen.kt` | `com.controlprestamos.app` | ui-screen | analytics-dashboard | 2 | 2 | 0 | 0 |
| 420 | `app/src/main/java/com/controlprestamos/app/RestoreBackupScreen.kt` | `com.controlprestamos.app` | ui-screen | backup-trash | 1 | 12 | 0 | 0 |
| 176 | `app/src/main/java/com/controlprestamos/app/SecuritySettingsScreen.kt` | `com.controlprestamos.app` | ui-screen | security-reminders | 1 | 1 | 0 | 0 |
| 421 | `app/src/main/java/com/controlprestamos/app/SessionBackupTrashStore.kt` | `com.controlprestamos.app` | data-store | backup-trash | 0 | 14 | 1 | 0 |
| 260 | `app/src/main/java/com/controlprestamos/app/SessionCatalogStore.kt` | `com.controlprestamos.app` | data-store | uncategorized | 0 | 16 | 1 | 0 |
| 354 | `app/src/main/java/com/controlprestamos/app/SessionLoanStore.kt` | `com.controlprestamos.app` | data-store | loans | 0 | 22 | 1 | 0 |
| 183 | `app/src/main/java/com/controlprestamos/app/SessionProfileHistoryStore.kt` | `com.controlprestamos.app` | data-store | loans | 0 | 11 | 1 | 0 |
| 124 | `app/src/main/java/com/controlprestamos/app/SessionSecurityStore.kt` | `com.controlprestamos.app` | data-store | security-reminders | 0 | 14 | 1 | 0 |
| 314 | `app/src/main/java/com/controlprestamos/app/SessionStore.kt` | `com.controlprestamos.app` | data-store | uncategorized | 0 | 68 | 9 | 0 |
| 37 | `app/src/main/java/com/controlprestamos/app/SessionTimeoutManager.kt` | `com.controlprestamos.app` | manager | uncategorized | 0 | 3 | 1 | 0 |
| 271 | `app/src/main/java/com/controlprestamos/app/TrashScreen.kt` | `com.controlprestamos.app` | ui-screen | backup-trash | 3 | 7 | 0 | 0 |
| 307 | `app/src/main/java/com/controlprestamos/app/WeeklyViewScreen.kt` | `com.controlprestamos.app` | ui-screen | loans | 3 | 3 | 0 | 0 |
| 6 | `app/src/main/java/com/controlprestamos/LoanApp.kt` | `com.controlprestamos` | app-entry | loans | 0 | 0 | 1 | 0 |
| 62 | `app/src/main/java/com/controlprestamos/MainActivity.kt` | `com.controlprestamos` | app-entry | app | 0 | 0 | 1 | 0 |
