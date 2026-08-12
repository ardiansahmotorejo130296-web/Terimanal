## 2024-05-18 - [Accessible Touch Targets for Action Icons]
**Learning:** Found multiple instances where `Icon`s used direct `.clickable()` modifiers with small `.size()` definitions, resulting in touch targets far below the accessible minimum (e.g. 16dp). Wrapping them in `IconButton` automatically provides a 48dp minimum touch target and standard focus states.
**Action:** Always wrap actionable `Icon` components in `IconButton` and avoid constraining `IconButton` size below 48dp.
