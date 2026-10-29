## 2026-10-29 - [Added roles to clickable Compose elements]
**Learning:** When using `Modifier.clickable` on structural elements (like `Row` or `Box`), provide a role (like `Role.Button`) and `onClickLabel` for accurate screen reader announcements.
**Action:** Always include `role = Role.Button` and `onClickLabel` in `Modifier.clickable()` when making non-button elements interactive.
## 2026-10-29 - [Min touch targets for Icons]
**Learning:** Wrapping a plain `Icon` with `Modifier.clickable` and small size restricts touch area and creates an accessibility issue for touch targets.
**Action:** Use an `IconButton` around the `Icon` instead to automatically ensure a minimum 48dp touch target size.
