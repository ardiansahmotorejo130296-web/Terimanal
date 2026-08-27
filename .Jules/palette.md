## 2024-05-15 - [Accessible Touch Targets in Compose]
**Learning:** Applying `.clickable()` directly to a small `Icon` (e.g., 16dp) fails accessibility standards by creating a too-small touch target.
**Action:** Always wrap standard `Icon`s in an `IconButton` to ensure a minimum accessible touch target size of 48dp.
