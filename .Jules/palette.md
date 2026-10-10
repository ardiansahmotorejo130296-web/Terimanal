## 2023-10-24 - Accessibility improvements for Compose interactives
**Learning:** Raw elements like `Icon`, `Row`, `Box` using `Modifier.clickable()` need semantic roles (`role = Role.Button`) and descriptions (`onClickLabel`) for screen readers. Small icons with `Modifier.clickable()` fail touch target guidelines (minimum 48dp).
**Action:** Always wrap interactive `Icon` elements in `IconButton`, or use explicit `role` and `onClickLabel` with `Modifier.clickable()` on generic containers, ensuring sufficient touch targets.
