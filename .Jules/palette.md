## 2024-05-18 - Ensure touch target size of 48dp on delete alias
**Learning:** Hardcoded sizing of `16.dp` on `.clickable()` modifiers applied directly to raw `Icon` elements within the Jetpack Compose app severely compromise accessibility, rendering standard touch targets too small for proper use.
**Action:** When auditing list items or interactive badges, swap `.clickable()` applied directly on `Icon` elements with standard `IconButton` wrappers to enforce a minimum touch target size of 48dp.
