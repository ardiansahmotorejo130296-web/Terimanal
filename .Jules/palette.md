## 2024-08-23 - [Accessible Touch Targets in Compose]
**Learning:** In Jetpack Compose, applying `.clickable()` directly to small icons (like a 16dp close icon) creates touch targets that are too small and fail accessibility guidelines. Overriding `IconButton` sizes with modifiers (e.g., `.size(16.dp)`) defeats their built-in minimum touch target size (48dp).
**Action:** Always wrap standard `Icon`s in an `IconButton` for interactive elements and avoid restricting the overall component size to ensure a minimum accessible touch target size of 48dp.
