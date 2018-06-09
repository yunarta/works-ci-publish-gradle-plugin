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
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class PluginHelpersTests {

    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    @Test
    fun md5zipTest() {
        val file = temporaryFolder.newFile()
        val stream = file.outputStream()
        val zip = ZipOutputStream(stream)
        zip.putNextEntry(ZipEntry("META-INF/"))
        zip.closeEntry()

        stream.close()
        val md5zip = md5zip(file, temporaryFolder.newFolder())
        println("md5zip = ${md5zip}")
    }

    @Test
    fun md5zipTest2() {
        val file = temporaryFolder.newFile()
        val stream = file.outputStream()
        val zip = ZipOutputStream(stream)
        zip.setLevel(0)
//        zip.putNextEntry(ZipEntry("META-INF"))

        val bytes = "test".toByteArray()

        val zipEntry = ZipEntry("META-INF/Manifest.txt")
        zipEntry.size = bytes.size.toLong()
        zipEntry.isDirectory

        zip.putNextEntry(zipEntry)
        zip.write(bytes)

        zip.closeEntry()

        zip.flush()
        stream.flush()
        stream.close()
        val md5zip = md5zip(file, temporaryFolder.newFolder())
        println("md5zip = ${md5zip}")
    }
}
