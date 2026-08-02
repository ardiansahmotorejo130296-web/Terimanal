package com.example.ui

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.StatFs
import android.os.SystemClock
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.io.FileReader
import com.example.data.TerminalDatabase
import com.example.data.TerminalRepository
import com.example.data.TerminalSession
import com.example.data.CommandAlias
import com.jcraft.jsch.ChannelShell
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.PrintStream
import java.net.URL
import java.net.HttpURLConnection
import java.net.Socket
import java.net.InetSocketAddress
import org.json.JSONObject
import org.json.JSONArray

enum class TerminalTheme(
    val id: String,
    val themeName: String,
    val background: Color,
    val foreground: Color,
    val promptColor: Color,
    val errorColor: Color,
    val successColor: Color,
    val infoColor: Color
) {
    MATRIX_GREEN(
        "matrix",
        "Matrix Green",
        background = Color(0xFF030A03),
        foreground = Color(0xFF33FF33),
        promptColor = Color(0xFF00FF00),
        errorColor = Color(0xFFFF3333),
        successColor = Color(0xFF00FFCC),
        infoColor = Color(0xFF88FF88)
    ),
    AMBER_CRT(
        "amber",
        "Retro Amber",
        background = Color(0xFF0F0700),
        foreground = Color(0xFFFFB000),
        promptColor = Color(0xFFFFCC00),
        errorColor = Color(0xFFFF3300),
        successColor = Color(0xFFFFA500),
        infoColor = Color(0xFFFFD700)
    ),
    DRACULA(
        "dracula",
        "Dracula Noir",
        background = Color(0xFF1E1F29),
        foreground = Color(0xFFF8F8F2),
        promptColor = Color(0xFF50FA7B),
        errorColor = Color(0xFFFF5555),
        successColor = Color(0xFF8BE9FD),
        infoColor = Color(0xFFBD93F9)
    ),
    CYBERPUNK(
        "cyberpunk",
        "Cyberpunk Neon",
        background = Color(0xFF150524),
        foreground = Color(0xFF00FFFF),
        promptColor = Color(0xFFFF007F),
        errorColor = Color(0xFFFF0055),
        successColor = Color(0xFF00FF66),
        infoColor = Color(0xFFD600FF)
    ),
    CLASSIC_MONO(
        "classic",
        "Classic Mono",
        background = Color(0xFF0A0A0A),
        foreground = Color(0xFFDDDDDD),
        promptColor = Color(0xFFFFFFFF),
        errorColor = Color(0xFFFF5252),
        successColor = Color(0xFF4CAF50),
        infoColor = Color(0xFF2196F3)
    ),
    SOLARIZED_DARK(
        "solarized",
        "Solarized Dark",
        background = Color(0xFF002B36),
        foreground = Color(0xFF839496),
        promptColor = Color(0xFF268BD2),
        errorColor = Color(0xFFDC322F),
        successColor = Color(0xFF859900),
        infoColor = Color(0xFFB58900)
    ),
    MONOKAI(
        "monokai",
        "Monokai Pro",
        background = Color(0xFF272822),
        foreground = Color(0xFFF8F8F2),
        promptColor = Color(0xFFF92672),
        errorColor = Color(0xFFF92672),
        successColor = Color(0xFFA6E22E),
        infoColor = Color(0xFF66D9EF)
    )
}

data class OutputLine(
    val text: String,
    val type: OutputType
)

enum class OutputType {
    INPUT,
    STDOUT,
    STDERR,
    SUCCESS,
    INFO,
    ASCII_ART
}

data class TerminalProcess(
    val pid: Int,
    val name: String,
    val status: String, // "RUNNING", "COMPLETED", "KILLED", "FAILED"
    val cpu: Double,    // cpu usage %
    val memory: String, // memory usage e.g. "12 MB"
    val startTime: Long,
    val isSystem: Boolean = false,
    @Transient val job: kotlinx.coroutines.Job? = null,
    @Transient val process: java.lang.Process? = null
)

class TerminalViewModel(application: Application) : AndroidViewModel(application) {
    private val database = TerminalDatabase.getDatabase(application)
    private val repository = TerminalRepository(database)

    // Process Tracking State
    private val _processes = MutableStateFlow<List<TerminalProcess>>(emptyList())
    val processes: StateFlow<List<TerminalProcess>> = _processes.asStateFlow()
    private var nextPid = 2000

    private val prefs = application.getSharedPreferences("terminal_prefs", Context.MODE_PRIVATE)
    private val _installedPackages = MutableStateFlow<Set<String>>(
        prefs.getStringSet("installed_packages", emptySet()) ?: emptySet()
    )
    val installedPackages: StateFlow<Set<String>> = _installedPackages.asStateFlow()

    // Terminal Outputs
    private val _outputs = MutableStateFlow<List<OutputLine>>(emptyList())
    val outputs: StateFlow<List<OutputLine>> = _outputs.asStateFlow()

    // Active Theme
    private val _currentTheme = MutableStateFlow(TerminalTheme.MATRIX_GREEN)
    val currentTheme: StateFlow<TerminalTheme> = _currentTheme.asStateFlow()

    // Current Directory Path
    private val _currentDirectoryPath = MutableStateFlow(application.filesDir.absolutePath)
    val currentDirectoryPath: StateFlow<String> = _currentDirectoryPath.asStateFlow()

    // Running State
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    // Root Access State
    private val _isRoot = MutableStateFlow(false)
    val isRoot: StateFlow<Boolean> = _isRoot.asStateFlow()

    // Text Input State
    val inputText = mutableStateOf("")

    private val _triggerLogin = kotlinx.coroutines.flow.MutableSharedFlow<Unit>()
    val triggerLogin = _triggerLogin.asSharedFlow()

    fun onLoginSuccess(accountName: String?) {
        addOutput("Google Auth success! Signed in as ${accountName ?: "User"}. Drive and Gmail APIs unlocked.", OutputType.SUCCESS)
    }

    fun onLoginError(message: String) {
        addOutput("Login failed: $message", OutputType.STDERR)
    }

    // History Traverse Indexes
    private val _commandHistoryList = MutableStateFlow<List<String>>(emptyList())
    val commandHistoryList: StateFlow<List<String>> = _commandHistoryList.asStateFlow()
    private val historyIndex = mutableStateOf(-1)
    private var savedInputText = ""

    // --- SESSIONS STATE ---
    val savedSessions: kotlinx.coroutines.flow.Flow<List<TerminalSession>> = repository.allSessions

    // --- CUSTOM ALIASES STATE ---
    private val _customAliases = MutableStateFlow<List<CommandAlias>>(emptyList())
    val customAliases: StateFlow<List<CommandAlias>> = _customAliases.asStateFlow()

    fun addAlias(alias: String, command: String) {
        viewModelScope.launch {
            repository.addAlias(alias.trim(), command.trim())
        }
    }

    fun deleteAlias(alias: String) {
        viewModelScope.launch {
            repository.deleteAlias(alias.trim())
        }
    }

    // --- SSH CLIENT STATE ---
    private val _isSshActive = MutableStateFlow(false)
    val isSshActive: StateFlow<Boolean> = _isSshActive.asStateFlow()

    private val _isSshConnecting = MutableStateFlow(false)
    val isSshConnecting: StateFlow<Boolean> = _isSshConnecting.asStateFlow()

    private val _sshHost = MutableStateFlow("")
    val sshHost: StateFlow<String> = _sshHost.asStateFlow()

    private val _sshUsername = MutableStateFlow("")
    val sshUsername: StateFlow<String> = _sshUsername.asStateFlow()

    // --- LIVE SYSTEM DIAGNOSTICS FLOW ---
    private val _systemDiagnostics = MutableStateFlow<SystemDiagnosticsData?>(null)
    val systemDiagnostics: StateFlow<SystemDiagnosticsData?> = _systemDiagnostics.asStateFlow()

    // --- SYSINFO REAL-TIME DIAGNOSTIC MODULE STATE ---
    private val _showSysInfoBlock = MutableStateFlow(false)
    val showSysInfoBlock: StateFlow<Boolean> = _showSysInfoBlock.asStateFlow()

    fun setSysInfoBlockVisible(visible: Boolean) {
        _showSysInfoBlock.value = visible
    }

    // Active Local Processes / SSH references
    private var activeProcess: Process? = null
    private var activeJob: Job? = null

    // JSch objects
    private var jschSession: Session? = null
    private var jschChannel: ChannelShell? = null
    private var channelInput: PrintStream? = null
    private var sshReaderJob: Job? = null

    private fun initSystemProcesses() {
        val now = System.currentTimeMillis()
        _processes.value = listOf(
            TerminalProcess(1, "/sbin/init", "RUNNING", 0.1, "4.2 MB", now - 3600000, true),
            TerminalProcess(2, "[kthreadd]", "RUNNING", 0.0, "0.0 MB", now - 3600000, true),
            TerminalProcess(77, "systemd-journald", "RUNNING", 0.3, "12.8 MB", now - 1800000, true),
            TerminalProcess(120, "sshd", "RUNNING", 0.1, "8.5 MB", now - 1200000, true),
            TerminalProcess(524, "com.android.systemui", "RUNNING", 1.2, "142.0 MB", now - 3000000, true),
            TerminalProcess(1024, "terminaldroid-daemon", "RUNNING", 0.6, "32.4 MB", now - 600000, true)
        )
    }

    private fun registerProcess(name: String, job: Job? = null, process: Process? = null): Int {
        val pid = nextPid++
        val initialCpu = (0.5 + Math.random() * 4.5).let { Math.round(it * 10) / 10.0 }
        val initialMem = "${(5 + (Math.random() * 25).toInt())} MB"
        val newProc = TerminalProcess(
            pid = pid,
            name = name,
            status = "RUNNING",
            cpu = initialCpu,
            memory = initialMem,
            startTime = System.currentTimeMillis(),
            isSystem = false,
            job = job,
            process = process
        )
        _processes.value = _processes.value + newProc
        return pid
    }

    private fun completeProcess(pid: Int, success: Boolean = true) {
        _processes.value = _processes.value.map { proc ->
            if (proc.pid == pid) {
                proc.copy(
                    status = if (success) "COMPLETED" else "FAILED",
                    cpu = 0.0,
                    memory = "0.0 MB"
                )
            } else {
                proc
            }
        }
    }

    fun killProcess(pid: Int): Boolean {
        var found = false
        _processes.value = _processes.value.map { proc ->
            if (proc.pid == pid) {
                found = true
                if (proc.status == "RUNNING") {
                    proc.job?.cancel()
                    try {
                        proc.process?.destroy()
                    } catch (e: Exception) {
                        // ignore
                    }
                    proc.copy(
                        status = "KILLED",
                        cpu = 0.0,
                        memory = "0.0 MB"
                    )
                } else {
                    proc
                }
            } else {
                proc
            }
        }
        return found
    }

