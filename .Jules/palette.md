## 2023-10-25 - Explicit Semantics for Structural Compose Elements
**Learning:** In Jetpack Compose, applying `.clickable()` to non-button structural elements (like `Box` or `Row`) does not automatically inform screen readers that the element functions as a button, leading to a poor accessibility experience.
**Action:** When making structural elements interactive with `.clickable()`, always provide explicit semantics by setting `onClickLabel` and `role = Role.Button` to ensure proper context for screen reader users.
