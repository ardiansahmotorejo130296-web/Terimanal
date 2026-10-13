## 2024-07-25 - Missing Semantic Roles on Custom Clickable Containers
**Learning:** Several custom interactive elements in the Terminaldroid app (like SuggestionChip's Box and the history list's Row) use Modifier.clickable() but lack explicit semantic roles, making them opaque to screen readers which won't announce them as interactive buttons.
**Action:** When adding .clickable() to non-button structural elements (Box, Row, Text), always explicitly provide role = Role.Button and a descriptive onClickLabel to ensure proper accessibility context.
