## 2024-11-20 - Accessible Clickable Targets in Compose
**Learning:** In Jetpack Compose, applying `.clickable()` directly to an `Icon` or smaller elements can violate minimum touch target (48dp) requirements and fail to convey standard interactive semantics to screen readers.
**Action:** Always wrap standard `Icon` components in an `IconButton` to ensure a 48dp minimum touch target. When applying `.clickable()` to non-button structural elements like `Box` or `Row`, explicitly set `role = Role.Button` and provide an `onClickLabel` to give screen readers proper context.
