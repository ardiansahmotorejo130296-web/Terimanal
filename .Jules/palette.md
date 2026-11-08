
## 2026-11-08 - [Touch Targets]
**Learning:** In Jetpack Compose, directly applying `Modifier.clickable` to a 16x16dp `Icon` component without wrapping it in an `IconButton` creates a difficult-to-tap interactive element that falls significantly short of the recommended minimum 48x48dp touch target size.
**Action:** Always wrap interactive `Icon`s in an `IconButton` instead of applying `.clickable()` directly, ensuring the `testTag` modifiers are moved to the outer `IconButton` structure so UI tests continue to function. Watch out for unintended parent layout scaling when bumping the interactive size.
