# Features Profile Refactor Log

Generated: 2026-04-19 17:30:44

## Refactor completed

Moved profile-related screens into a dedicated feature package.

## New package

- com.controlprestamos.features.profile

## Moved files

- ProfileScreen.kt -> features/profile
- EditProfileScreen.kt -> features/profile

## Current architecture

- core.navigation contains app routes and navigation graph.
- core.design contains UI design components.
- core.format contains formatting helpers.
- core.validation contains input sanitizing and validation helpers.
- features.loans contains loan screens.
- features.profile now contains profile screens.

## Validation

- gradlew clean --no-daemon
- gradlew assembleDebug --no-daemon

## Notes

- Profile screens still import com.controlprestamos.app.* temporarily because stores remain in app.
- Next recommended refactor: move backup/trash screens or reduce wildcard imports gradually.
