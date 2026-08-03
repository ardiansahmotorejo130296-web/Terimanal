## 2024-03-25 - Jetpack Compose Accessibility for Modifier.clickable
**Learning:** Using `Modifier.clickable` without a `role` and `onClickLabel` fails to provide context to screen readers, making icon-only buttons or interactive boxes inaccessible.
**Action:** Always provide `role = Role.Button` (or appropriate role) and a descriptive `onClickLabel` inside `Modifier.clickable` blocks when making non-standard UI elements interactive in Compose.
