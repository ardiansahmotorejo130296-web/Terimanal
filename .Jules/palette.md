## 2024-06-25 - Accessibility Semantics on Generic Compose Elements
**Learning:** When using `Modifier.clickable()` on generic non-button Compose elements (like `Box`, `Row`, `Text`, or standard `Icon`), these elements lack intrinsic accessibility semantics. Screen readers will just announce them as generic click targets without context.
**Action:** Always provide explicit semantics via `onClickLabel` and `role = Role.Button` within the `clickable` modifier block to ensure proper context and role announcement for screen readers and visually impaired users.
