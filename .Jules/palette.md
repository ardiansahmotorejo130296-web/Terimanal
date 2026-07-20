## 2024-05-24 - Accessible clickables in Jetpack Compose
**Learning:** Elements built using non-interactive primitives (like `Icon`, `Box`, `Row`, etc.) that are made clickable using `Modifier.clickable` are not automatically announced properly by screen readers (like TalkBack) without explicitly providing semantics.
**Action:** Always provide the `role = androidx.compose.ui.semantics.Role.Button` and a descriptive `onClickLabel` inside the `Modifier.clickable` block for custom components acting as buttons to ensure accessibility.
