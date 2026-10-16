## 2024-11-20 - Adding Accessible Roles to Structural Components
**Learning:** In Jetpack Compose, applying `.clickable()` to non-button structural elements like `Box` or `Row` does not automatically confer interactive accessibility traits. Screen readers may misinterpret them, creating a barrier.
**Action:** Always provide explicit semantics via `onClickLabel = "..."` and `role = Role.Button` when adding `.clickable()` to non-button elements to ensure proper context for screen readers.
