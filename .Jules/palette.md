## 2024-08-17 - Avoid clickable on Icons directly for accessibility

**Learning:** When using Jetpack Compose, applying a `Modifier.clickable()` directly on an `Icon` component often results in a touch target that is smaller than the recommended minimum of 48dp, reducing accessibility.

**Action:** Wrap the `Icon` in an `IconButton` component instead. `IconButton` is designed to automatically ensure a minimum touch target size of 48dp, complying with accessibility guidelines. Furthermore, when using `Modifier.clickable()` on generic containers (like `Box` or `Row`), make sure to provide semantics directly using `role = Role.Button` and `onClickLabel` for screen readers.