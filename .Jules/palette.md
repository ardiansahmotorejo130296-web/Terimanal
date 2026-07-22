## 2026-07-22 - Jetpack Compose Clickable Semantics
**Learning:** Modifier.clickable() in this app's Jetpack Compose components lacks semantic roles and accessibility labels by default, resulting in poor screen reader support for custom clickable elements (like icon-only buttons or interactive rows).
**Action:** Always provide `role = Role.Button` and a descriptive `onClickLabel` within `Modifier.clickable()` blocks for interactive elements to ensure full accessibility compliance.
