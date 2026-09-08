## 2024-05-24 - Accessible Touch Targets for Icons
**Learning:** Applying `.clickable()` directly to a small `Icon` (like a 16dp close button) creates an inaccessible touch target that frustrates users with motor impairments.
**Action:** Always wrap interactive icons in an `IconButton`, which natively ensures a minimum 48dp accessible touch target size according to Material guidelines.
