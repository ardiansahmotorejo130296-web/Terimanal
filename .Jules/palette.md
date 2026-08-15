## 2024-08-15 - [Accessible Touch Targets for Icons]
**Learning:** [Wrapping standard `Icon`s with a `.clickable()` modifier creates an inaccessible touch target area which can be difficult for some users to use. Jetpack Compose `IconButton` provides a standard 48dp minimum accessible touch target.]
**Action:** [Use `IconButton` with a child `Icon` for clickable icons instead of placing the `.clickable()` modifier directly on the `Icon`.]
