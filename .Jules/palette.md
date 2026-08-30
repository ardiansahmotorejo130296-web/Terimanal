## 2024-05-14 - Touch Target Accessibility
**Learning:** Applying `.clickable()` directly to small `Icon`s (e.g. `size(16.dp)`) results in touch targets that are too small and inaccessible.
**Action:** Wrap interactive `Icon` elements in an `IconButton` to ensure a minimum accessible touch target size of 48dp, avoiding direct `.size()` constraints that undermine this on the button wrapper itself.
