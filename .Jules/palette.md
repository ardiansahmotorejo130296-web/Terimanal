## 2024-11-07 - Screen reader roles for clickable structural elements
**Learning:** In Jetpack Compose, applying `.clickable()` to structural elements like `Box` or `Row` (e.g., SuggestionChips or History dropdown rows) doesn't automatically announce them as buttons to screen readers, making navigation confusing for visually impaired users.
**Action:** Always add explicit semantics (`onClickLabel` and `role = Role.Button`) when making structural elements interactive to ensure they are properly identified as actionable controls.
