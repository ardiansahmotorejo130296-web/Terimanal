## 2024-09-04 - Jetpack Compose Touch Targets and Semantics
**Learning:** Raw `Icon` elements wrapped with `.clickable` and fixed small sizes (e.g., `16.dp`) fail to meet minimum touch target sizes (48dp) and lack semantic button roles, making them frustrating for users and inaccessible to screen readers.
**Action:** Always wrap interactive icons in an `IconButton` (which guarantees size) and provide explicit `role = Role.Button` and `onClickLabel` properties on custom `Modifier.clickable` components like chips or list items.
