package com.mobilesolutionworks.gradle.publish;

import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.util.zip.ZipEntry
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
