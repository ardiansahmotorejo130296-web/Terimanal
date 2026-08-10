## 2024-08-10 - Touch Targets on Small Icons
**Learning:** Found small 16dp `Icon` elements using `.clickable()` for interactive actions, which fails Android accessibility minimum touch target size (48dp). Users with limited dexterity or using touch screens would have difficulty interacting with these elements.
**Action:** Replace `Icon(modifier = Modifier.clickable { ... })` with `IconButton(onClick = { ... }) { Icon(...) }` for standalone icon actions to automatically enforce accessible touch target sizes.
