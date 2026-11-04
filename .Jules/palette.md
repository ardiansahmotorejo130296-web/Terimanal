## 2024-11-04 - Screen Reader support for Compose clickable modifiers
**Learning:** When using `Modifier.clickable()` on non-standard UI elements in Jetpack Compose, the screen reader does not automatically announce the element as a button or provide context for the interaction, severely degrading accessibility.
**Action:** Always provide explicit semantics via `onClickLabel` and `role = Role.Button` to `Modifier.clickable()` blocks to ensure proper context for screen readers and visually impaired users.
