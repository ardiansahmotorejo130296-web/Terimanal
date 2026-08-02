package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.TerminalViewModel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.system.measureTimeMillis
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Before

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TouchBenchmarkTest {

    private lateinit var viewModel: TerminalViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
        val context = ApplicationProvider.getApplicationContext<Application>()
        viewModel = TerminalViewModel(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun benchmarkTouchCommand() = runTest {
        // Pre-create some files to see how fast we can run touch repeatedly
        val iterations = 100
        val times = mutableListOf<Long>()

        for (i in 1..iterations) {
            val filename = "test_file_$i.txt"
            val time = measureTimeMillis {
                viewModel.executeCommand("touch $filename")
            }
            times.add(time)
        }

        println("Average time per touch command: ${times.average()} ms")
        println("Total time for $iterations touch commands: ${times.sum()} ms")
    }
}
