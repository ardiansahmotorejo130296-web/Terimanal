## 2024-05-24 - Jetpack Compose Clickable Accessibility
**Learning:** In Jetpack Compose, `Modifier.clickable` elements often lack context for screen readers. By default, they might just read as "clickable" without explaining what action they perform.
**Action:** Always add `role = Role.Button` and a descriptive `onClickLabel` to `Modifier.clickable` when the element acts as a button, ensuring screen reader users understand the action before activating it.
