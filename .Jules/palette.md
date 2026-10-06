## 2024-05-24 - Accessibility and Touch Target Initialization
**Learning:** `Icon` components with a `clickable` modifier and a small size (e.g. `16.dp`) have touch targets that are too small and violate Android accessibility standard for minimum touch targets (48dp).
**Action:** When making small interactive icons, always wrap them in an `IconButton` and specify the outer button's size to be at least `48.dp`, keeping the inner icon at its intended visual size. Transfer any `testTag` to the outer `IconButton`.
