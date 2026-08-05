## 2024-05-24 - Compose Clickable Accessibility
**Learning:** When using `Modifier.clickable()` on custom compose elements, screen readers may not announce them correctly as interactive elements or describe their action. Also, standard icons have small default bounds, often failing touch target size requirements.
**Action:** Always provide `role = Role.Button` and contextual `onClickLabel` inside `Modifier.clickable()` for custom interactive elements to ensure proper screen reader support. Use `IconButton` to meet 48dp touch targets.
