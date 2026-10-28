## 2024-03-01 - Adding Semantic Roles to Clickable Modifiers
**Learning:** When adding `Modifier.clickable()` to non-button structural elements like `Box` or `Text` in Jetpack Compose, the screen reader lacks the proper context.
**Action:** Always provide explicit semantics via `onClickLabel` and `role = Role.Button` to ensure proper context for screen readers and visually impaired users.
