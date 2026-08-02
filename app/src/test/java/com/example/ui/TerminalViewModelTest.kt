package com.example.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

@RunWith(RobolectricTestRunner::class)
class TerminalViewModelTest {

    private lateinit var viewModel: TerminalViewModel

    @Before
    fun setup() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        viewModel = TerminalViewModel(application)
    }

    @Test
    fun testKillProcess_ReturnsFalseIfProcessNotFound() {
        val result = viewModel.killProcess(9999)
        assertFalse("killProcess should return false for a non-existent PID", result)
    }

    @Test
    fun testKillProcess_RunningProcess() = runTest {
        // Find the initial system processes
        val initialProcesses = viewModel.processes.first()
        val runningProc = initialProcesses.find { it.status == "RUNNING" }
        assertNotNull("Should have at least one initial running process", runningProc)

        if (runningProc != null) {
            val pid = runningProc.pid

            // Kill the process
            val result = viewModel.killProcess(pid)
            assertTrue("killProcess should return true", result)

            // Verify state
            val processesAfter = viewModel.processes.first()
            val procAfter = processesAfter.find { it.pid == pid }
            assertNotNull("Process should still be in the list", procAfter)
            assertEquals("KILLED", procAfter?.status)
            assertTrue("CPU should be 0.0", procAfter?.cpu == 0.0)
            assertEquals("0.0 MB", procAfter?.memory)
        }
    }

    @Test
    fun testKillProcess_AlreadyCompletedProcess() = runTest {
        // Find the initial system processes
        val initialProcesses = viewModel.processes.first()
        val runningProc = initialProcesses.find { it.status == "RUNNING" }
        assertNotNull("Should have at least one initial running process", runningProc)

        if (runningProc != null) {
            val pid = runningProc.pid

            // Kill the process once
            viewModel.killProcess(pid)

            // Try to kill it again
            val result = viewModel.killProcess(pid)
            assertTrue("killProcess should return true since the process was found", result)

            val processesAfter = viewModel.processes.first()
            val procAfter = processesAfter.find { it.pid == pid }
            assertEquals("KILLED", procAfter?.status) // Should remain KILLED
        }
    }
}
