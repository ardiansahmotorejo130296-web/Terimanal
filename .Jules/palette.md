## 2024-08-16 - Jetpack Compose Touch Targets
**Learning:** Applying `.clickable()` directly to `Icon` or `Text` elements creates unreachably small touch targets and lacks proper screen reader support. Small touch targets negatively impact accessibility for users with motor impairments.
**Action:** Always wrap standard `Icon`s in an `IconButton` to ensure a minimum accessible touch target size of 48dp and appropriate `role = Role.Button` semantics for screen readers. Avoid overriding the `IconButton` size.
