package com.example.ui

import android.app.Application
import androidx.test.core.app.ApplicationProvider
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
    private lateinit var application: Application
    private lateinit var filesDir: String

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        viewModel = TerminalViewModel(application)
        filesDir = application.filesDir.absolutePath
    }

    @Test
    fun getCompactPath_returnsTildeForExactFilesDir() {
        val result = viewModel.getCompactPath(filesDir)
        assertEquals("~", result)
    }

    @Test
    fun getCompactPath_returnsTildePlusPathForSubdirectory() {
        val subDir = "$filesDir/some/folder"
        val result = viewModel.getCompactPath(subDir)
        assertEquals("~/some/folder", result)
    }

    @Test
    fun getCompactPath_returnsFullPathForOutsidePath() {
        val outsidePath = "/some/other/path"
        val result = viewModel.getCompactPath(outsidePath)
        assertEquals("/some/other/path", result)
    }
}
