## 2024-10-27 - TerminalInput Accessibility
**Learning:** Raw `.clickable()` modifiers applied directly to small icons fail minimum touch target guidelines (48dp). Custom list items and chips also need semantic roles and action labels to properly support screen readers in Jetpack Compose.
**Action:** Always wrap standard `Icon`s in an `IconButton` rather than using `.clickable()`. When building custom interactive components like chips or history items with `.clickable()`, explicitly include `role = androidx.compose.ui.semantics.Role.Button` and a descriptive `onClickLabel`.
