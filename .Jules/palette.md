
## 2024-05-19 - Accessible Touch Targets in Jetpack Compose
**Learning:** Applying `.clickable()` directly to an `Icon` component with a fixed size (e.g., `.size(16.dp)`) creates an inaccessible touch target that fails standard accessibility checks (which require at least 48dp).
**Action:** Always wrap interactive `Icon` elements in an `IconButton` (which inherently manages the 48dp minimum touch target requirement) rather than applying `.clickable()` directly to the `Icon` and overriding its size.
