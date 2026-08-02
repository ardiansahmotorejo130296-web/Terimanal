package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.ApiException
import androidx.compose.ui.platform.LocalContext
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.FolderShared
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.TerminalSession
import com.example.ui.OutputType
import com.example.ui.TerminalTheme
import com.example.ui.TerminalViewModel
import com.example.ui.TerminalInputComponent
import com.example.ui.SystemDiagnosticsComponent
import com.example.ui.TerminalThemeSwitcherComponent
import com.example.ui.TerminalAliasComponent
import com.example.ui.RealtimeSysInfoComponent
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: TerminalViewModel = viewModel()
            val theme by viewModel.currentTheme.collectAsStateWithLifecycle()

            MaterialTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("terminal_scaffold"),
                    contentWindowInsets = WindowInsets.safeDrawing
                ) { innerPadding ->
                    TerminalScreen(
                        viewModel = viewModel,
                        theme = theme,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalScreen(
    viewModel: TerminalViewModel,
    theme: TerminalTheme,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val googleSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            viewModel.onLoginSuccess(account?.account?.name)
        } catch (e: ApiException) {
            viewModel.onLoginError("Failed (Code: ${e.statusCode})")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.triggerLogin.collect {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(
                    Scope("https://www.googleapis.com/auth/drive.readonly"),
                    Scope("https://www.googleapis.com/auth/gmail.readonly")
                )
                .build()
            val client = GoogleSignIn.getClient(context, gso)
            googleSignInLauncher.launch(client.signInIntent)
        }
    }

    val outputs by viewModel.outputs.collectAsStateWithLifecycle()
    val currentPath by viewModel.currentDirectoryPath.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val isSshActive by viewModel.isSshActive.collectAsStateWithLifecycle()
    val isSshConnecting by viewModel.isSshConnecting.collectAsStateWithLifecycle()
    val sshHost by viewModel.sshHost.collectAsStateWithLifecycle()
    val sshUsername by viewModel.sshUsername.collectAsStateWithLifecycle()
    val savedSessions by viewModel.savedSessions.collectAsStateWithLifecycle(emptyList())
    val processes by viewModel.processes.collectAsStateWithLifecycle()
    val commandHistoryList by viewModel.commandHistoryList.collectAsStateWithLifecycle()
    val systemDiagnosticsData by viewModel.systemDiagnostics.collectAsStateWithLifecycle()
    val customAliases by viewModel.customAliases.collectAsStateWithLifecycle()
    val showSysInfoBlock by viewModel.showSysInfoBlock.collectAsStateWithLifecycle()

    val inputText = viewModel.inputText
    val keyboardController = LocalSoftwareKeyboardController.current
    val lazyListState = rememberLazyListState()

    // Dialog flags
    var showThemeDropdown by remember { mutableStateOf(false) }
    var showSshDialog by remember { mutableStateOf(false) }
    var showSessionsDialog by remember { mutableStateOf(false) }
    var showProcessDialog by remember { mutableStateOf(false) }
    var showDiagnosticsBlock by remember { mutableStateOf(false) }
    var showThemeSwitcherBlock by remember { mutableStateOf(false) }
    var showAliasBlock by remember { mutableStateOf(false) }

    // Auto scroll to bottom when outputs list size increases
    LaunchedEffect(outputs.size) {
        if (outputs.isNotEmpty()) {
            lazyListState.animateScrollToItem(outputs.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background)
    ) {
        // --- TOP HEADER BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Retro pulse dot indicating console running state
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .alpha(if (isRunning || isSshConnecting) pulseAlpha else 1.0f)
                        .background(
                            if (isSshActive) theme.successColor
                            else if (isRunning || isSshConnecting) theme.errorColor
                            else theme.promptColor
                        )
                )

                Column {
                    Text(
                        text = if (isSshActive) "TerminalDroid [SSH]" else "TerminalDroid",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = theme.foreground
                    )
                    Text(
                        text = if (isSshConnecting) {
                            "connecting ssh..."
                        } else if (isSshActive) {
                            "remote: $sshUsername@$sshHost"
                        } else if (isRunning) {
                            "executing command..."
                        } else {
                            "local sandbox ready"
                        },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = theme.foreground.copy(alpha = 0.6f)
                    )
                }
            }

            // Quick Actions: SSH Client Dialog, Sessions Manager, Themes, Clear
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // SSH Quick Connect Trigger
                IconButton(
                    onClick = { showSshDialog = true },
                    modifier = Modifier.testTag("ssh_dialog_trigger")
                ) {
                    Icon(
                        imageVector = if (isSshActive) Icons.Default.CloudDone else Icons.Default.Cloud,
                        contentDescription = "SSH Client",
                        tint = if (isSshActive) theme.successColor else theme.foreground.copy(alpha = 0.8f)
                    )
                }

                // Sessions Manager Dialog Trigger
                IconButton(
                    onClick = { showSessionsDialog = true },
                    modifier = Modifier.testTag("sessions_dialog_trigger")
                ) {
                    Icon(
                        imageVector = Icons.Default.Bookmarks,
                        contentDescription = "Saved Sessions",
                        tint = theme.foreground.copy(alpha = 0.8f)
                    )
                }

                // Process Monitor Dialog Trigger
                IconButton(
                    onClick = { showProcessDialog = true },
                    modifier = Modifier.testTag("process_monitor_dialog_trigger")
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = "Process Monitor",
                        tint = theme.foreground.copy(alpha = 0.8f)
                    )
                }

                // Toggle Live System Diagnostics block
                IconButton(
                    onClick = { showDiagnosticsBlock = !showDiagnosticsBlock },
                    modifier = Modifier.testTag("diagnostics_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = "System Diagnostics",
                        tint = if (showDiagnosticsBlock) theme.successColor else theme.foreground.copy(alpha = 0.8f)
                    )
                }

                // Toggle Custom Alias Manager block
                IconButton(
                    onClick = { showAliasBlock = !showAliasBlock },
                    modifier = Modifier.testTag("alias_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Label,
                        contentDescription = "Custom Alias Manager",
                        tint = if (showAliasBlock) theme.successColor else theme.foreground.copy(alpha = 0.8f)
                    )
                }

                // Clean/Clear Outputs
                IconButton(
                    onClick = { viewModel.executeCommand("clear") },
                    modifier = Modifier.testTag("clear_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Console",
                        tint = theme.foreground.copy(alpha = 0.8f)
                    )
                }

                // Palette Themes selector toggle
                IconButton(
                    onClick = { showThemeSwitcherBlock = !showThemeSwitcherBlock },
                    modifier = Modifier.testTag("theme_selector_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Change Theme",
                        tint = if (showThemeSwitcherBlock) theme.successColor else theme.foreground.copy(alpha = 0.8f)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showDiagnosticsBlock,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            SystemDiagnosticsComponent(
                data = systemDiagnosticsData,
                theme = theme,
                onClose = { showDiagnosticsBlock = false }
            )
        }

        AnimatedVisibility(
            visible = showSysInfoBlock,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            RealtimeSysInfoComponent(
                data = systemDiagnosticsData,
                theme = theme,
                onClose = { viewModel.setSysInfoBlockVisible(false) }
            )
        }

        AnimatedVisibility(
            visible = showThemeSwitcherBlock,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            TerminalThemeSwitcherComponent(
                currentTheme = theme,
                onThemeSelected = { t ->
                    viewModel.setThemeById(t.id)
                },
                onClose = { showThemeSwitcherBlock = false }
            )
        }

        AnimatedVisibility(
            visible = showAliasBlock,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            TerminalAliasComponent(
                aliases = customAliases,
                currentTheme = theme,
                onAddAlias = { alias, cmd ->
                    viewModel.addAlias(alias, cmd)
                },
                onDeleteAlias = { alias ->
                    viewModel.deleteAlias(alias)
                },
                onClose = { showAliasBlock = false }
            )
        }

        HorizontalDivider(
            color = theme.foreground.copy(alpha = 0.15f),
            thickness = 1.dp
        )

        // --- RETRO SCANLINE CONSOLE OUTPUT LIST ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .drawBehind {
                    // Draw retro horizontal crt scanlines
                    val scanlineSpacing = 12.dp.toPx()
                    val totalHeight = size.height
                    var currentY = 0f
                    while (currentY < totalHeight) {
                        drawLine(
                            color = Color.Black.copy(alpha = 0.08f),
                            start = Offset(0f, currentY),
                            end = Offset(size.width, currentY),
                            strokeWidth = 2.dp.toPx()
                        )
                        currentY += scanlineSpacing
                    }
                }
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("terminal_output_list"),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(outputs) { output ->
                    val textColor = when (output.type) {
                        OutputType.INPUT -> theme.promptColor
                        OutputType.STDOUT -> theme.foreground
                        OutputType.STDERR -> theme.errorColor
                        OutputType.SUCCESS -> theme.successColor
                        OutputType.INFO -> theme.infoColor
                        OutputType.ASCII_ART -> theme.foreground
                    }

                    val textStyle = if (output.type == OutputType.INPUT) {
                        FontWeight.Bold
                    } else {
                        FontWeight.Normal
                    }

                    // For inputted command logs, render prompt prefixes differently
                    if (output.type == OutputType.INPUT) {
                        val commandText = output.text
                        val parts = commandText.split("$ ")
                        if (parts.size >= 2) {
                            val promptPart = parts[0] + "$ "
                            val inputCmdPart = parts.subList(1, parts.size).joinToString("$ ")

                            Text(
                                text = buildAnnotatedString {
                                    withStyle(style = SpanStyle(color = theme.promptColor, fontWeight = FontWeight.Bold)) {
                                        append(promptPart)
                                    }
                                    withStyle(style = SpanStyle(color = theme.foreground, fontWeight = FontWeight.Normal)) {
                                        append(inputCmdPart)
                                    }
                                },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text(
                                text = output.text,
                                color = textColor,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = textStyle,
                                fontSize = 13.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Text(
                            text = output.text,
                            color = textColor,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = textStyle,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        HorizontalDivider(
            color = theme.foreground.copy(alpha = 0.15f),
            thickness = 1.dp
        )

        // --- COMMAND ACCESSORY PAD & INTERACTIVE SHORTCUTS ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.background)
        ) {
            // Scrollable row of quick native commands
            val quickCommands = if (isSshActive) {
                listOf("exit", "ls -la", "uname -a", "df -h", "uptime", "free -m", "clear")
            } else {
                listOf("ls", "pwd", "apt update", "apt list", "dnslookup google.com", "subfinder google.com", "whois google.com", "ipinfo", "ssh", "history", "clear")
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                LazyRow(
                    modifier = Modifier.height(38.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(quickCommands) { cmd ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = theme.foreground.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .clickable { viewModel.executeCommand(cmd) }
                                .border(
                                    width = 1.dp,
                                    color = theme.foreground.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                        ) {
                            Text(
                                text = cmd,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = theme.foreground,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Modular, feature-rich Terminal Input Component that manages and visualizes command history stack
            TerminalInputComponent(
                value = inputText.value,
                onValueChange = { inputText.value = it },
                onCommandSubmitted = { cmd ->
                    viewModel.executeCommand(cmd)
                },
                isSshActive = isSshActive,
                sshHost = sshHost,
                currentPath = currentPath,
                theme = theme,
                onTabAutocomplete = { viewModel.handleTabAutocomplete() },
                onInterruptProcess = { viewModel.interruptActiveProcess() },
                isRunning = isRunning,
                externalHistoryList = commandHistoryList,
                customAliases = customAliases
            )
        }
    }

    // --- SSH QUICK CONNECT DIALOG ---
    if (showSshDialog) {
        var hostInput by remember { mutableStateOf("test.rebex.net") }
        var portInput by remember { mutableStateOf("22") }
        var usernameInput by remember { mutableStateOf("demo") }
        var passwordInput by remember { mutableStateOf("") }
        var privateKeyVal by remember { mutableStateOf("") }
        var showPassword by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showSshDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Cloud, contentDescription = null, tint = theme.promptColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "SSH Quick Connect",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSshActive) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = theme.successColor.copy(alpha = 0.1f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Connected to $sshHost",
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = theme.successColor
                                )
                                Button(
                                    onClick = {
                                        viewModel.disconnectSsh()
                                        showSshDialog = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.errorColor),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text("Disconnect", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    // Host & Port
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = hostInput,
                            onValueChange = { hostInput = it },
                            label = { Text("Host / IP") },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = portInput,
                            onValueChange = { portInput = it },
                            label = { Text("Port") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }

                    // Username
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text("Username") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Password
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password (Optional)") },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Private Key content
                    OutlinedTextField(
                        value = privateKeyVal,
                        onValueChange = { privateKeyVal = it },
                        label = { Text("Private Key (PEM, Optional)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.connectSsh(
                            host = hostInput,
                            username = usernameInput,
                            portStr = portInput,
                            passwordVal = passwordInput,
                            privateKeyContent = privateKeyVal
                        )
                        showSshDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.promptColor),
                    enabled = hostInput.isNotBlank() && usernameInput.isNotBlank()
                ) {
                    Text("Connect", fontFamily = FontFamily.Monospace)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSshDialog = false }) {
                    Text("Close", fontFamily = FontFamily.Monospace)
                }
            }
        )
    }

    // --- SESSIONS MANAGER DIALOG ---
    if (showSessionsDialog) {
        var newSessionName by remember { mutableStateOf("") }
        val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

        AlertDialog(
            onDismissRequest = { showSessionsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bookmarks, contentDescription = null, tint = theme.promptColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Sessions Manager",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    // Save Current Session Form
                    Card(
                        colors = CardDefaults.cardColors(containerColor = theme.foreground.copy(alpha = 0.05f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Save Current Session",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = theme.promptColor,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = newSessionName,
                                    onValueChange = { newSessionName = it },
                                    placeholder = { Text("Session Name (e.g., Work)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        viewModel.saveCurrentSession(newSessionName)
                                        newSessionName = ""
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.promptColor),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text("Save", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // List of saved sessions
                    Text(
                        "Saved Sessions logs",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = theme.foreground.copy(alpha = 0.8f)
                    )

                    if (savedSessions.isEmpty()) {
                        Text(
                            "No saved sessions found.",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = theme.foreground.copy(alpha = 0.5f),
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(savedSessions) { session ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = theme.foreground.copy(alpha = 0.08f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = session.name,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 13.sp,
                                                color = theme.successColor
                                            )
                                            Text(
                                                text = "Path: " + if (session.workingDirectory == "/data/user/0/com.aistudio.terminaldroid.qpywz/files") "~" else session.workingDirectory.substringAfterLast("/"),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = theme.foreground.copy(alpha = 0.6f)
                                            )
                                            Text(
                                                text = dateFormat.format(Date(session.timestamp)),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                color = theme.foreground.copy(alpha = 0.4f)
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            // Load button
                                            IconButton(
                                                onClick = {
                                                    viewModel.loadSession(session)
                                                    showSessionsDialog = false
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Launch,
                                                    contentDescription = "Load Session",
                                                    tint = theme.promptColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            // Delete button
                                            IconButton(
                                                onClick = { viewModel.deleteSession(session.id) }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete Session",
                                                    tint = theme.errorColor,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showSessionsDialog = false }) {
                    Text("Close", fontFamily = FontFamily.Monospace)
                }
            }
        )
    }

    // --- PROCESS MONITOR DIALOG (TOP) ---
    if (showProcessDialog) {
        AlertDialog(
            onDismissRequest = { showProcessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Dns, contentDescription = null, tint = theme.promptColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Process Monitor (top)",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = theme.foreground
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                ) {
                    Text(
                        "Active/Background processes running inside sandboxed shell context.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = theme.foreground.copy(alpha = 0.7f)
                    )

                    HorizontalDivider(color = theme.foreground.copy(alpha = 0.15f))

                    if (processes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No processes registered.",
                                fontFamily = FontFamily.Monospace,
                                color = theme.foreground.copy(alpha = 0.5f)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(processes) { proc ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = theme.foreground.copy(alpha = 0.05f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "PID: ${proc.pid}",
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 12.sp,
                                                    color = theme.promptColor
                                                )
                                                if (proc.isSystem) {
                                                    Card(
                                                        colors = CardDefaults.cardColors(containerColor = theme.foreground.copy(alpha = 0.1f)),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = "system",
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 9.sp,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                            color = theme.foreground.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = proc.name,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 13.sp,
                                                color = theme.foreground,
                                                maxLines = 1
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Text(
                                                    text = "CPU: ${proc.cpu}%",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = theme.successColor.copy(alpha = 0.8f)
                                                )
                                                Text(
                                                    text = "MEM: ${proc.memory}",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    color = theme.foreground.copy(alpha = 0.6f)
                                                )
                                                Text(
                                                    text = proc.status,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (proc.status) {
                                                        "RUNNING" -> theme.successColor
                                                        "KILLED" -> theme.errorColor
                                                        "FAILED" -> theme.errorColor
                                                        else -> theme.foreground.copy(alpha = 0.5f)
                                                    }
                                                )
                                            }
                                        }

                                        if (proc.status == "RUNNING" && !proc.isSystem) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.killProcess(proc.pid)
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Cancel,
                                                    contentDescription = "Terminate Process",
                                                    tint = theme.errorColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showProcessDialog = false }) {
                    Text("Close", fontFamily = FontFamily.Monospace, color = theme.promptColor)
                }
            }
        )
    }
}

@Composable
fun TerminalUtilityButton(
    text: String,
    theme: TerminalTheme,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = theme.foreground.copy(alpha = 0.08f),
    borderColor: Color = theme.foreground.copy(alpha = 0.2f),
    textColor: Color = theme.foreground,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) containerColor else theme.foreground.copy(alpha = 0.02f)
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
            .clickable(enabled = enabled) { onClick() }
            .border(
                width = 1.dp,
                color = if (enabled) borderColor else theme.foreground.copy(alpha = 0.05f),
                shape = RoundedCornerShape(6.dp)
            )
            .height(34.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) textColor else theme.foreground.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun TerminalUtilityIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    theme: TerminalTheme,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = theme.foreground.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
            .clickable { onClick() }
            .border(1.dp, theme.foreground.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
            .height(34.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = theme.foreground,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
