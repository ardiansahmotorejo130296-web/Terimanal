package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TerminalTheme
import com.example.data.CommandAlias

private val SUGGESTED_COMMANDS = listOf(
    "help", "clear", "diagnostics", "theme", "history", "ps", "top", "pwd", "ls", "cd",
    "mkdir", "rm", "cp", "mv", "touch", "cat", "echo", "uname", "df", "free", "uptime",
    "whoami", "ping", "ssh", "date", "ifconfig", "kill", "gemini", "login"
)

/**
 * A highly polished, custom terminal input component that captures user commands,
 * manages and visually displays the command history stack internally, and provides
 * intuitive navigation of previously executed commands.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalInputComponent(
    value: String,
    onValueChange: (String) -> Unit,
    onCommandSubmitted: (String) -> Unit,
    isSshActive: Boolean,
    sshHost: String,
    currentPath: String,
    theme: TerminalTheme,
    onTabAutocomplete: () -> Unit,
    onInterruptProcess: () -> Unit,
    isRunning: Boolean,
    externalHistoryList: List<String>,
    customAliases: List<CommandAlias> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Internal command history stack state to capture and manage history
    val historyStack = remember { mutableStateListOf<String>() }
    var historyIndex by remember { mutableStateOf(-1) }
    var savedInputText by remember { mutableStateOf("") }
    var showHistoryDropdown by remember { mutableStateOf(false) }

    // Live suggestion filter based on command prefix match (typed text before any space)
    val suggestions = remember(value, customAliases) {
        val trimmed = value.trim()
        if (trimmed.isNotEmpty() && !value.contains(" ")) {
            val aliasNames = customAliases.map { it.alias }
            val combined = (SUGGESTED_COMMANDS + aliasNames).distinct()
            combined.filter {
                it.startsWith(trimmed, ignoreCase = true) && !it.equals(trimmed, ignoreCase = true)
            }
        } else {
            emptyList()
        }
    }

    // Sync external history list with internal stack
    LaunchedEffect(externalHistoryList) {
        historyStack.clear()
        historyStack.addAll(externalHistoryList)
    }

    // Helper to navigate up the history stack
    val navigateHistoryUp = {
        if (historyStack.isNotEmpty()) {
            if (historyIndex == -1) {
                savedInputText = value
            }
            val nextIndex = historyIndex + 1
            if (nextIndex < historyStack.size) {
                historyIndex = nextIndex
                onValueChange(historyStack[nextIndex])
            }
        }
    }

    // Helper to navigate down the history stack
    val navigateHistoryDown = {
        if (historyStack.isNotEmpty() && historyIndex != -1) {
            val nextIndex = historyIndex - 1
            if (nextIndex >= 0) {
                historyIndex = nextIndex
                onValueChange(historyStack[nextIndex])
            } else {
                historyIndex = -1
                onValueChange(savedInputText)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(theme.background)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // --- Quick Utility Keys and History Stack Peek bar ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // TAB Autocompletion Key (Local mode only)
            Button(
                onClick = onTabAutocomplete,
                enabled = !isSshActive,
                colors = ButtonDefaults.buttonColors(
                    containerColor = theme.foreground.copy(alpha = 0.08f),
                    contentColor = theme.foreground,
                    disabledContainerColor = theme.foreground.copy(alpha = 0.03f),
                    disabledContentColor = theme.foreground.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, theme.foreground.copy(alpha = 0.15f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("tab_button")
            ) {
                Text(
                    text = "TAB",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // History Navigation UP Arrow
            IconButton(
                onClick = { navigateHistoryUp() },
                enabled = historyStack.isNotEmpty(),
                modifier = Modifier
                    .size(36.dp)
                    .background(theme.foreground.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                    .border(1.dp, theme.foreground.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .testTag("up_arrow_button")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Previous Command",
                    tint = if (historyStack.isNotEmpty()) theme.foreground else theme.foreground.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // History Navigation DOWN Arrow
            IconButton(
                onClick = { navigateHistoryDown() },
                enabled = historyIndex != -1,
                modifier = Modifier
                    .size(36.dp)
                    .background(theme.foreground.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                    .border(1.dp, theme.foreground.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                    .testTag("down_arrow_button")
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Next Command",
                    tint = if (historyIndex != -1) theme.foreground else theme.foreground.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Ctrl+C (Process kill / SSH interrupt)
            Button(
                onClick = onInterruptProcess,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) theme.errorColor.copy(alpha = 0.2f) else theme.foreground.copy(alpha = 0.08f),
                    contentColor = if (isRunning) theme.errorColor else theme.foreground
                ),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(1.dp, if (isRunning) theme.errorColor else theme.foreground.copy(alpha = 0.15f)),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier
                    .height(36.dp)
                    .weight(1f)
                    .testTag("ctrl_c_button")
            ) {
                Text(
                    text = "Ctrl+C",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Visual History Stack Toggle button
            IconButton(
                onClick = { showHistoryDropdown = !showHistoryDropdown },
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (showHistoryDropdown) theme.promptColor.copy(alpha = 0.15f) else theme.foreground.copy(alpha = 0.08f),
                        RoundedCornerShape(4.dp)
                    )
                    .border(
                        1.dp,
                        if (showHistoryDropdown) theme.promptColor else theme.foreground.copy(alpha = 0.15f),
                        RoundedCornerShape(4.dp)
                    )
                    .testTag("history_stack_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = "History Stack",
                    tint = if (showHistoryDropdown) theme.promptColor else theme.foreground,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // --- History Stack Dropdown Peek Panel ---
        AnimatedVisibility(
            visible = showHistoryDropdown,
            enter = expandVertically(animationSpec = tween(150)) + fadeIn(),
            exit = shrinkVertically(animationSpec = tween(150)) + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = theme.background.copy(alpha = 0.95f)
                ),
                border = BorderStroke(1.dp, theme.promptColor.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .heightIn(max = 160.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Command History Stack [${historyStack.size} items]",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = theme.promptColor
                        )
                        IconButton(
                            onClick = { showHistoryDropdown = false },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close History",
                                tint = theme.foreground.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (historyStack.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Stack is empty",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = theme.foreground.copy(alpha = 0.4f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            itemsIndexed(historyStack.reversed()) { revIdx, command ->
                                val actualIdx = historyStack.size - 1 - revIdx
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (historyIndex == actualIdx) theme.foreground.copy(alpha = 0.1f) else Color.Transparent,
                                            RoundedCornerShape(4.dp)
                                        )
                                        .clickable(
                                            role = Role.Button,
                                            onClickLabel = "Select command ${command}"
                                        ) {
                                            historyIndex = actualIdx
                                            onValueChange(command)
                                            showHistoryDropdown = false
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "[${actualIdx}]",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = theme.promptColor.copy(alpha = 0.7f),
                                        modifier = Modifier.width(36.dp)
                                    )
                                    Text(
                                        text = command,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = theme.foreground
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Auto-Complete Suggestions Row ---
        AnimatedVisibility(
            visible = suggestions.isNotEmpty(),
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SUGGESTIONS:",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = theme.promptColor.copy(alpha = 0.8f)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(suggestions) { cmd ->
                        SuggestionChip(
                            text = cmd,
                            theme = theme,
                            onClick = {
                                onValueChange("$cmd ")
                            }
                        )
                    }
                }
            }
        }

        // --- Bottom Directory Prompt Input Row ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dynamic path prompt prefix
            Text(
                text = if (isSshActive) {
                    "ssh@$sshHost:$ "
                } else {
                    val displayPath = if (currentPath.endsWith("/files") || currentPath.endsWith("/files/")) "~" else currentPath.substringAfterLast("/")
                    "$displayPath $ "
                },
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = theme.promptColor
            )

            // Blinking block cursor transition
            val cursorTransition = rememberInfiniteTransition(label = "cursor")
            val cursorVisible by cursorTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "blink"
            )

            OutlinedTextField(
                value = value,
                onValueChange = {
                    onValueChange(it)
                    // Reset history pointer if user typing custom text
                    if (historyIndex != -1 && (historyIndex >= historyStack.size || historyStack[historyIndex] != it)) {
                        historyIndex = -1
                    }
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = theme.foreground
                ),
                placeholder = {
                    Text(
                        text = "enter command...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = theme.foreground.copy(alpha = 0.4f)
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("command_input_field"),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Go,
                    autoCorrectEnabled = false
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (value.isNotBlank()) {
                            onCommandSubmitted(value)
                            historyIndex = -1
                        }
                    }
                ),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = theme.foreground
                )
            )

            // Render blinking cursor box next to text input field
            Box(
                modifier = Modifier
                    .size(width = 8.dp, height = 16.dp)
                    .alpha(cursorVisible)
                    .background(theme.foreground)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (value.isNotBlank()) {
                        onCommandSubmitted(value)
                        historyIndex = -1
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .background(theme.promptColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                    .testTag("submit_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Execute",
                    tint = theme.promptColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun SuggestionChip(
    text: String,
    theme: TerminalTheme,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(theme.promptColor.copy(alpha = 0.12f))
            .border(1.dp, theme.promptColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = "Use suggestion $text",
                onClick = { onClick() }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .testTag("suggestion_chip_$text")
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = theme.promptColor
        )
    }
}
