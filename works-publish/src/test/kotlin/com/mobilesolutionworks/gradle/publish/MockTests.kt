package com.mobilesolutionworks.gradle.publish;

import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtensionContainer
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.Mockito

class MockTests {

    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    @Test
    fun testMockProject() {
        val project = Mockito.mock(Project::class.java)
        val logger = Mockito.mock(Logger::class.java)

        val plugin = PublishPlugin()
        plugin.logWarn("test")

        plugin.createLogWriter(project)

        whenever(project.logger).thenReturn(logger)
        plugin.logWarn("test")

        verify(logger, atLeastOnce()).quiet("test")
    }

    @Test
    fun testMockMaven() {
        val project = Mockito.mock(Project::class.java)
        val extensions = Mockito.mock(ExtensionContainer::class.java)

        val plugin = PublishPlugin()
        plugin.createPOM(project, extensions)

        whenever(extensions.findByName("publishing")).thenReturn(Publication())
        plugin.createPOM(project, extensions)
    }
}
