package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CommandAlias

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalAliasComponent(
    aliases: List<CommandAlias>,
    currentTheme: TerminalTheme,
    onAddAlias: (String, String) -> Unit,
    onDeleteAlias: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var newAlias by remember { mutableStateOf("") }
    var newCommand by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = currentTheme.background.copy(alpha = 0.95f)
        ),
        border = BorderStroke(1.dp, currentTheme.promptColor.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("alias_manager_card")
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
                        imageVector = Icons.AutoMirrored.Filled.Label,
                        contentDescription = null,
                        tint = currentTheme.promptColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "CUSTOM ALIAS MANAGER",
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
                        .clickable { onClose() }
                        .padding(horizontal = 4.dp)
                        .testTag("alias_manager_close_button")
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = currentTheme.foreground.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(10.dp))

            // Text Description
            Text(
                text = "Map shortcuts to complex terminal sequences. Aliases are persisted in Room and resolved automatically before execution.",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = currentTheme.foreground.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Input Form to Create Alias
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newAlias,
                    onValueChange = {
                        newAlias = it.take(15).filter { char -> char.isLetterOrDigit() || char == '_' }
                    },
                    placeholder = {
                        Text(
                            "Alias (e.g. ll)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = currentTheme.foreground.copy(alpha = 0.4f)
                        )
                    },
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = currentTheme.foreground
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = currentTheme.promptColor,
                        unfocusedBorderColor = currentTheme.foreground.copy(alpha = 0.2f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("new_alias_input")
                )

                OutlinedTextField(
                    value = newCommand,
                    onValueChange = { newCommand = it },
                    placeholder = {
                        Text(
                            "Command (e.g. ls -la)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = currentTheme.foreground.copy(alpha = 0.4f)
                        )
                    },
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = currentTheme.foreground
                    ),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = currentTheme.promptColor,
                        unfocusedBorderColor = currentTheme.foreground.copy(alpha = 0.2f),
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1.8f)
                        .height(48.dp)
                        .testTag("new_command_input")
                )

                IconButton(
                    onClick = {
                        if (newAlias.trim().isEmpty() || newCommand.trim().isEmpty()) {
                            errorMessage = "Alias and Command fields cannot be empty!"
                        } else {
                            onAddAlias(newAlias.trim(), newCommand.trim())
                            newAlias = ""
                            newCommand = ""
                            errorMessage = ""
                        }
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(currentTheme.promptColor.copy(alpha = 0.15f))
                        .size(36.dp)
                        .testTag("add_alias_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Alias",
                        tint = currentTheme.promptColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorMessage,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = currentTheme.errorColor,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = currentTheme.foreground.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            // Persisted Aliases List
            if (aliases.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "NO CUSTOM ALIASES REGISTERED",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = currentTheme.foreground.copy(alpha = 0.4f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(aliases) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(currentTheme.foreground.copy(alpha = 0.03f))
                                .border(1.dp, currentTheme.foreground.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = item.alias,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = currentTheme.promptColor
                                    )
                                    Text(
                                        text = "->",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = currentTheme.foreground.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = item.command,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = currentTheme.foreground
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onDeleteAlias(item.alias) },
                                modifier = Modifier.testTag("delete_alias_${item.alias}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Alias",
                                    tint = currentTheme.errorColor.copy(alpha = 0.8f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
