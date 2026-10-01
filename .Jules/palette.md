## 2024-05-24 - [Accessible Clickables in Jetpack Compose]
**Learning:** Adding `.clickable` to generic structures like `Box` or `Row` strips semantics. Replacing simple icons with `IconButton` ensures accessible minimum touch targets.
**Action:** Always include `role = Role.Button` and `onClickLabel` in `.clickable`, and use `IconButton` instead of `.clickable` on `Icon`s where touch target sizes matter.
