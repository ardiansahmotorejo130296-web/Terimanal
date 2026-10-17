## 2026-10-17 - Semantics in Modifier.clickable
**Learning:** In Jetpack Compose, using `Modifier.clickable` on raw structural elements like `Box` or `Icon` does not inherently communicate interaction intent to screen readers like TalkBack, making them silent buttons.
**Action:** Always provide explicit semantics when applying `Modifier.clickable()` to non-button elements by supplying the `onClickLabel` string parameter and adding `role = Role.Button` to properly surface context to screen readers.
