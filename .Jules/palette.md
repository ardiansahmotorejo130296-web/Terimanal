## 2024-05-24 - [TerminalInputComponent History Stack Accessibility]
**Learning:** Found an `Icon` component wrapped with `Modifier.clickable()` for the history dropdown close button without an accessible touch target size or `contentDescription` / `Role.Button` bindings, which reduces accessibility for screen readers and touch users.
**Action:** Replace `Modifier.clickable()` on small icons with `IconButton` wrapping the `Icon` and proper accessibility traits in Compose.
