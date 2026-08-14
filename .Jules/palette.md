## 2024-08-14 - Replace `.clickable` on Icon with `IconButton` for proper a11y touch targets
**Learning:** In Jetpack Compose, applying `.clickable` directly to a small `Icon` (like `16.dp`) results in a touch target that is too small and violates accessibility standards (minimum 48dp).
**Action:** Always wrap small interactive `Icon`s in an `IconButton` (which provides the standard 48dp touch target and proper button semantics) instead of applying `.clickable` directly to the `Icon`. Ensure not to override the `IconButton`'s size with small constraints that defeat its purpose.
