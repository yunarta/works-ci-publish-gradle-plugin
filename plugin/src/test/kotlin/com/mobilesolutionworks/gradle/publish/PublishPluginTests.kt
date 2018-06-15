package com.mobilesolutionworks.gradle.publish

import com.nhaarman.mockito_kotlin.whenever
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.mockito.Mockito

class PublishPluginTests {

    @Test
    fun testExtension() {
        val project = Mockito.mock(Project::class.java)
        val extensions = Mockito.mock(ExtensionContainer::class.java)
        whenever(project.extensions).thenReturn(extensions)

        val any = project.worksPublication
        assertNull(project.worksPublication)

        val publication = Publication()
        whenever(extensions.findByName("publication")).thenReturn(publication)
        assertEquals(publication, project.worksPublication)
    }

}