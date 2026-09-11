## 2024-05-24 - Interactive Icon Accessibility
**Learning:** When adding interaction to `Icon` components in Jetpack Compose, applying `.clickable()` directly often results in a touch target that is too small, violating the 48dp minimum accessible touch target rule.
**Action:** Always wrap standard `Icon` components in an `IconButton` to provide a properly sized minimum touch target (48dp x 48dp) for accessibility, and move modifiers like `testTag` to the wrapper.
