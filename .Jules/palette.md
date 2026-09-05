## 2024-05-24 - Accessible Touch Targets for Icons
**Learning:** Applying `.clickable()` directly to small `Icon` or text components creates a touch target that is too small, violating accessibility guidelines which recommend a minimum of 48dp.
**Action:** Always wrap interactive icons or close texts in an `IconButton` to ensure the minimum 48dp touch target size is met natively without explicitly overriding its size to be smaller, or use `minimumInteractiveComponentSize()`.
