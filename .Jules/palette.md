## 2026-08-01 - Jetpack Compose Accessibility: Modifiers
**Learning:** When using the `.clickable` modifier in Jetpack Compose, it is critical to provide `onClickLabel` and `role` parameters (e.g., `Role.Button`) to ensure proper screen reader support. This was missed on custom components like suggestion chips and history list items.
**Action:** Always include `onClickLabel` and `role` inside `.clickable` blocks for actionable components to guarantee screen reader accessibility.
