## 2024-06-25 - Improve Touch Target Size for Icons
**Learning:** In Jetpack Compose, applying `.clickable()` directly to a small `Icon` (e.g., 16dp or 24dp) violates touch target size accessibility requirements. The touch target must be at least 48dp.
**Action:** When adding interactivity to `Icon`s, always wrap them in an `IconButton` (which inherently provides a 48dp minimum touch target). Make sure to move any `testTag` and `onClick` logic from the `Icon` to the outer `IconButton` to ensure UI tests can still interact with the component properly.
