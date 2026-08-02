package com.example.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.robolectric.annotation.Config
import java.lang.reflect.Field
import kotlinx.coroutines.flow.MutableStateFlow

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class TerminalViewModelTest {

    private lateinit var viewModel: TerminalViewModel
    private lateinit var tempDir: File
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val application = ApplicationProvider.getApplicationContext<Application>()
        viewModel = TerminalViewModel(application)

        tempDir = Files.createTempDirectory("terminal_test").toFile()

        File(tempDir, "test_file.txt").createNewFile()
        File(tempDir, "another_file.txt").createNewFile()
        File(tempDir, "test_dir").mkdir()
        File(tempDir, "test_dir2").mkdir()

        // Use reflection to set _currentDirectoryPath for testing autocomplete
        val field = TerminalViewModel::class.java.getDeclaredField("_currentDirectoryPath")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val stateFlow = field.get(viewModel) as MutableStateFlow<String>
        stateFlow.value = tempDir.absolutePath
    }

    @After
    fun teardown() {
        tempDir.deleteRecursively()
        Dispatchers.resetMain()
    }

    @Test
    fun handleTabAutocomplete_emptyInput_doesNothing() {
        viewModel.inputText.value = ""
        viewModel.handleTabAutocomplete()
        assertEquals("", viewModel.inputText.value)
    }

    @Test
    fun handleTabAutocomplete_singleFileMatch_autocompletesFile() {
        viewModel.inputText.value = "cat a"
        viewModel.handleTabAutocomplete()
        assertEquals("cat another_file.txt", viewModel.inputText.value)
    }

    @Test
    fun handleTabAutocomplete_singleDirMatch_autocompletesDirWithSlash() {
        viewModel.inputText.value = "cd test_dir2"
        viewModel.handleTabAutocomplete()
        assertEquals("cd test_dir2/", viewModel.inputText.value)
    }

    @Test
    fun handleTabAutocomplete_multipleMatches_doesNotAutocomplete_addsOutput() {
        viewModel.inputText.value = "ls test"
        viewModel.handleTabAutocomplete()
        assertEquals("ls test", viewModel.inputText.value)
    }

    @Test
    fun handleTabAutocomplete_noMatches_doesNothing() {
        viewModel.inputText.value = "cat nonexistent"
        viewModel.handleTabAutocomplete()
        assertEquals("cat nonexistent", viewModel.inputText.value)
    }

    @Test
    fun handleTabAutocomplete_ignoreCase_autocompletesFile() {
        viewModel.inputText.value = "cat ANOTHER"
        viewModel.handleTabAutocomplete()
        assertEquals("cat another_file.txt", viewModel.inputText.value)
    }
}
