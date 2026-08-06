## 2026-08-06 - Jetpack Compose Accessibility Enhancements
**Learning:** Replaced Icon with IconButton to guarantee 48dp minimum touch target and added semantics roles and labels to custom clickable items.
**Action:** Always wrap interactive Icons in IconButtons instead of appending .clickable modifiers to ensure accessible touch targets and proper screen reader feedback on mobile.
