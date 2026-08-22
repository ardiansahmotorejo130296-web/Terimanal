## 2024-05-24 - Accessibility Improvement for Small Icons
**Learning:** Using `.clickable()` directly on a small `Icon` violates accessibility guidelines as it results in a touch target smaller than the recommended minimum of 48dp.
**Action:** Always wrap standard `Icon`s in an `IconButton` to ensure a sufficiently large touch target area for interactive elements.
