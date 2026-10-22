## 2023-10-24 - [Semantic Roles for Structural Elements]
**Learning:** When using `Modifier.clickable()` on non-button structural elements like `Box` or `Row` in Jetpack Compose, the semantic context of it being a button or interactive element is lost for screen readers.
**Action:** Always provide explicit semantics via `onClickLabel` and `role = Role.Button` along with `import androidx.compose.ui.semantics.Role` when adding `Modifier.clickable()` to generic layout elements.
