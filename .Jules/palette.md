## 2024-05-18 - Jetpack Compose Clickable Accessibility
**Learning:** When using `Modifier.clickable` in Jetpack Compose, screen readers do not automatically announce the element as a button or explain its action. This is a critical pattern specific to custom UI components in this app.
**Action:** Always include `role = androidx.compose.ui.semantics.Role.Button` and a descriptive `onClickLabel` within `Modifier.clickable` blocks to ensure proper accessibility for screen reader users.