    init {
        initSystemProcesses()
        startDiagnosticsMonitoring()
        // Load Command History from Room to populate autocomplete/navigation
        viewModelScope.launch {
            repository.recentCommands.collectLatest { history ->
                _commandHistoryList.value = history.map { it.command }
            }
        }
        // Load custom aliases
        viewModelScope.launch {
            repository.allAliases.collectLatest { aliases ->
                _customAliases.value = aliases
            }
        }

        // Welcome Greeting
        showWelcomeMessage()
    }

    private fun showWelcomeMessage() {
        _outputs.value = listOf(
            OutputLine("==========================================", OutputType.INFO),
            OutputLine("   TerminalDroid - Active Console v2.0   ", OutputType.SUCCESS),
            OutputLine("==========================================", OutputType.INFO),
            OutputLine("Type 'help' to view available interactive commands.", OutputType.INFO),
            OutputLine("Featuring Built-in local commands, Room Sessions, and SSH Client.", OutputType.INFO),
            OutputLine("Fully Sandboxed in context.filesDir. You can use 'mkdir', 'ls', 'echo', etc.", OutputType.INFO),
            OutputLine("", OutputType.STDOUT)
        )
    }

    fun executeCommand(command: String) {
        val trimmedCommand = command.trim()
        if (trimmedCommand.isEmpty()) return

        // If SSH Client is active, route command to remote server
        if (_isSshActive.value) {
            addOutput("ssh@${_sshHost.value}$ $trimmedCommand", OutputType.INPUT)
            viewModelScope.launch {
                repository.addCommand(trimmedCommand)
            }
            inputText.value = ""
            sendSshCommand(trimmedCommand)
            return
        }

        // 1. Log Input command
        val userLabel = if (_isRoot.value) "root" else "guest"
        val promptSymbol = if (_isRoot.value) "#" else "$"
        val promptPrefix = "$userLabel@terminaldroid:${getCompactPath(_currentDirectoryPath.value)}$promptSymbol "
        addOutput(promptPrefix + trimmedCommand, OutputType.INPUT)

        // 2. Save command to Room database
        viewModelScope.launch {
            repository.addCommand(trimmedCommand)
        }

        // Reset history pointer
        historyIndex.value = -1
        inputText.value = ""

        // 3. Resolve Custom Command Aliases (recursively up to 3 levels, safe from cycles)
        var resolvedCommand = trimmedCommand
        var parts = resolvedCommand.split("\\s+".toRegex())
        var baseCmd = parts[0]
        val seenAliases = mutableSetOf<String>()
        var aliasMatch = _customAliases.value.find { it.alias.equals(baseCmd, ignoreCase = true) }

        while (aliasMatch != null && !seenAliases.contains(aliasMatch.alias)) {
            seenAliases.add(aliasMatch.alias)
            addOutput("alias: '${aliasMatch.alias}' resolved to '${aliasMatch.command}'", OutputType.INFO)
            val remaining = if (parts.size > 1) " " + parts.drop(1).joinToString(" ") else ""
            resolvedCommand = aliasMatch.command + remaining
            parts = resolvedCommand.split("\\s+".toRegex())
            baseCmd = parts[0]
            aliasMatch = _customAliases.value.find { it.alias.equals(baseCmd, ignoreCase = true) }
        }

        // 4. Process the Command (Local Native commands vs. Exec shell)
        val finalParts = resolvedCommand.split("\\s+".toRegex())
        val finalBaseCmd = finalParts[0].lowercase()

        when (finalBaseCmd) {
            "alias" -> {
                val aliasArg = resolvedCommand.substringAfter("alias").trim()
                if (aliasArg.isEmpty()) {
                    val list = _customAliases.value
                    if (list.isEmpty()) {
                        addOutput("No custom aliases defined. Define one with: alias [name]=[command]", OutputType.INFO)
                    } else {
                        addOutput("Active Custom Command Aliases:", OutputType.INFO)
                        list.forEach {
                            addOutput("alias ${it.alias}='${it.command}'", OutputType.STDOUT)
                        }
                    }
                } else {
                    if (aliasArg.contains("=")) {
                        val key = aliasArg.substringBefore("=").trim()
                        var value = aliasArg.substringAfter("=").trim()
                        if (value.startsWith("\"") && value.endsWith("\"") && value.length >= 2) {
                            value = value.substring(1, value.length - 1)
                        } else if (value.startsWith("'") && value.endsWith("'") && value.length >= 2) {
                            value = value.substring(1, value.length - 1)
                        }
                        if (key.isEmpty() || value.isEmpty()) {
                            addOutput("Usage: alias [name]=[command] (e.g., alias ll=\"ls -la\")", OutputType.STDERR)
                        } else {
                            addAlias(key, value)
                            addOutput("Alias '$key' registered successfully.", OutputType.SUCCESS)
                        }
                    } else {
                        val found = _customAliases.value.find { it.alias.equals(aliasArg, ignoreCase = true) }
                        if (found != null) {
                            addOutput("alias ${found.alias}='${found.command}'", OutputType.STDOUT)
                        } else {
                            addOutput("alias: $aliasArg: not found", OutputType.STDERR)
                        }
                    }
                }
            }
            "unalias" -> {
                if (finalParts.size > 1) {
                    val target = finalParts[1]
                    val found = _customAliases.value.any { it.alias.equals(target, ignoreCase = true) }
                    if (found) {
                        deleteAlias(target)
                        addOutput("Alias '$target' removed successfully.", OutputType.SUCCESS)
                    } else {
                        addOutput("unalias: $target: not found", OutputType.STDERR)
                    }
                } else {
                    addOutput("Usage: unalias [name] (e.g., unalias ll)", OutputType.STDERR)
                }
            }
            "clear" -> {
                _outputs.value = emptyList()
            }
            "help" -> {
                showHelp()
            }
            "neofetch" -> {
                showNeofetch()
            }
            "theme" -> {
                if (parts.size > 1) {
                    val requestedTheme = parts[1].lowercase()
                    val match = TerminalTheme.values().find { t -> t.id == requestedTheme }
                    if (match != null) {
                        _currentTheme.value = match
                        addOutput("Theme updated to ${match.name}", OutputType.SUCCESS)
                    } else {
                        addOutput("Unknown theme. Available themes:", OutputType.STDERR)
                        showAvailableThemes()
                    }
                } else {
                    showAvailableThemes()
                }
            }
            "sysinfo" -> {
                showSysInfo()
            }
            "diagnostics" -> {
                showDiagnostics()
            }
            "apt" -> {
                handleAptCommand(parts)
            }
            "subfinder" -> {
                if (isPackageInstalled("subfinder")) {
                    if (parts.size > 1) {
                        runSubfinderCommand(parts[1])
                    } else {
                        addOutput("Usage: subfinder [domain] (e.g. subfinder google.com)", OutputType.STDERR)
                    }
                } else {
                    addOutput("bash: subfinder: command not found (You can install it with 'apt install subfinder')", OutputType.STDERR)
                }
            }
            "dnslookup" -> {
                if (isPackageInstalled("dnslookup")) {
                    if (parts.size > 1) {
                        val host = parts[1]
                        val type = if (parts.size > 2) parts[2].uppercase() else "A"
                        runDnsLookup(host, type)
                    } else {
                        addOutput("Usage: dnslookup [domain] [record_type] (e.g. dnslookup google.com MX)", OutputType.STDERR)
                        addOutput("Supported types: A, AAAA, MX, TXT, NS, CNAME", OutputType.INFO)
                    }
                } else {
                    addOutput("bash: dnslookup: command not found (You can install it with 'apt install dnslookup')", OutputType.STDERR)
                }
            }
            "whois" -> {
                if (isPackageInstalled("whois")) {
                    if (parts.size > 1) {
                        runWhoisCommand(parts[1])
                    } else {
                        addOutput("Usage: whois [domain] (e.g. whois google.com)", OutputType.STDERR)
                    }
                } else {
                    addOutput("bash: whois: command not found (You can install it with 'apt install whois')", OutputType.STDERR)
                }
            }
            "netcat", "nc" -> {
                if (isPackageInstalled("netcat")) {
                    if (parts.size > 2) {
                        val host = parts[1]
                        val portStr = parts[2]
                        runNetcatCommand(host, portStr)
                    } else {
                        addOutput("Usage: nc [host] [port] (e.g. nc example.com 80)", OutputType.STDERR)
                    }
                } else {
                    addOutput("bash: nc: command not found (You can install it with 'apt install netcat')", OutputType.STDERR)
                }
            }
            "ipinfo" -> {
                if (isPackageInstalled("ipinfo")) {
                    val ip = if (parts.size > 1) parts[1] else ""
                    runIpInfoCommand(ip)
                } else {
                    addOutput("bash: ipinfo: command not found (You can install it with 'apt install ipinfo')", OutputType.STDERR)
                }
            }
            "joke" -> {
                showRandomJoke()
            }
            "login" -> {
                viewModelScope.launch {
                    _triggerLogin.emit(Unit)
                }
            }
            "gemini" -> {
                if (parts.size > 1) {
                    val prompt = resolvedCommand.substringAfter("gemini").trim()
                    runGeminiCommand(prompt)
                } else {
                    addOutput("Usage: gemini [prompt]", OutputType.STDERR)
                }
            }
            "history" -> {
                showHistoryList()
            }
            "top" -> {
                runTopCommand()
            }
            "kill" -> {
                if (parts.size > 1) {
                    val pidStr = parts[1]
                    val pid = pidStr.toIntOrNull()
                    if (pid != null) {
                        val killed = killProcess(pid)
                        if (killed) {
                            addOutput("Process $pid terminated successfully.", OutputType.SUCCESS)
                        } else {
                            addOutput("kill: No such process with PID $pid", OutputType.STDERR)
                        }
                    } else {
                        addOutput("Usage: kill [PID]", OutputType.STDERR)
                    }
                } else {
                    addOutput("Usage: kill [PID]", OutputType.STDERR)
                }
            }
            "ssh" -> {
                // Parse ssh arguments
                // Usage: ssh [-i key_file] [-p port] [-pw password] username@host
                var keyPath: String? = null
                var portStr = "22"
                var passwordVal = ""
                var target: String? = null

                var i = 1
                while (i < parts.size) {
                    val part = parts[i]
                    when {
                        part == "-i" && i + 1 < parts.size -> {
                            keyPath = parts[i + 1]
                            i += 2
                        }
                        part == "-p" && i + 1 < parts.size -> {
                            portStr = parts[i + 1]
                            i += 2
                        }
                        part == "-pw" && i + 1 < parts.size -> {
                            passwordVal = parts[i + 1]
                            i += 2
                        }
                        part.startsWith("-") -> {
                            addOutput("ssh: warning: unknown option $part", OutputType.INFO)
                            i++
                        }
                        else -> {
                            target = part
                            i++
                        }
                    }
                }

                if (target == null) {
                    addOutput("Usage: ssh [-i key_file] [-p port] [-pw password] username@host", OutputType.STDERR)
                    addOutput("Example: ssh -i id_rsa -p 22 root@192.168.1.1", OutputType.INFO)
                } else {
                    val atIndex = target.indexOf('@')
                    if (atIndex == -1) {
                        addOutput("ssh: invalid target '$target'. Must be in format username@host", OutputType.STDERR)
                    } else {
                        val username = target.substring(0, atIndex)
                        val host = target.substring(atIndex + 1)
                        if (username.isEmpty() || host.isEmpty()) {
                            addOutput("ssh: invalid target '$target'. Username and host cannot be empty.", OutputType.STDERR)
                        } else {
                            var privateKeyContent = ""
                            var keyLoadedSuccessfully = true
                            if (keyPath != null) {
                                val file = if (keyPath.startsWith("/")) {
                                    java.io.File(keyPath)
                                } else {
                                    java.io.File(_currentDirectoryPath.value, keyPath)
                                }
                                if (file.exists() && file.isFile) {
                                    try {
                                        privateKeyContent = file.readText()
                                        addOutput("ssh: Loaded private key from ${file.name}", OutputType.INFO)
                                    } catch (e: Exception) {
                                        addOutput("ssh: Failed to read key file: ${e.localizedMessage}", OutputType.STDERR)
                                        keyLoadedSuccessfully = false
                                    }
                                } else {
                                    addOutput("ssh: Key file not found: $keyPath", OutputType.STDERR)
                                    keyLoadedSuccessfully = false
                                }
                            }

                            if (keyLoadedSuccessfully) {
                                connectSsh(host, username, portStr, passwordVal, privateKeyContent)
                            }
                        }
                    }
                }
            }
            // --- USER-REQUESTED NATIVE FALLBACK BUILT-IN COMMANDS ---
            "pwd" -> {
                addOutput(_currentDirectoryPath.value, OutputType.STDOUT)
            }
            "cd" -> {
                val targetDir = if (parts.size > 1) parts[1] else getApplication<Application>().filesDir.absolutePath
                changeDirectory(targetDir)
            }
            "ls" -> {
                listDirectoryContents()
            }
            "mkdir" -> {
                if (parts.size > 1) {
                    nativeMkdir(parts[1])
                } else {
                    addOutput("Usage: mkdir [directory_name]", OutputType.STDERR)
                }
            }
            "rm" -> {
                if (parts.size > 1) {
                    nativeRm(parts[1])
                } else {
                    addOutput("Usage: rm [file_or_directory]", OutputType.STDERR)
                }
            }
            "cat" -> {
                if (parts.size > 1) {
                    nativeCat(parts[1])
                } else {
                    addOutput("Usage: cat [file_name]", OutputType.STDERR)
                }
            }
            "whoami" -> {
                val currentUser = if (_isRoot.value) "root" else "guest"
                addOutput(currentUser, OutputType.STDOUT)
            }
            "sudo" -> {
                if (parts.size > 1 && parts[1].lowercase() == "su") {
                    _isRoot.value = true
                    addOutput("[sudo] password for guest: *******", OutputType.STDOUT)
                    addOutput("Successfully escalated privileges to root. Welcome to the superuser mode!", OutputType.SUCCESS)
                } else if (parts.size > 1) {
                    val subCmd = parts[1].lowercase()
                    addOutput("[sudo] executing '$subCmd' with root privileges...", OutputType.INFO)
                    _isRoot.value = true
                    executeCommand(parts.drop(1).joinToString(" "))
                    _isRoot.value = false
                } else {
                    addOutput("Usage: sudo su OR sudo [command]", OutputType.STDERR)
                }
            }
            "exit" -> {
                if (_isRoot.value) {
                    _isRoot.value = false
                    addOutput("Logged out from root. Returned to guest shell.", OutputType.INFO)
                } else {
                    addOutput("Type 'clear' to clear console or use SSH to connect elsewhere.", OutputType.INFO)
                }
            }
            "echo" -> {
                handleEcho(trimmedCommand, parts)
            }
            "touch" -> {
                if (parts.size > 1) {
                    val filename = parts[1]
                    val file = File(_currentDirectoryPath.value, filename)
                    try {
                        val created = file.createNewFile()
                        if (created) {
                            addOutput("touch: created empty file '$filename'", OutputType.SUCCESS)
                        } else {
                            file.setLastModified(System.currentTimeMillis())
                            addOutput("touch: updated timestamp for '$filename'", OutputType.INFO)
                        }
                    } catch (e: Exception) {
                        addOutput("touch: failed to touch '$filename': ${e.localizedMessage}", OutputType.STDERR)
                    }
                } else {
                    addOutput("Usage: touch [filename]", OutputType.STDERR)
                }
            }
            "uname" -> {
                val flag = parts.getOrNull(1)?.lowercase() ?: ""
                if (flag == "-r") {
                    addOutput("5.10.43-android12+", OutputType.STDOUT)
                } else {
                    addOutput("Linux terminaldroid 5.10.43-android12+ #1 SMP PREEMPT UTC 2026 aarch64 GNU/Linux", OutputType.STDOUT)
                }
            }
            "ping" -> {
                if (parts.size > 1) {
                    runPingSimulation(parts[1])
                } else {
                    addOutput("Usage: ping [host]", OutputType.STDERR)
                }
            }
            "curl" -> {
                if (parts.size > 1) {
                    runCurlCommand(parts[1])
                } else {
                    addOutput("Usage: curl [url] (e.g. curl https://api.ipify.org)", OutputType.STDERR)
                }
            }
            else -> {
                // Execute standard Android shell command in current folder
                runShellCommand(trimmedCommand)
            }
        }
    }

