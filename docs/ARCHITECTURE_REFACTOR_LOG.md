# Architecture Refactor Log

Generated: 2026-04-19 16:31:12

## Refactor completed

Moved architecture base files into dedicated core packages.

## New packages

- com.controlprestamos.core.navigation
- com.controlprestamos.core.design

## Moved files

- AppRoutes.kt -> core/navigation
- NavGraph.kt -> core/navigation
- AppDesign.kt -> core/design
- AppFields.kt -> core/design

## Validation

- gradlew clean --no-daemon
- gradlew assembleDebug --no-daemon

## Next recommended refactor

- Move screens into feature packages gradually.
- Start with features/loans.
- Split large screens after package movement.
