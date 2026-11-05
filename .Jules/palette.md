## 2026-11-05 - Added semantics to non-button interactive elements
**Learning:** Using Modifier.clickable() on structural elements like Box or Row without specifying explicit semantic properties (like role = Role.Button and onClickLabel) leaves screen readers without proper context, causing poor accessibility.
**Action:** Always provide explicit semantics via onClickLabel and role = Role.Button when making structural elements interactive in Jetpack Compose.
