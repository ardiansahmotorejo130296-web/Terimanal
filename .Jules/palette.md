## 2024-05-15 - TerminalDroid Accessibility Learnings
**Learning:** In TerminalDroid (Jetpack Compose), Icon elements need contentDescription, but when wrapped in a Modifier.clickable(), they also need role = Role.Button and onClickLabel to ensure proper screen reader support for interactive icons.
**Action:** Always add role = Role.Button and onClickLabel to Modifier.clickable() when making an Icon interactive.
