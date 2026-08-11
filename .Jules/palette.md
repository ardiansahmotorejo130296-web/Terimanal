## 2024-06-25 - Interactive Icons Touch Target Accessiblity in Jetpack Compose
**Learning:** Found multiple instances where `.clickable()` was applied directly to a small `Icon` or `Text` acting as a button, which misses semantic button roles (like `Role.Button`) and fails the accessibility touch target size guidelines (48dp).
**Action:** When implementing small interactive icons or text close buttons, always wrap them in a standard `IconButton` to ensure a minimum accessible touch target size and proper semantics for screen readers.
