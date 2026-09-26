## 2026-09-26 - [Accessible Touch Targets for Jetpack Compose Icons]
**Learning:** In Jetpack Compose, applying `.clickable()` directly to a small `Icon` (e.g., 16dp) fails to provide the minimum accessible touch target size of 48dp, which negatively impacts screen reader users and those with motor impairments.
**Action:** When adding interactivity to icons, always wrap them in an `IconButton` (or use a parent element with a minimum size of 48dp) to ensure accessibility guidelines are met, and remember to move any existing `testTag` modifiers to the new interactive outer element.
