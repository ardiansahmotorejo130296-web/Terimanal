## 2024-05-24 - [Interactive Structural Elements]
**Learning:** When making non-interactive structural elements like `Box` or `Row` interactive using the `.clickable()` modifier in Jetpack Compose, screen readers may not announce them properly, missing vital context.
**Action:** Always provide an explicit semantic role (`role = Role.Button`) and an `onClickLabel` within the `.clickable` modifier to ensure proper accessibility and contextual announcements for screen readers.
