## 2024-05-24 - [Accessible Clickables in Jetpack Compose]
**Learning:** Raw elements like Box or Row using `Modifier.clickable()` without providing `onClickLabel` and `role = Role.Button` lack screen reader context, making them difficult to interact with for visually impaired users.
**Action:** Always provide explicit semantics via `onClickLabel` and `role` when adding `.clickable()` to non-button structural elements.
