package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TerminalTheme

@Composable
fun TerminalThemeSwitcherComponent(
    currentTheme: TerminalTheme,
    onThemeSelected: (TerminalTheme) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = currentTheme.background.copy(alpha = 0.95f)
        ),
        border = BorderStroke(1.dp, currentTheme.promptColor.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("theme_switcher_card")
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = currentTheme.promptColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "TERMINAL THEME SELECTOR",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = currentTheme.foreground
                    )
                }

                Text(
                    text = "[X]",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = currentTheme.errorColor,
                    modifier = Modifier
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Close Theme Selector",
                            onClick = { onClose() }
                        )
                        .padding(horizontal = 4.dp)
                        .testTag("theme_switcher_close_button")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = currentTheme.foreground.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(10.dp))

            // Text Description
            Text(
                text = "Select a color profile to load new terminal styles & background contrast. Prompt preview and color dots are shown inside each profile box below:",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = currentTheme.foreground.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Themes Grid Layout
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
            ) {
                items(TerminalTheme.values()) { t ->
                    val isSelected = currentTheme.id == t.id
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(t.background)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) currentTheme.promptColor else currentTheme.foreground.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Select Theme ${t.themeName}",
                                onClick = { onThemeSelected(t) }
                            )
                            .padding(8.dp)
                            .testTag("theme_card_${t.id}")
                    ) {
                        Column {
                            // Theme Title & Checkmark Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = t.themeName,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = t.foreground
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = t.successColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Immersive Terminal Prompt Mock Preview Block
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(t.background.copy(alpha = 0.5f))
                                    .border(1.dp, t.foreground.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "~ $",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = t.promptColor
                                    )
                                    Text(
                                        text = "ls -la",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        color = t.foreground
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Palette Color Chips Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ColorDot(color = t.background, description = "Bg")
                                ColorDot(color = t.foreground, description = "Fg")
                                ColorDot(color = t.promptColor, description = "Prompt")
                                ColorDot(color = t.errorColor, description = "Error")
                                ColorDot(color = t.successColor, description = "Success")
                                ColorDot(color = t.infoColor, description = "Info")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorDot(
    color: Color,
    description: String
) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
            .border(0.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
    )
}
