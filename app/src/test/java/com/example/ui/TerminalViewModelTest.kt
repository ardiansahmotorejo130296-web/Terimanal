package com.example.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TerminalViewModelTest {

    private lateinit var viewModel: TerminalViewModel

    @Before
    fun setup() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        viewModel = TerminalViewModel(application)
    }

    private fun setHistory(history: List<String>) {
        val field = TerminalViewModel::class.java.getDeclaredField("_commandHistoryList")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val flow = field.get(viewModel) as MutableStateFlow<List<String>>
        flow.value = history
    }

    @Test
    fun `navigateHistoryUp with empty history does nothing`() {
        setHistory(emptyList())
        viewModel.inputText.value = "initial"

        viewModel.navigateHistoryUp()

        assertEquals("initial", viewModel.inputText.value)
    }

    @Test
    fun `navigateHistoryDown with empty history does nothing`() {
        setHistory(emptyList())
        viewModel.inputText.value = "initial"

        viewModel.navigateHistoryDown()

        assertEquals("initial", viewModel.inputText.value)
    }

    @Test
    fun `navigateHistoryDown from index -1 does nothing`() {
        setHistory(listOf("cmd1"))
        viewModel.inputText.value = "initial"

        viewModel.navigateHistoryDown()

        assertEquals("initial", viewModel.inputText.value)
    }

    @Test
    fun `navigateHistoryUp navigates through history and saves current input`() {
        setHistory(listOf("cmd1", "cmd2", "cmd3")) // Index 0: cmd1, Index 1: cmd2, etc.
        viewModel.inputText.value = "current input"

        // History starts at index -1
        viewModel.navigateHistoryUp()
        assertEquals("cmd1", viewModel.inputText.value)

        viewModel.navigateHistoryUp()
        assertEquals("cmd2", viewModel.inputText.value)

        viewModel.navigateHistoryUp()
        assertEquals("cmd3", viewModel.inputText.value)

        // Cannot go past the end of history
        viewModel.navigateHistoryUp()
        assertEquals("cmd3", viewModel.inputText.value)
    }

    @Test
    fun `navigateHistoryDown from middle of history`() {
        setHistory(listOf("cmd1", "cmd2", "cmd3"))
        viewModel.inputText.value = "current input"

        // Go up to cmd2
        viewModel.navigateHistoryUp() // cmd1
        viewModel.navigateHistoryUp() // cmd2

        // Go back down
        viewModel.navigateHistoryDown() // cmd1
        assertEquals("cmd1", viewModel.inputText.value)

        viewModel.navigateHistoryDown() // back to saved input (index -1)
        assertEquals("current input", viewModel.inputText.value)

        // Cannot go below -1
        viewModel.navigateHistoryDown()
        assertEquals("current input", viewModel.inputText.value)
    }
}
