## 2025-01-20 - [Minimum Touch Target Size for Icons]
**Learning:** Small icons with direct `.clickable()` modifiers violate accessibility guidelines for minimum touch target sizes (48dp). Screen reader users also lack context if roles aren't defined.
**Action:** Always wrap interactive `Icon`s in an `IconButton` instead of using `.clickable()` on the icon directly, and avoid shrinking the `IconButton` size.
