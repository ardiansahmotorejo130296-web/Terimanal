## 2024-05-15 - [Accessible Touch Targets for Icons]
**Learning:** In Jetpack Compose, wrapping a standard `Icon` with an `IconButton` is crucial to ensure a minimum accessible touch target size of 48dp, which is the standard accessibility guideline. Applying `.clickable()` directly to a small icon size (e.g., `.size(16.dp)`) defeats accessibility best practices.
**Action:** When adding interactivity to small icons, always use `IconButton` instead of `.clickable()` directly.
