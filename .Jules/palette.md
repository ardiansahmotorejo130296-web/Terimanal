
## 2024-05-18 - Accessibility: Semantics for Structural Elements and Touch Target Sizes in Compose
**Learning:** Applying `Modifier.clickable()` to non-button structural elements (`Box`, `Row`) requires explicit semantics via `onClickLabel` and `role = Role.Button` to provide proper context for screen readers. Using `Icon` with `clickable` instead of `IconButton` causes touch targets to fall below the 48dp recommended accessibility minimum.
**Action:** Always verify `Modifier.clickable()` usage in Compose and ensure proper roles, labels, and touch target sizes are respected by substituting raw icons with `IconButton` when interactive. Ensure `import androidx.compose.ui.semantics.Role` is included.
