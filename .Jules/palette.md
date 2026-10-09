## 2026-10-09 - [A11y: Semantic Roles for Structural Elements]
**Learning:** When using `Modifier.clickable` on non-button structural elements (like `Row` or `Box`) in Jetpack Compose, screen readers do not automatically announce them as actionable buttons. This leads to poor discoverability for visually impaired users.
**Action:** Always include explicit semantic roles (`role = Role.Button`) and descriptive `onClickLabel` attributes within `Modifier.clickable` blocks for structural elements to ensure proper context for screen readers.