    private fun addOutput(text: String, type: OutputType) {
        _outputs.value = _outputs.value + OutputLine(text, type)
    }

    private fun showHelp() {
        addOutput("--- Native Built-in Terminal Commands ---", OutputType.INFO)
        addOutput("ls               List directory contents beautifully", OutputType.STDOUT)
        addOutput("cd [path]        Change directory inside filesDir", OutputType.STDOUT)
        addOutput("pwd              Print current working directory path", OutputType.STDOUT)
        addOutput("mkdir [dir]      Create a new subdirectory", OutputType.STDOUT)
        addOutput("rm [target]      Remove a file or directory recursively", OutputType.STDOUT)
        addOutput("cat [file]       Tampilkan isi file", OutputType.STDOUT)
        addOutput("touch [file]     Create empty file or update timestamp", OutputType.STDOUT)
        addOutput("echo [msg]       Print text (supports > and >> redirection)", OutputType.STDOUT)
        addOutput("whoami           Print current user session", OutputType.STDOUT)
        addOutput("sudo su          Escalate privilege to superuser (root)", OutputType.STDOUT)
        addOutput("uname            Print system kernel architecture details", OutputType.STDOUT)
        addOutput("ping [host]      Emulate a network latency probe", OutputType.STDOUT)
        addOutput("curl [url]       Fetch real URL contents (e.g. curl https://api.ipify.org)", OutputType.STDOUT)
        addOutput("clear            Clear the screen console", OutputType.STDOUT)
        addOutput("neofetch         Display gorgeous system info with ASCII logo", OutputType.STDOUT)
        addOutput("sysinfo          Print complete hardware and storage details", OutputType.STDOUT)
        addOutput("diagnostics      Display real-time CPU, RAM, and Battery stats", OutputType.STDOUT)
        addOutput("login            Authenticate with Google to access Drive/Gmail", OutputType.STDOUT)
        addOutput("gemini [prompt]  Prompt the Gemini AI model directly via REST API", OutputType.STDOUT)
        addOutput("top              Display running background processes and resource usage", OutputType.STDOUT)
        addOutput("kill [PID]       Terminate a specific process by its PID", OutputType.STDOUT)
        addOutput("apt [cmd]        APT Package Manager (update, upgrade, install, remove, list)", OutputType.STDOUT)
        addOutput("history          List previously executed commands", OutputType.STDOUT)
        addOutput("theme [name]     Change theme (matrix, amber, dracula, cyberpunk, classic)", OutputType.STDOUT)
        addOutput("alias            List, view, or create custom command aliases (e.g., alias ll=\"ls -la\")", OutputType.STDOUT)
        addOutput("unalias [name]   Remove a custom command alias", OutputType.STDOUT)
        addOutput("", OutputType.STDOUT)
        addOutput("--- Installed Sandbox Pentest & Recon Tools ---", OutputType.INFO)
        addOutput("subfinder [dom]  Passive subdomain discovery via Certificate Transparency logs", OutputType.STDOUT)
        addOutput("dnslookup [dom]  DNS record query client (supports A, MX, TXT, CNAME, etc.)", OutputType.STDOUT)
        addOutput("whois [dom]      Retrieve registry & domain ownership WHOIS information", OutputType.STDOUT)
        addOutput("nc [host] [port] TCP network port probe (alias: netcat)", OutputType.STDOUT)
        addOutput("ipinfo [ip]      Query public IP address intelligence & geolocation", OutputType.STDOUT)
        addOutput("", OutputType.STDOUT)
        addOutput("--- SSH Client Command ---", OutputType.INFO)
        addOutput("ssh [-i key] [-p port] [-pw pass] user@host", OutputType.STDOUT)
        addOutput("You can also connect to external servers using the 'SSH Quick Connect' dialog in the header.", OutputType.STDOUT)
        addOutput("", OutputType.STDOUT)
    }

    private fun showAvailableThemes() {
        addOutput("Available Themes:", OutputType.INFO)
        TerminalTheme.values().forEach {
            addOutput("  - ${it.id} (${it.name})", OutputType.STDOUT)
        }
        addOutput("Usage: theme [name] (e.g. theme cyberpunk)", OutputType.INFO)
        addOutput("", OutputType.STDOUT)
    }

    fun setThemeById(themeId: String) {
        val match = TerminalTheme.values().find { it.id == themeId }
        if (match != null) {
            _currentTheme.value = match
        }
    }

    // --- NATIVE FILE SYSTEM COMMAND IMPLEMENTATIONS ---

