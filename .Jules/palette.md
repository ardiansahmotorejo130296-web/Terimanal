## 2024-10-21 - Add semantics for generic custom clickables
**Learning:** Adding `.clickable()` to structural elements like `Box` or `Row` does not automatically provide button semantics to screen readers.
**Action:** Always include `onClickLabel` and `role = Role.Button` when making generic layouts interactive.
