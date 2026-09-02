## 2024-05-24 - [Accessible Touch Targets in Jetpack Compose]
**Learning:** In Jetpack Compose, applying `.clickable()` directly to small `Icon` components can violate the minimum accessible touch target size (typically 48dp).
**Action:** Always wrap interactive icons in an `IconButton` (which enforces a minimum 48dp size) rather than applying `.clickable()` directly to the `Icon` itself, and avoid shrinking the `IconButton` with size modifiers that defeat its accessibility purpose.