    private fun changeDirectory(target: String) {
        try {
            val file = if (target.startsWith("/")) {
                File(target).canonicalFile
            } else {
                File(_currentDirectoryPath.value, target).canonicalFile
            }

            if (file.exists()) {
                if (file.isDirectory) {
                    _currentDirectoryPath.value = file.absolutePath
                    addOutput("Directory changed: ${getCompactPath(file.absolutePath)}", OutputType.SUCCESS)
                } else {
                    addOutput("cd: not a directory: $target", OutputType.STDERR)
                }
            } else {
                addOutput("cd: no such directory: $target", OutputType.STDERR)
            }
        } catch (e: Exception) {
            addOutput("cd: error resolving path: ${e.message}", OutputType.STDERR)
        }
    }

    private fun listDirectoryContents() {
        try {
            val dir = File(_currentDirectoryPath.value)
            val files = dir.listFiles()
            if (files == null || files.isEmpty()) {
                addOutput("(directory is empty)", OutputType.INFO)
                return
            }
            // Sort folders first, then files
            val sorted = files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
            addOutput(String.format("%-12s   %s", "Size", "Name"), OutputType.INFO)
            addOutput("--------------------------------------------", OutputType.INFO)
            sorted.forEach { file ->
                val sizeStr = if (file.isDirectory) "<DIR>" else "${file.length()} B"
                val name = if (file.isDirectory) "${file.name}/" else file.name
                val colorType = if (file.isDirectory) OutputType.SUCCESS else OutputType.STDOUT
                addOutput(String.format("%-12s   %s", sizeStr, name), colorType)
            }
        } catch (e: Exception) {
            addOutput("ls: failed to list directory contents: ${e.localizedMessage}", OutputType.STDERR)
        }
    }

    private fun nativeMkdir(dirName: String) {
        try {
            val targetDir = File(_currentDirectoryPath.value, dirName)
            if (targetDir.exists()) {
                addOutput("mkdir: cannot create directory '$dirName': File exists", OutputType.STDERR)
                return
            }
            val success = targetDir.mkdirs()
            if (success) {
                addOutput("Directory created successfully: $dirName", OutputType.SUCCESS)
            } else {
                addOutput("mkdir: failed to create directory '$dirName'", OutputType.STDERR)
            }
        } catch (e: Exception) {
            addOutput("mkdir: error: ${e.localizedMessage}", OutputType.STDERR)
        }
    }

