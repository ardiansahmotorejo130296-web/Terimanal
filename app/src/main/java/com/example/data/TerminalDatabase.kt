package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// 1. Local Command History (for global autocompletion/nav)
@Entity(tableName = "command_history")
data class CommandHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val command: String,
    val timestamp: Long = System.currentTimeMillis()
)

// 2. Named Sessions representing a saved state
@Entity(tableName = "terminal_sessions")
data class TerminalSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val workingDirectory: String,
    val timestamp: Long = System.currentTimeMillis()
)

// 3. Saved Command History specific to a saved session
@Entity(
    tableName = "session_history",
    foreignKeys = [
        ForeignKey(
            entity = TerminalSession::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class SessionHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int,
    val command: String,
    val timestamp: Long = System.currentTimeMillis()
)

// 4. Custom Command Aliases for shortcut execution
@Entity(tableName = "command_aliases")
data class CommandAlias(
    @PrimaryKey val alias: String,
    val command: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface CommandDao {
    @Query("SELECT * FROM command_history ORDER BY timestamp DESC LIMIT 100")
    fun getRecentHistory(): Flow<List<CommandHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommand(command: CommandHistory)

    @Query("DELETE FROM command_history")
    suspend fun clearHistory()
}

@Dao
interface AliasDao {
    @Query("SELECT * FROM command_aliases ORDER BY timestamp DESC")
    fun getAllAliasesFlow(): Flow<List<CommandAlias>>

    @Query("SELECT * FROM command_aliases")
    suspend fun getAllAliases(): List<CommandAlias>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: CommandAlias)

    @Query("DELETE FROM command_aliases WHERE alias = :alias")
    suspend fun deleteAlias(alias: String)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM terminal_sessions ORDER BY timestamp DESC")
    fun getAllSessionsFlow(): Flow<List<TerminalSession>>

    @Query("SELECT * FROM terminal_sessions ORDER BY timestamp DESC")
    suspend fun getAllSessions(): List<TerminalSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: TerminalSession): Long

    @Query("DELETE FROM terminal_sessions WHERE id = :sessionId")
    suspend fun deleteSession(sessionId: Int)

    @Query("SELECT * FROM session_history WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getHistoryForSession(sessionId: Int): List<SessionHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessionHistory(history: List<SessionHistory>)
}

@Database(
    entities = [CommandHistory::class, TerminalSession::class, SessionHistory::class, CommandAlias::class],
    version = 3,
    exportSchema = false
)
abstract class TerminalDatabase : RoomDatabase() {
    abstract fun commandDao(): CommandDao
    abstract fun sessionDao(): SessionDao
    abstract fun aliasDao(): AliasDao

    companion object {
        @Volatile
        private var INSTANCE: TerminalDatabase? = null

        fun getDatabase(context: Context): TerminalDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TerminalDatabase::class.java,
                    "terminal_database"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class TerminalRepository(private val database: TerminalDatabase) {
    private val commandDao = database.commandDao()
    private val sessionDao = database.sessionDao()
    private val aliasDao = database.aliasDao()

    val recentCommands: Flow<List<CommandHistory>> = commandDao.getRecentHistory()
    val allSessions: Flow<List<TerminalSession>> = sessionDao.getAllSessionsFlow()
    val allAliases: Flow<List<CommandAlias>> = aliasDao.getAllAliasesFlow()

    suspend fun getAliasesList(): List<CommandAlias> {
        return aliasDao.getAllAliases()
    }

    suspend fun addAlias(alias: String, command: String) {
        aliasDao.insertAlias(CommandAlias(alias = alias, command = command))
    }

    suspend fun deleteAlias(alias: String) {
        aliasDao.deleteAlias(alias)
    }

    suspend fun addCommand(commandText: String) {
        val trimmed = commandText.trim()
        if (trimmed.isNotEmpty()) {
            commandDao.insertCommand(CommandHistory(command = trimmed))
        }
    }

    suspend fun clearHistory() {
        commandDao.clearHistory()
    }

    // Sessions API
    suspend fun saveSession(name: String, workingDir: String, commandHistoryList: List<String>): Long {
        val session = TerminalSession(name = name, workingDirectory = workingDir)
        val sessionId = sessionDao.insertSession(session)

        val historyEntities = commandHistoryList.map { cmd ->
            SessionHistory(sessionId = sessionId.toInt(), command = cmd)
        }
        if (historyEntities.isNotEmpty()) {
            sessionDao.insertSessionHistory(historyEntities)
        }
        return sessionId
    }

    suspend fun deleteSession(sessionId: Int) {
        sessionDao.deleteSession(sessionId)
    }

    suspend fun getSessionHistory(sessionId: Int): List<String> {
        return sessionDao.getHistoryForSession(sessionId).map { it.command }
    }
}
