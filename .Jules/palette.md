## 2026-09-28 - [Maintain Minimum Accessible Touch Target Size]
**Learning:** Applying `Modifier.clickable()` directly to a small `Icon` (e.g. 16dp) in Jetpack Compose violates accessibility guidelines by creating a touch target that is too small for many users to reliably tap.
**Action:** Always wrap standard `Icon`s in an `IconButton` to ensure a minimum accessible touch target size of 48dp. When refactoring, ensure any existing `testTag` modifiers are moved to the outer interactive element to preserve UI test compatibility.
