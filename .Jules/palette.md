## 2024-05-24 - [TerminalInputComponent A11y Update]
**Learning:** In Jetpack Compose, directly applying `Modifier.clickable` to a small `Icon` results in a touch target that is too small (e.g., 16dp), violating accessibility guidelines which recommend a minimum touch target size of 48dp.
**Action:** Always wrap standard interactive `Icon`s in an `IconButton` (or use `Modifier.minimumInteractiveComponentSize()`) to ensure proper touch target dimensions.
