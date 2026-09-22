## 2023-10-27 - [Compose Interactive Elements Accessibility]
**Learning:** Applying `Modifier.clickable()` to structural elements (`Row`, `Box`) or icons without a role or `onClickLabel` fails to provide necessary context to screen readers, and direct application to icons can lead to undersized (<48dp) touch targets.
**Action:** Always provide explicit semantics via `role = Role.Button` and `onClickLabel` for `Modifier.clickable()`, and wrap icons in `IconButton` rather than using `Modifier.clickable()` directly to ensure a minimum 48dp accessible touch target.