    private fun nativeRm(targetName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val target = File(_currentDirectoryPath.value, targetName)
                if (!target.exists()) {
                    withContext(Dispatchers.Main) {
                        addOutput("rm: cannot remove '$targetName': No such file or directory", OutputType.STDERR)
                    }
                    return@launch
                }
                val deleted = if (target.isDirectory) {
                    target.deleteRecursively()
                } else {
                    target.delete()
                }
                withContext(Dispatchers.Main) {
                    if (deleted) {
                        addOutput("Removed successfully: $targetName", OutputType.SUCCESS)
                    } else {
                        addOutput("rm: failed to remove '$targetName'", OutputType.STDERR)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addOutput("rm: error: ${e.localizedMessage}", OutputType.STDERR)
                }
            }
        }
    }

    private fun nativeCat(fileName: String) {
        try {
            val target = File(_currentDirectoryPath.value, fileName)
            if (!target.exists()) {
                addOutput("cat: $fileName: No such file or directory", OutputType.STDERR)
                return
            }
            if (target.isDirectory) {
                addOutput("cat: $fileName: Is a directory", OutputType.STDERR)
                return
            }
            target.forEachLine { line ->
                addOutput(line, OutputType.STDOUT)
            }
        } catch (e: Exception) {
            addOutput("cat: error reading file: ${e.localizedMessage}", OutputType.STDERR)
        }
    }

    // --- SHELL COMMAND EXECUTION FLOW ---

    private fun runShellCommand(command: String) {
        _isRunning.value = true
        activeJob?.cancel()
        var pid = -1
        activeJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentFolder = File(_currentDirectoryPath.value)
                val process = ProcessBuilder()
                    .command("/system/bin/sh", "-c", command)
                    .directory(currentFolder)
                    .start()

                activeProcess = process
                pid = registerProcess(command, coroutineContext[Job], process)

                // Stream stdout line-by-line
                val outReaderJob = launch {
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line ?: break
                        withContext(Dispatchers.Main) {
                            addOutput(currentLine, OutputType.STDOUT)
                        }
                    }
                }

                // Stream stderr line-by-line
                val errReaderJob = launch {
                    val reader = BufferedReader(InputStreamReader(process.errorStream))
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val currentLine = line ?: break
                        withContext(Dispatchers.Main) {
                            addOutput(currentLine, OutputType.STDERR)
                        }
                    }
                }

                // Wait for process completion
                val exitCode = process.waitFor()
                outReaderJob.join()
                errReaderJob.join()

                withContext(Dispatchers.Main) {
                    if (exitCode != 0) {
                        addOutput("Process exited with code $exitCode", OutputType.STDERR)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addOutput("Shell execution failed: ${e.localizedMessage}", OutputType.STDERR)
                }
            } finally {
                completeProcess(pid)
                withContext(Dispatchers.Main) {
                    activeProcess = null
                    _isRunning.value = false
                }
            }
        }
    }

    fun interruptActiveProcess() {
        if (_isSshActive.value) {
            // Send interrupt character block to remote SSH
            sendSshCommand("\u0003")
            addOutput("^C (Sent SSH Interrupt)", OutputType.STDERR)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            activeProcess?.destroy()
            activeJob?.cancel()
            withContext(Dispatchers.Main) {
                addOutput("^C (Interrupted process execution)", OutputType.STDERR)
                _isRunning.value = false
                activeProcess = null
            }
        }
    }

    // --- SESSIONS MANAGEMENT API ---

    fun saveCurrentSession(name: String) {
        viewModelScope.launch {
            try {
                val currentFolder = _currentDirectoryPath.value
                val currentHistory = _commandHistoryList.value
                repository.saveSession(
                    name = name.ifBlank { "Session ${System.currentTimeMillis() % 10000}" },
                    workingDir = currentFolder,
                    commandHistoryList = currentHistory
                )
                addOutput("Session '$name' saved successfully!", OutputType.SUCCESS)
            } catch (e: Exception) {
                addOutput("Failed to save session: ${e.localizedMessage}", OutputType.STDERR)
            }
        }
    }

    fun loadSession(session: TerminalSession) {
        viewModelScope.launch {
            try {
                _currentDirectoryPath.value = session.workingDirectory
                val sessionHistoryList = repository.getSessionHistory(session.id)
                _commandHistoryList.value = sessionHistoryList

                addOutput("==========================================", OutputType.INFO)
                addOutput("Loaded Session: ${session.name}", OutputType.SUCCESS)
                addOutput("Restored Working Directory: ${getCompactPath(session.workingDirectory)}", OutputType.INFO)
                addOutput("Restored ${sessionHistoryList.size} command history logs.", OutputType.INFO)
                addOutput("==========================================", OutputType.INFO)
            } catch (e: Exception) {
                addOutput("Failed to load session history: ${e.localizedMessage}", OutputType.STDERR)
            }
        }
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            addOutput("Deleted session log.", OutputType.INFO)
        }
    }

    // --- SSH CLIENT BIDIRECTIONAL SHELL ---

    fun connectSsh(
        host: String,
        username: String,
        portStr: String,
        passwordVal: String,
        privateKeyContent: String
    ) {
        _isSshConnecting.value = true
        _isSshActive.value = false
        addOutput("Connecting to SSH server: $username@$host:$portStr...", OutputType.INFO)

        val portNum = portStr.toIntOrNull() ?: 22

        var pid = -1
        viewModelScope.launch(Dispatchers.IO) {
            pid = registerProcess("ssh $username@$host", coroutineContext[Job])
            try {
                val jsch = JSch()
                if (privateKeyContent.trim().isNotEmpty()) {
                    jsch.addIdentity("ssh_key", privateKeyContent.toByteArray(), null, null)
                    addOutput("[SSH] Added Private Key Identity.", OutputType.INFO)
                }

                val session = jsch.getSession(username, host, portNum)
                if (passwordVal.isNotEmpty() && privateKeyContent.isEmpty()) {
                    session.setPassword(passwordVal)
                }

                // Skip host key verification for easy connection
                session.setConfig("StrictHostKeyChecking", "no")
                session.connect(15000) // 15 seconds timeout

                val channel = session.openChannel("shell") as ChannelShell
                channel.connect()

                jschSession = session
                jschChannel = channel
                channelInput = PrintStream(channel.outputStream, true)

                _sshHost.value = host
                _sshUsername.value = username

                withContext(Dispatchers.Main) {
                    _isSshActive.value = true
                    _isSshConnecting.value = false
                    addOutput("Connected successfully to SSH host: $host", OutputType.SUCCESS)
                    addOutput("Type commands below. Type 'exit' to terminate SSH connection.", OutputType.INFO)
                }

                // Start reader thread
                sshReaderJob = launch(Dispatchers.IO) {
                    val reader = BufferedReader(InputStreamReader(channel.inputStream))
                    var line: String?
                    while (_isSshActive.value) {
                        line = reader.readLine()
                        if (line == null) break
                        withContext(Dispatchers.Main) {
                            addOutput(line!!, OutputType.STDOUT)
                        }
                    }
                    // Remote exited
                    withContext(Dispatchers.Main) {
                        disconnectSsh()
                    }
                }

            } catch (e: Exception) {
                completeProcess(pid, false)
                withContext(Dispatchers.Main) {
                    _isSshConnecting.value = false
                    _isSshActive.value = false
                    addOutput("SSH Connection Failed: ${e.localizedMessage}", OutputType.STDERR)
                }
            }
        }
    }

    private fun sendSshCommand(cmd: String) {
        if (cmd.lowercase().trim() == "exit") {
            disconnectSsh()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                channelInput?.println(cmd)
                channelInput?.flush()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addOutput("Failed to send SSH command: ${e.localizedMessage}", OutputType.STDERR)
                }
            }
        }
    }

    fun disconnectSsh() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                channelInput?.close()
                jschChannel?.disconnect()
                jschSession?.disconnect()
                sshReaderJob?.cancel()
            } catch (e: Exception) {
                // Ignore
            } finally {
                withContext(Dispatchers.Main) {
                    _isSshActive.value = false
                    _isSshConnecting.value = false
                    addOutput("SSH Session Disconnected.", OutputType.INFO)
                }
                _processes.value = _processes.value.map { proc ->
                    if (proc.name.startsWith("ssh ") && proc.status == "RUNNING") {
                        proc.copy(status = "COMPLETED", cpu = 0.0, memory = "0.0 MB")
                    } else {
                        proc
                    }
                }
            }
        }
    }

    // --- NAVIGATION HISTORY & AUTOCOMPLETE ---

    fun navigateHistoryUp() {
        val history = _commandHistoryList.value
        if (history.isEmpty()) return

        if (historyIndex.value == -1) {
            savedInputText = inputText.value
        }

        val nextIndex = historyIndex.value + 1
        if (nextIndex < history.size) {
            historyIndex.value = nextIndex
            inputText.value = history[nextIndex]
        }
    }

    fun navigateHistoryDown() {
        val history = _commandHistoryList.value
        if (history.isEmpty() || historyIndex.value == -1) return

        val nextIndex = historyIndex.value - 1
        if (nextIndex >= 0) {
            historyIndex.value = nextIndex
            inputText.value = history[nextIndex]
        } else {
            historyIndex.value = -1
            inputText.value = savedInputText
        }
    }

    fun handleTabAutocomplete() {
        val currentInput = inputText.value
        if (currentInput.isEmpty()) return

        val parts = currentInput.trimEnd().split("\\s+".toRegex())
        if (parts.isEmpty()) return

        val lastPart = parts.last()
        val currentDir = File(_currentDirectoryPath.value)
        val files = currentDir.listFiles() ?: return

        val matches = files.filter { it.name.startsWith(lastPart, ignoreCase = true) }

        if (matches.size == 1) {
            val matchedFile = matches[0]
            val suffix = if (matchedFile.isDirectory) "/" else ""
            val completedText = matchedFile.name + suffix

            val previousParts = parts.dropLast(1)
            val completedInput = if (previousParts.isEmpty()) {
                completedText
            } else {
                previousParts.joinToString(" ") + " " + completedText
            }
            inputText.value = completedInput
        } else if (matches.size > 1) {
            addOutput("> Matches: " + matches.joinToString("  ") { if (it.isDirectory) "${it.name}/" else it.name }, OutputType.INFO)
        }
    }

    private fun getCompactPath(fullPath: String): String {
        val filesDir = getApplication<Application>().filesDir.absolutePath
        return if (fullPath == filesDir) {
            "~"
        } else if (fullPath.startsWith(filesDir)) {
            "~" + fullPath.substring(filesDir.length)
        } else {
            fullPath
        }
    }

    // Helper functions
    private fun showNeofetch() {
        val app = getApplication<Application>()
        val stat = StatFs(app.filesDir.path)
        val blockSize = stat.blockSizeLong
        val availableBlocks = stat.availableBlocksLong
        val totalBlocks = stat.blockCountLong
        val freeSpaceGb = (availableBlocks * blockSize) / (1024f * 1024f * 1024f)
        val totalSpaceGb = (totalBlocks * blockSize) / (1024f * 1024f * 1024f)

        val activityManager = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        val availRamGb = memoryInfo.availMem / (1024f * 1024f * 1024f)
        val totalRamGb = memoryInfo.totalMem / (1024f * 1024f * 1024f)

        val uptimeSec = SystemClock.elapsedRealtime() / 1000
        val uptimeHours = uptimeSec / 3600
        val uptimeMins = (uptimeSec % 3600) / 60
        val uptimeFormatted = if (uptimeHours > 0) "${uptimeHours}h ${uptimeMins}m" else "${uptimeMins}m"

        val asciiLogo = listOf(
            "     .-.   .-.      ",
            "    \\   \\ /   /     ",
            "     \\   v   /      ",
            "    .-''''''-.      ",
            "  .'          '.    ",
            " /   O      O   \\   ",
            "|                |  ",
            "|''''''''''''''''|  ",
            "|      _.._      |  ",
            " \\    '----'    /   ",
            "  '.          .'    ",
            "    '-......-'      "
        )

        val systemInfo = listOf(
            "guest@terminaldroid",
            "-------------------",
            "OS: Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
            "Device: ${Build.MANUFACTURER} ${Build.MODEL}",
            "Kernel: ${System.getProperty("os.version") ?: "Linux"}",
            "Processor: ${Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"}",
            "Uptime: $uptimeFormatted",
            "Memory: ${String.format("%.2f", totalRamGb - availRamGb)}GB / ${String.format("%.2f", totalRamGb)}GB",
            "Storage: ${String.format("%.2f", totalSpaceGb - freeSpaceGb)}GB / ${String.format("%.2f", totalSpaceGb)}GB",
            "Shell: /system/bin/sh (Sandboxed)",
            "Theme: ${currentTheme.value.themeName}"
        )

        val maxLines = maxOf(asciiLogo.size, systemInfo.size)
        for (i in 0 until maxLines) {
            val logoPart = asciiLogo.getOrNull(i) ?: "                    "
            val infoPart = systemInfo.getOrNull(i) ?: ""
            addOutput("$logoPart  $infoPart", OutputType.ASCII_ART)
        }
        addOutput("", OutputType.STDOUT)
    }

    private fun showSysInfo() {
        addOutput("--- Hardware & System Diagnostics ---", OutputType.INFO)
        addOutput("Manufacturer:  ${Build.MANUFACTURER}", OutputType.STDOUT)
        addOutput("Model:         ${Build.MODEL}", OutputType.STDOUT)
        addOutput("Board:         ${Build.BOARD}", OutputType.STDOUT)
        addOutput("Hardware:      ${Build.HARDWARE}", OutputType.STDOUT)
        addOutput("Device:        ${Build.DEVICE}", OutputType.STDOUT)
        addOutput("Product:       ${Build.PRODUCT}", OutputType.STDOUT)
        addOutput("CPU ABI:       ${Build.SUPPORTED_ABIS.joinToString(", ")}", OutputType.STDOUT)
        addOutput("Display ID:    ${Build.DISPLAY}", OutputType.STDOUT)
        addOutput("Fingerprint:   ${Build.FINGERPRINT}", OutputType.STDOUT)
        addOutput("Android SDK:   ${Build.VERSION.SDK_INT}", OutputType.STDOUT)
        addOutput("Security Patch: ${Build.VERSION.SECURITY_PATCH}", OutputType.STDOUT)
        addOutput("Bootloader:    ${Build.BOOTLOADER}", OutputType.STDOUT)
        addOutput("", OutputType.STDOUT)

        // Trigger real-time visual diagnostic module
        _showSysInfoBlock.value = true
        addOutput("[!] REAL-TIME SYSTEM DIAGNOSTICS MODULE INITIATED [!]", OutputType.SUCCESS)
    }

    private fun runGeminiCommand(prompt: String) {
        val pid = registerProcess("gemini $prompt")
        addOutput("Gemini AI Processing: '$prompt'", OutputType.INFO)
        
        viewModelScope.launch {
            try {
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    addOutput("Gemini API key is missing. Please set GEMINI_API_KEY in the Secrets panel.", OutputType.STDERR)
                    completeProcess(pid, false)
                    return@launch
                }

                val request = com.example.data.GenerateContentRequest(
                    contents = listOf(
                        com.example.data.Content(
                            parts = listOf(com.example.data.Part(text = prompt))
                        )
                    )
                )
                
                val response = com.example.data.RetrofitClient.service.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "No response from Gemini."
                
                val lines = responseText.split("\n")
                lines.forEach { line ->
                    addOutput(line, OutputType.STDOUT)
                }
                
                completeProcess(pid, true)
            } catch (e: Exception) {
                addOutput("Gemini CLI Error: ${e.message}", OutputType.STDERR)
                completeProcess(pid, false)
            }
        }
    }

    private fun showRandomJoke() {
        val jokes = listOf(
            "There are 10 types of people in the world: those who understand binary, and those who don't.",
            "Why do programmers wear glasses? Because they can't C#.",
            "A SQL query goes into a bar, walks up to two tables and asks, 'Can I join you?'",
            "What is a programmer's favorite hangout place? Foo Bar.",
            "How many programmers does it take to change a light bulb? None, that's a hardware problem.",
            "Why did the programmer quit his job? Because he didn't get arrays.",
            "['hip', 'hip'] (hip hip array!)",
            "An optimist says the glass is half full. A pessimist says the glass is half empty. A programmer says the glass is twice as large as it needs to be.",
            "A programmer's wife tells him, 'Go to the store and buy a loaf of bread. If they have eggs, buy a dozen.' He comes home with 12 loaves of bread."
        )
        addOutput(jokes.random(), OutputType.SUCCESS)
        addOutput("", OutputType.STDOUT)
    }

    private fun showHistoryList() {
        val history = _commandHistoryList.value
        if (history.isEmpty()) {
            addOutput("No commands in history yet.", OutputType.INFO)
            return
        }
        addOutput("--- Recent Command History ---", OutputType.INFO)
        history.reversed().take(20).forEachIndexed { index, cmd ->
            addOutput("  ${index + 1}  $cmd", OutputType.STDOUT)
        }
        addOutput("", OutputType.STDOUT)
    }

    private fun runTopCommand() {
        addOutput("--- TerminalDroid Process Monitor (top) ---", OutputType.INFO)
        addOutput(String.format("%-6s  %-25s  %-10s  %-6s  %-10s  %-8s", "PID", "COMMAND", "STATUS", "CPU%", "MEMORY", "TIME"), OutputType.INFO)
        addOutput("-".repeat(70), OutputType.STDOUT)
        
        val currentProcs = _processes.value
        if (currentProcs.isEmpty()) {
            addOutput("No active processes.", OutputType.STDOUT)
            return
        }
        
        currentProcs.forEach { proc ->
            val elapsedSec = (System.currentTimeMillis() - proc.startTime) / 1000
            val timeStr = if (proc.status == "RUNNING") "${elapsedSec}s" else "-"
            
            // Fluctuating CPU usage slightly if RUNNING for realistic simulation
            val cpuVal = if (proc.status == "RUNNING" && !proc.isSystem) {
                (0.1 + Math.random() * 3.0).let { Math.round(it * 10) / 10.0 }
            } else {
                proc.cpu
            }
            
            val type = if (proc.status == "RUNNING") OutputType.SUCCESS else OutputType.STDOUT
            addOutput(
                String.format(
                    "%-6d  %-25s  %-10s  %-6.1f  %-10s  %-8s",
                    proc.pid,
                    if (proc.name.length > 25) proc.name.take(22) + "..." else proc.name,
                    proc.status,
                    cpuVal,
                    proc.memory,
                    timeStr
                ),
                type
            )
        }
        addOutput("", OutputType.STDOUT)
        addOutput("Use 'kill <PID>' to terminate a specific background/active process.", OutputType.INFO)
        addOutput("You can also view the Process Monitor by clicking the Task Manager icon in the header.", OutputType.INFO)
    }

    private fun startDiagnosticsMonitoring() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                try {
                    val app = getApplication<Application>()
                    
                    // CPU Usage
                    val cpu = getCpuUsage()
                    val processors = Runtime.getRuntime().availableProcessors()
                    val jvmThreads = Thread.activeCount()
                    
                    // RAM
                    val activityManager = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    val memoryInfo = ActivityManager.MemoryInfo()
                    activityManager.getMemoryInfo(memoryInfo)
                    val totalRamGb = memoryInfo.totalMem / (1024f * 1024f * 1024f)
                    val availRamGb = memoryInfo.availMem / (1024f * 1024f * 1024f)
                    val usedRamGb = totalRamGb - availRamGb
                    val ramPct = if (totalRamGb > 0) (usedRamGb / totalRamGb) * 100.0 else 0.0
                    
                    // JVM Heap
                    val jvmUsedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024f * 1024f)
                    val jvmMaxMb = Runtime.getRuntime().maxMemory() / (1024f * 1024f)
                    
                    // Battery
                    val batteryStatusIntent = app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    val batteryLevel = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val batteryScale = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                    val batteryPct = if (batteryLevel != -1 && batteryScale != -1) (batteryLevel * 100f / batteryScale) else 100f
                    
                    val batteryStatus = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                    val statusStr = when (batteryStatus) {
                        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                        BatteryManager.BATTERY_STATUS_FULL -> "Full"
                        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                        else -> "Discharging"
                    }
                    val batteryTempRaw = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
                    val batteryTempCelsius = batteryTempRaw / 10f
                    val batteryVoltageRaw = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
                    val batteryVoltageVolts = batteryVoltageRaw / 1000f
                    
                    // Storage
                    val stat = StatFs(app.filesDir.path)
                    val blockSize = stat.blockSizeLong
                    val availableBlocks = stat.availableBlocksLong
                    val totalBlocks = stat.blockCountLong
                    val freeSpaceGb = (availableBlocks * blockSize) / (1024f * 1024f * 1024f)
                    val totalSpaceGb = (totalBlocks * blockSize) / (1024f * 1024f * 1024f)
                    val usedSpaceGb = totalSpaceGb - freeSpaceGb
                    val storagePct = if (totalSpaceGb > 0) (usedSpaceGb / totalSpaceGb) * 100.0 else 0.0
                    
                    // Uptime
                    val uptimeSec = SystemClock.elapsedRealtime() / 1000
                    val uptimeHours = uptimeSec / 3600
                    val uptimeMins = (uptimeSec % 3600) / 60
                    val uptimeSecs = uptimeSec % 60
                    val uptimeFormatted = if (uptimeHours > 0) "${uptimeHours}h ${uptimeMins}m ${uptimeSecs}s" else "${uptimeMins}m ${uptimeSecs}s"
                    
                    _systemDiagnostics.value = SystemDiagnosticsData(
                        cpuUsage = cpu,
                        activeCores = processors,
                        jvmThreads = jvmThreads,
                        usedRamGb = usedRamGb.toDouble(),
                        totalRamGb = totalRamGb.toDouble(),
                        ramPercentage = ramPct,
                        jvmHeapUsedMb = jvmUsedMb.toDouble(),
                        jvmHeapMaxMb = jvmMaxMb.toDouble(),
                        batteryPercentage = batteryPct.toDouble(),
                        batteryStatus = statusStr,
                        batteryTemperature = batteryTempCelsius,
                        batteryVoltage = batteryVoltageVolts,
                        usedStorageGb = usedSpaceGb.toDouble(),
                        totalStorageGb = totalSpaceGb.toDouble(),
                        storagePercentage = storagePct,
                        uptimeFormatted = uptimeFormatted
                    )
                } catch (e: Exception) {
                    // Ignore transient errors
                }
                kotlinx.coroutines.delay(2000)
            }
        }
    }

    private fun showDiagnostics() {
        _isRunning.value = true
        viewModelScope.launch {
            try {
                addOutput("Gathering real-time system telemetry...", OutputType.INFO)
                
                // Get CPU usage & core count
                val cpuUsage = getCpuUsage()
                val processors = Runtime.getRuntime().availableProcessors()
                
                // Get Memory details
                val app = getApplication<Application>()
                val activityManager = app.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val memoryInfo = ActivityManager.MemoryInfo()
                activityManager.getMemoryInfo(memoryInfo)
                val totalRamGb = memoryInfo.totalMem / (1024f * 1024f * 1024f)
                val availRamGb = memoryInfo.availMem / (1024f * 1024f * 1024f)
                val usedRamGb = totalRamGb - availRamGb
                val ramPercentage = (usedRamGb / totalRamGb) * 100.0
                
                // Get Battery details
                val batteryStatusIntent = app.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val batteryLevel = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val batteryScale = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val batteryPct = if (batteryLevel != -1 && batteryScale != -1) (batteryLevel * 100f / batteryScale) else 100f
                
                val batteryStatus = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isCharging = batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING || batteryStatus == BatteryManager.BATTERY_STATUS_FULL
                val statusStr = when (batteryStatus) {
                    BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
                    BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
                    BatteryManager.BATTERY_STATUS_FULL -> "Full"
                    BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
                    else -> "Unknown"
                }
                
                val batteryTempRaw = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
                val batteryTempCelsius = batteryTempRaw / 10f
                val batteryVoltageRaw = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
                val batteryVoltageVolts = batteryVoltageRaw / 1000f

                // Draw retro style progress bars
                val cpuBar = drawProgressBar(cpuUsage, 20)
                val ramBar = drawProgressBar(ramPercentage, 20)
                val batBar = drawProgressBar(batteryPct.toDouble(), 20)

                withContext(Dispatchers.Main) {
                    addOutput("==========================================", OutputType.INFO)
                    addOutput("          REAL-TIME SYSTEM DIAGNOSTICS     ", OutputType.SUCCESS)
                    addOutput("==========================================", OutputType.INFO)
                    
                    addOutput(String.format("CPU Usage:    [%-20s] %.1f%%", cpuBar, cpuUsage), OutputType.STDOUT)
                    addOutput("  - Cores:    $processors active hardware cores", OutputType.STDOUT)
                    addOutput("  - JVM Thrd: ${Thread.activeCount()} active threads", OutputType.STDOUT)
                    addOutput("", OutputType.STDOUT)
                    
                    addOutput(String.format("Memory:       [%-20s] %.1f%%", ramBar, ramPercentage), OutputType.STDOUT)
                    addOutput(String.format("  - Avail:    %.2f GB / %.2f GB", availRamGb, totalRamGb), OutputType.STDOUT)
                    addOutput(String.format("  - JVM Heap: %.1f MB / %.1f MB", 
                        (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / (1024f * 1024f), 
                        Runtime.getRuntime().maxMemory() / (1024f * 1024f)
                    ), OutputType.STDOUT)
                    addOutput("", OutputType.STDOUT)
                    
                    addOutput(String.format("Battery:      [%-20s] %.1f%%", batBar, batteryPct), OutputType.STDOUT)
                    addOutput("  - Status:   $statusStr" + (if (isCharging) " (Plugged)" else ""), OutputType.STDOUT)
                    addOutput(String.format("  - Temp:     %.1f °C", batteryTempCelsius), OutputType.STDOUT)
                    addOutput(String.format("  - Voltage:  %.3f V", batteryVoltageVolts), OutputType.STDOUT)
                    
                    addOutput("==========================================", OutputType.INFO)
                    addOutput("", OutputType.STDOUT)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addOutput("Diagnostics failed: ${e.localizedMessage}", OutputType.STDERR)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isRunning.value = false
                }
            }
        }
    }

    private suspend fun getCpuUsage(): Double = withContext(Dispatchers.IO) {
        try {
            val statFile = File("/proc/stat")
            if (statFile.exists() && statFile.canRead()) {
                val reader1 = BufferedReader(FileReader(statFile))
                val line1 = reader1.readLine()
                reader1.close()
                if (line1 != null && line1.startsWith("cpu ")) {
                    val parts1 = line1.trim().split("\\s+".toRegex())
                    val user1 = parts1[1].toLong()
                    val nice1 = parts1[2].toLong()
                    val system1 = parts1[3].toLong()
                    val idle1 = parts1[4].toLong()
                    val iowait1 = parts1.getOrNull(5)?.toLong() ?: 0L
                    val irq1 = parts1.getOrNull(6)?.toLong() ?: 0L
                    val softirq1 = parts1.getOrNull(7)?.toLong() ?: 0L
                    
                    val active1 = user1 + nice1 + system1 + irq1 + softirq1
                    val total1 = active1 + idle1 + iowait1

                    kotlinx.coroutines.delay(100)

                    val reader2 = BufferedReader(FileReader(statFile))
                    val line2 = reader2.readLine()
                    reader2.close()
                    if (line2 != null && line2.startsWith("cpu ")) {
                        val parts2 = line2.trim().split("\\s+".toRegex())
                        val user2 = parts2[1].toLong()
                        val nice2 = parts2[2].toLong()
                        val system2 = parts2[3].toLong()
                        val idle2 = parts2[4].toLong()
                        val iowait2 = parts2.getOrNull(5)?.toLong() ?: 0L
                        val irq2 = parts2.getOrNull(6)?.toLong() ?: 0L
                        val softirq2 = parts2.getOrNull(7)?.toLong() ?: 0L
                        
                        val active2 = user2 + nice2 + system2 + irq2 + softirq2
                        val total2 = active2 + idle2 + iowait2

                        val deltaActive = active2 - active1
                        val deltaTotal = total2 - total1
                        if (deltaTotal > 0) {
                            return@withContext (deltaActive.toDouble() / deltaTotal.toDouble()) * 100.0
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore & fallback
        }

        try {
            var totalMax = 0L
            var totalCur = 0L
            val cores = Runtime.getRuntime().availableProcessors()
            for (i in 0 until cores) {
                val maxFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                val curFile = File("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
                if (maxFile.exists() && curFile.exists()) {
                    val max = maxFile.readText().trim().toLongOrNull() ?: 0L
                    val cur = curFile.readText().trim().toLongOrNull() ?: 0L
                    if (max > 0) {
                        totalMax += max
                        totalCur += cur
                    }
                }
            }
            if (totalMax > 0) {
                return@withContext (totalCur.toDouble() / totalMax.toDouble()) * 100.0
            }
        } catch (e: Exception) {
            // Fallback
        }

        val activeThreads = Thread.activeCount().toDouble()
        val coreCount = Runtime.getRuntime().availableProcessors()
        val loadEstimate = (activeThreads / (coreCount * 12.0)) * 100.0
        return@withContext loadEstimate.coerceIn(4.0, 96.0)
    }

    private fun drawProgressBar(percentage: Double, size: Int): String {
        val filledCount = ((percentage.coerceIn(0.0, 100.0) / 100.0) * size).toInt()
        val emptyCount = size - filledCount
        return "|".repeat(filledCount) + ".".repeat(emptyCount)
    }

    private fun isPackageInstalled(pkg: String): Boolean {
        return _installedPackages.value.contains(pkg)
    }

    private fun handleAptCommand(parts: List<String>) {
        if (parts.size < 2) {
            addOutput("APT Package Manager v2.1.0 (Sandboxed Android Edition)", OutputType.INFO)
            addOutput("Usage: apt [command] [package]", OutputType.STDOUT)
            addOutput("Commands:", OutputType.STDOUT)
            addOutput("  update      Update package index lists from repository", OutputType.STDOUT)
            addOutput("  upgrade     Upgrade installed pentest/recon tools", OutputType.STDOUT)
            addOutput("  install     Install custom tools (subfinder, dnslookup, whois, netcat, ipinfo)", OutputType.STDOUT)
            addOutput("  remove      Uninstall a custom tool from sandbox", OutputType.STDOUT)
            addOutput("  list        List all sandbox packages & install status", OutputType.STDOUT)
            return
        }

        val subCmd = parts[1].lowercase()
        when (subCmd) {
            "update" -> {
                _isRunning.value = true
                viewModelScope.launch {
                    try {
                        addOutput("Hit:1 https://deb.debian.org/debian stable InRelease", OutputType.STDOUT)
                        kotlinx.coroutines.delay(300)
                        addOutput("Get:2 https://security.debian.org/debian-security stable-security InRelease [48.1 kB]", OutputType.STDOUT)
                        kotlinx.coroutines.delay(400)
                        addOutput("Get:3 https://deb.debian.org/debian stable-updates InRelease [55.4 kB]", OutputType.STDOUT)
                        kotlinx.coroutines.delay(300)
                        addOutput("Reading package lists... Done", OutputType.INFO)
                        addOutput("Building dependency tree... Done", OutputType.INFO)
                        addOutput("Reading state information... Done", OutputType.INFO)
                        addOutput("5 packages can be upgraded or installed. Run 'apt list' to see them.", OutputType.SUCCESS)
                    } catch (e: Exception) {
                        addOutput("APT Error: ${e.localizedMessage}", OutputType.STDERR)
                    } finally {
                        _isRunning.value = false
                    }
                }
            }
            "upgrade" -> {
                _isRunning.value = true
                viewModelScope.launch {
                    try {
                        addOutput("Reading package lists... Done", OutputType.INFO)
                        addOutput("Building dependency tree... Done", OutputType.INFO)
                        addOutput("Reading state information... Done", OutputType.INFO)
                        val installed = _installedPackages.value
                        if (installed.isEmpty()) {
                            addOutput("0 upgraded, 0 newly installed, 0 to remove and 0 not upgraded.", OutputType.SUCCESS)
                            return@launch
                        }
                        addOutput("The following packages will be upgraded:", OutputType.INFO)
                        addOutput("  " + installed.joinToString(" "), OutputType.STDOUT)
                        addOutput("Need to get 142 kB of archives.", OutputType.STDOUT)
                        addOutput("After this operation, 12.3 kB of additional disk space will be used.", OutputType.STDOUT)
                        kotlinx.coroutines.delay(600)
                        for (pkg in installed) {
                            addOutput("Get:1 https://deb.debian.org/debian stable/main $pkg [35.5 kB]", OutputType.STDOUT)
                            kotlinx.coroutines.delay(300)
                            addOutput("Preparing to unpack .../$pkg.deb ...", OutputType.STDOUT)
                            addOutput("Unpacking $pkg ...", OutputType.STDOUT)
                            addOutput("Setting up $pkg ...", OutputType.SUCCESS)
                            kotlinx.coroutines.delay(200)
                        }
                        addOutput("Upgrade complete! All custom tools are at their latest version.", OutputType.SUCCESS)
                    } catch (e: Exception) {
                        addOutput("APT Error: ${e.localizedMessage}", OutputType.STDERR)
                    } finally {
                        _isRunning.value = false
                    }
                }
            }
            "install" -> {
                if (parts.size < 3) {
                    addOutput("E: You must specify at least one package to install", OutputType.STDERR)
                    return
                }
                val pkgName = parts[2].lowercase()
                val validPackages = listOf("subfinder", "dnslookup", "whois", "netcat", "ipinfo")
                if (!validPackages.contains(pkgName)) {
                    addOutput("E: Unable to locate package '$pkgName'", OutputType.STDERR)
                    addOutput("Available sandboxed tools: " + validPackages.joinToString(", "), OutputType.INFO)
                    return
                }
                if (isPackageInstalled(pkgName)) {
                    addOutput("$pkgName is already the newest version (2.1.0-android).", OutputType.SUCCESS)
                    return
                }

                _isRunning.value = true
                viewModelScope.launch {
                    try {
                        addOutput("Reading package lists... Done", OutputType.INFO)
                        addOutput("Building dependency tree... Done", OutputType.INFO)
                        addOutput("The following NEW packages will be installed:", OutputType.INFO)
                        addOutput("  $pkgName", OutputType.STDOUT)
                        addOutput("Need to get 85.2 kB of archives.", OutputType.STDOUT)
                        kotlinx.coroutines.delay(500)
                        addOutput("Get:1 https://deb.debian.org/debian stable/main $pkgName [85.2 kB]", OutputType.STDOUT)
                        kotlinx.coroutines.delay(400)
                        addOutput("Unpacking $pkgName (2.1.0-android) ...", OutputType.STDOUT)
                        kotlinx.coroutines.delay(300)
                        addOutput("Setting up $pkgName (2.1.0-android) ...", OutputType.STDOUT)
                        
                        // Save to preferences
                        val updated = _installedPackages.value + pkgName
                        _installedPackages.value = updated
                        prefs.edit().putStringSet("installed_packages", updated).apply()
                        
                        addOutput("Package '$pkgName' installed successfully!", OutputType.SUCCESS)
                        addOutput("Type '$pkgName' to run it now.", OutputType.INFO)
                    } catch (e: Exception) {
                        addOutput("APT Error: ${e.localizedMessage}", OutputType.STDERR)
                    } finally {
                        _isRunning.value = false
                    }
                }
            }
            "remove" -> {
                if (parts.size < 3) {
                    addOutput("E: You must specify at least one package to remove", OutputType.STDERR)
                    return
                }
                val pkgName = parts[2].lowercase()
                if (!isPackageInstalled(pkgName)) {
                    addOutput("E: Package '$pkgName' is not installed, so it cannot be removed", OutputType.STDERR)
                    return
                }

                _isRunning.value = true
                viewModelScope.launch {
                    try {
                        addOutput("Removing $pkgName ...", OutputType.STDOUT)
                        kotlinx.coroutines.delay(400)
                        
                        val updated = _installedPackages.value - pkgName
                        _installedPackages.value = updated
                        prefs.edit().putStringSet("installed_packages", updated).apply()
                        
                        addOutput("Package '$pkgName' uninstalled successfully.", OutputType.SUCCESS)
                    } catch (e: Exception) {
                        addOutput("APT Error: ${e.localizedMessage}", OutputType.STDERR)
                    } finally {
                        _isRunning.value = false
                    }
                }
            }
            "list" -> {
                addOutput("Listing sandboxed packages...", OutputType.INFO)
                val allPkgs = listOf("subfinder", "dnslookup", "whois", "netcat", "ipinfo")
                allPkgs.forEach { pkg ->
                    val isInstalled = isPackageInstalled(pkg)
                    val statusStr = if (isInstalled) "[installed]" else "[not installed]"
                    val desc = when (pkg) {
                        "subfinder" -> "Passive subdomain enumeration via CT logs"
                        "dnslookup" -> "DNS record query (A, MX, TXT, CNAME, etc.)"
                        "whois" -> "Domain registration WHOIS databases client"
                        "netcat" -> "TCP network port probe utility (nc alias supported)"
                        "ipinfo" -> "Public IP address details & geolocation"
                        else -> ""
                    }
                    val type = if (isInstalled) OutputType.SUCCESS else OutputType.STDOUT
                    addOutput(String.format("%-11s  %-15s - %s", pkg, statusStr, desc), type)
                }
            }
            else -> {
                addOutput("E: Invalid operation $subCmd", OutputType.STDERR)
            }
        }
    }

    private fun runSubfinderCommand(domain: String) {
        _isRunning.value = true
        activeJob?.cancel()
        var pid = -1
        activeJob = viewModelScope.launch(Dispatchers.IO) {
            pid = registerProcess("subfinder $domain", coroutineContext[Job])
            try {
                withContext(Dispatchers.Main) {
                    addOutput("subfinder: passive subdomain enumeration for '$domain' using Certificate Transparency logs...", OutputType.INFO)
                }
                
                // Fetch passively from HackerTarget Host Search API which uses public search logs
                val urlStr = "https://api.hackertarget.com/hostsearch/?q=$domain"
                val url = java.net.URL(urlStr)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (TerminalDroid; Android)")

                val responseCode = connection.responseCode
                if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    val stream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(stream))
                    val results = mutableListOf<String>()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val trimmed = line?.trim() ?: ""
                        if (trimmed.isNotEmpty() && !trimmed.contains("error limit")) {
                            results.add(trimmed)
                        }
                    }
                    reader.close()

                    withContext(Dispatchers.Main) {
                        if (results.isEmpty()) {
                            addOutput("subfinder: No subdomains found or API rate limit reached.", OutputType.STDERR)
                        } else {
                            addOutput("subfinder: Found ${results.size} subdomains/hosts:", OutputType.SUCCESS)
                            results.forEach { res ->
                                val parts = res.split(",")
                                val sub = parts.getOrNull(0) ?: ""
                                val ip = parts.getOrNull(1) ?: ""
                                addOutput(String.format("  %-35s  [%s]", sub, ip), OutputType.STDOUT)
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        addOutput("subfinder: Server returned HTTP response code: $responseCode", OutputType.STDERR)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addOutput("subfinder: enumeration failed: ${e.localizedMessage}", OutputType.STDERR)
                }
            } finally {
                completeProcess(pid)
                withContext(Dispatchers.Main) {
                    _isRunning.value = false
                }
            }
        }
    }

    private fun runDnsLookup(host: String, recordType: String) {
        _isRunning.value = true
        activeJob?.cancel()
        var pid = -1
        activeJob = viewModelScope.launch(Dispatchers.IO) {
            pid = registerProcess("dnslookup $host $recordType", coroutineContext[Job])
            try {
                withContext(Dispatchers.Main) {
                    addOutput("dnslookup: Querying Cloudflare secure DoH for record '$recordType' of '$host'...", OutputType.INFO)
                }

                // Query Cloudflare DNS-over-HTTPS JSON API
                val urlStr = "https://cloudflare-dns.com/dns-query?name=$host&type=$recordType"
                val url = java.net.URL(urlStr)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("Accept", "application/json")
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                val responseCode = connection.responseCode
                if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    val stream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(stream))
                    val response = reader.readText()
                    reader.close()

                    val json = org.json.JSONObject(response)
                    val status = json.optInt("Status", -1)
                    val answers = json.optJSONArray("Answer")

                    withContext(Dispatchers.Main) {
                        if (status != 0) {
                            addOutput("dnslookup: Query status returned failure code: $status", OutputType.STDERR)
                        } else if (answers == null || answers.length() == 0) {
                            addOutput("dnslookup: No $recordType records found for '$host'.", OutputType.INFO)
                        } else {
                            addOutput("dnslookup: Resolved ${answers.length()} record(s):", OutputType.SUCCESS)
                            for (i in 0 until answers.length()) {
                                val item = answers.getJSONObject(i)
                                val name = item.optString("name")
                                val data = item.optString("data")
                                val ttl = item.optInt("TTL")
                                addOutput(String.format("  %-25s  TTL=%-5d  data=%s", name, ttl, data), OutputType.STDOUT)
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        addOutput("dnslookup: Cloudflare returned HTTP response code: $responseCode", OutputType.STDERR)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addOutput("dnslookup: lookup failed: ${e.localizedMessage}", OutputType.STDERR)
                }
            } finally {
                completeProcess(pid)
                withContext(Dispatchers.Main) {
                    _isRunning.value = false
                }
            }
        }
    }

    private fun runWhoisCommand(domain: String) {
        _isRunning.value = true
        activeJob?.cancel()
        var pid = -1
        activeJob = viewModelScope.launch(Dispatchers.IO) {
            pid = registerProcess("whois $domain", coroutineContext[Job])
            try {
                withContext(Dispatchers.Main) {
                    addOutput("whois: Querying registry database for '$domain'...", OutputType.INFO)
                }

                val urlStr = "https://api.hackertarget.com/whois/?q=$domain"
                val url = java.net.URL(urlStr)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (TerminalDroid; Android)")

                val responseCode = connection.responseCode
                if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    val stream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(stream))
                    val body = StringBuilder()
                    var line: String?
                    var linesCount = 0
                    while (reader.readLine().also { line = it } != null) {
                        body.append(line).append("\n")
                        linesCount++
                        if (linesCount > 40) {
                            body.append("... [Output truncated to 40 lines. Real whois might contain more registration text] ...\n")
                            break
                        }
                    }
                    reader.close()

                    withContext(Dispatchers.Main) {
                        addOutput(body.toString(), OutputType.STDOUT)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        addOutput("whois: Server returned HTTP response code: $responseCode", OutputType.STDERR)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addOutput("whois: execution failed: ${e.localizedMessage}", OutputType.STDERR)
                }
            } finally {
                completeProcess(pid)
                withContext(Dispatchers.Main) {
                    _isRunning.value = false
                }
            }
        }
    }

    private fun runNetcatCommand(host: String, portStr: String) {
        _isRunning.value = true
        activeJob?.cancel()
        var pid = -1
        activeJob = viewModelScope.launch(Dispatchers.IO) {
            pid = registerProcess("nc $host $portStr", coroutineContext[Job])
            try {
                val port = portStr.toIntOrNull()
                if (port == null || port < 1 || port > 65535) {
                    withContext(Dispatchers.Main) {
                        addOutput("netcat: Invalid port number: '$portStr'", OutputType.STDERR)
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    addOutput("nc: Probing TCP host $host on port $port...", OutputType.INFO)
                }

                val socket = java.net.Socket()
                val start = System.currentTimeMillis()
                socket.connect(java.net.InetSocketAddress(host, port), 4000)
                val delay = System.currentTimeMillis() - start
                socket.close()

                withContext(Dispatchers.Main) {
                    addOutput("Connection to $host $port port [tcp] succeeded! Response time: $delay ms", OutputType.SUCCESS)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addOutput("nc: connect to $host port $portStr (tcp) failed: ${e.localizedMessage}", OutputType.STDERR)
                }
            } finally {
                completeProcess(pid)
                withContext(Dispatchers.Main) {
                    _isRunning.value = false
                }
            }
        }
    }

    private fun runIpInfoCommand(ip: String) {
        _isRunning.value = true
        activeJob?.cancel()
        var pid = -1
        activeJob = viewModelScope.launch(Dispatchers.IO) {
            pid = registerProcess("ipinfo" + (if (ip.isEmpty()) "" else " $ip"), coroutineContext[Job])
            try {
                withContext(Dispatchers.Main) {
                    if (ip.isEmpty()) {
                        addOutput("ipinfo: Querying public geolocation details of your current client IP...", OutputType.INFO)
                    } else {
                        addOutput("ipinfo: Querying geolocation details of IP '$ip'...", OutputType.INFO)
                    }
                }

                val urlStr = if (ip.isEmpty()) "https://ipapi.co/json/" else "https://ipapi.co/$ip/json/"
                val url = java.net.URL(urlStr)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (TerminalDroid; Android)")
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                val responseCode = connection.responseCode
                if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    val stream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(stream))
                    val response = reader.readText()
                    reader.close()

                    val json = org.json.JSONObject(response)
                    val ipAddr = json.optString("ip", "N/A")
                    val city = json.optString("city", "N/A")
                    val region = json.optString("region", "N/A")
                    val country = json.optString("country_name", "N/A")
                    val asn = json.optString("asn", "N/A")
                    val orgName = json.optString("org", "N/A")
                    val lat = json.optDouble("latitude", 0.0)
                    val lon = json.optDouble("longitude", 0.0)

                    withContext(Dispatchers.Main) {
                        addOutput("--- IP Geolocation Report ---", OutputType.INFO)
                        addOutput("IP Address:   $ipAddr", OutputType.STDOUT)
                        addOutput("ASN / ISP:    $asn - $orgName", OutputType.STDOUT)
                        addOutput("Location:     $city, $region, $country", OutputType.SUCCESS)
                        addOutput("Coordinates:  Lat $lat, Lon $lon", OutputType.STDOUT)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        addOutput("ipinfo: Server returned HTTP response code: $responseCode", OutputType.STDERR)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addOutput("ipinfo: execution failed: ${e.localizedMessage}", OutputType.STDERR)
                }
            } finally {
                completeProcess(pid)
                withContext(Dispatchers.Main) {
                    _isRunning.value = false
                }
            }
        }
    }

    private fun handleEcho(command: String, parts: List<String>) {
        if (parts.size < 2) {
            addOutput("", OutputType.STDOUT)
            return
        }

        val hasAppend = command.contains(">>")
        val hasOverwrite = !hasAppend && command.contains(">")

        if (hasAppend || hasOverwrite) {
            val delimiter = if (hasAppend) ">>" else ">"
            val index = command.indexOf(delimiter)
            if (index == -1) return

            val echoPart = command.substring(0, index).trim()
            val filePart = command.substring(index + delimiter.length).trim()

            // Extract the message inside echo
            var message = if (echoPart.length > 5) echoPart.substring(5).trim() else ""
            if ((message.startsWith("\"") && message.endsWith("\"")) || 
                (message.startsWith("'") && message.endsWith("'"))) {
                message = message.substring(1, message.length - 1)
            }

            if (filePart.isEmpty()) {
                addOutput("echo: syntax error near unexpected token 'newline'", OutputType.STDERR)
                return
            }

            try {
                val file = File(_currentDirectoryPath.value, filePart)
                if (hasAppend) {
                    file.appendText(message + "\n")
                } else {
                    file.writeText(message + "\n")
                }
                // Silent success like Linux
            } catch (e: Exception) {
                addOutput("echo: failed to write to file '$filePart': ${e.localizedMessage}", OutputType.STDERR)
            }
        } else {
            // Standard echo
            var message = if (command.length > 5) command.substring(5).trim() else ""
            if ((message.startsWith("\"") && message.endsWith("\"")) || 
                (message.startsWith("'") && message.endsWith("'"))) {
                message = message.substring(1, message.length - 1)
            }
            addOutput(message, OutputType.STDOUT)
        }
    }

    private fun runPingSimulation(host: String) {
        _isRunning.value = true
        activeJob?.cancel()
        activeJob = viewModelScope.launch {
            try {
                addOutput("PING $host (142.250.190.46) 56(84) bytes of data.", OutputType.INFO)
                kotlinx.coroutines.delay(400)
                
                for (seq in 1..4) {
                    val ttl = (50..120).random()
                    val time = String.format("%.2f", (10.0 + Math.random() * 25.0))
                    addOutput("64 bytes from 142.250.190.46: icmp_seq=$seq ttl=$ttl time=$time ms", OutputType.STDOUT)
                    kotlinx.coroutines.delay(800)
                }
                
                addOutput("--- $host ping statistics ---", OutputType.INFO)
                addOutput("4 packets transmitted, 4 received, 0% packet loss, time 2400ms", OutputType.STDOUT)
                addOutput("rtt min/avg/max = 10.21/18.45/32.14 ms", OutputType.STDOUT)
            } catch (e: Exception) {
                // Cancelled
            } finally {
                _isRunning.value = false
            }
        }
    }

    private fun runCurlCommand(urlStr: String) {
        _isRunning.value = true
        activeJob?.cancel()
        activeJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                var formattedUrl = urlStr
                if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                    formattedUrl = "https://$formattedUrl"
                }
                
                withContext(Dispatchers.Main) {
                    addOutput("Connecting to $formattedUrl...", OutputType.INFO)
                }

                val url = java.net.URL(formattedUrl)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (TerminalDroid; Android)")

                val responseCode = connection.responseCode
                if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                    val stream = connection.inputStream
                    val reader = BufferedReader(InputStreamReader(stream))
                    val body = StringBuilder()
                    var line: String?
                    var lineCount = 0
                    while (reader.readLine().also { line = it } != null) {
                        body.append(line).append("\n")
                        lineCount++
                        // Limit to 45 lines of text to avoid flooding the console
                        if (lineCount > 45) {
                            body.append("... [Output truncated to 45 lines] ...\n")
                            break
                        }
                    }
                    reader.close()

                    withContext(Dispatchers.Main) {
                        addOutput(body.toString(), OutputType.STDOUT)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        addOutput("curl: Server returned HTTP response code: $responseCode", OutputType.STDERR)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addOutput("curl: connection failed: ${e.localizedMessage}", OutputType.STDERR)
                    addOutput("Please check the URL or your network connection.", OutputType.INFO)
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isRunning.value = false
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeProcess?.destroy()
        activeJob?.cancel()
        disconnectSsh()
    }
}
