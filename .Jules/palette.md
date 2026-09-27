## 2024-05-19 - Semantic Roles for Custom Composables
**Learning:** In Jetpack Compose, using `Modifier.clickable` on raw elements like `Box` or `Row` creates an interactive element but doesn't inherently communicate its purpose to screen readers.
**Action:** Always provide explicit semantics via `onClickLabel` and `role = Role.Button` within `Modifier.clickable(...)` blocks to ensure context is provided to screen readers and visually impaired users.
