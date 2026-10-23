## 2023-10-23 - Add explicit semantics to structural clickable elements
**Learning:** When using `Modifier.clickable` on non-button structural elements like `Box` or `Row` in Jetpack Compose, they lack inherent context for screen readers.
**Action:** Always provide explicit semantics via `onClickLabel` and `role = Role.Button` inside `Modifier.clickable()` to ensure proper accessibility and context for visually impaired users.
