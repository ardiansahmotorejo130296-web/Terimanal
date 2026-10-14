## 2026-10-14 - [Add explicit roles to structurally clickable items]
**Learning:** Found that applying `.clickable` to generic structures like `Box` or `Row` without specifying an accessibility role and onClick label limits usability for screen-reader users, as they are not announced properly.
**Action:** Always provide explicit `role = Role.Button` and a descriptive `onClickLabel` whenever using `Modifier.clickable` on non-button layout components.
