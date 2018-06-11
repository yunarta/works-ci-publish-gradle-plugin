package com.mobilesolutionworks.gradle.publish

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.plugins.ExtensionContainer
import java.io.File
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

internal enum class DependencyTransform(val configuration: String, val scope: String) {
    TestImplementation("testImplementation", "test"),
    Implementation("implementation", "runtime"),
    Api("api", "compile"),
    CompileOnly("compileOnly", "provided");
}

internal class DependencyData(
        val group: String?,
        val artifact: String,
        val version: String?,
        val scope: String
)


internal fun Collection<File>.commaSeparated(): String = joinToString(File.pathSeparator)

internal fun Project.makeFileTree(files: Collection<File>): ConfigurableFileTree {
    return fileTree(files.commaSeparated())
}

internal fun ExtensionContainer.has(name: String): Boolean {
    return findByName(name) != null
}

internal fun File.md5zip(destination: File) {
    val aar = ZipInputStream(inputStream())
    var entry: ZipEntry? = aar.nextEntry
    while (entry != null) {
        val file = File(destination, entry.name)
        if (entry.isDirectory) {
            file.mkdirs()
        } else {
            file.parentFile.mkdirs()
//            println(file.parentFile.absolutePath)
//            println(file.parentFile.mkdirs())
//            println(file.parentFile.exists())
            aar.copyTo(file.outputStream())
            aar.closeEntry()
        }

        entry = aar.nextEntry
    }
}

internal fun File.md5(
        instance: MessageDigest = MessageDigest.getInstance("MD5"),
        bufferSize: Int = DEFAULT_BUFFER_SIZE
): ByteArray {
    instance.reset()
    var bytesCopied: Long = 0
    val buffer = ByteArray(bufferSize)
    val stream = inputStream()

    var bytes = stream.read(buffer)
    while (bytes >= 0) {
        instance.update(buffer, 0, bytes)
        bytesCopied += bytes
        bytes = stream.read(buffer)
    }

    return instance.digest()
}

private val hexArray = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f')
private const val HIGH_ORDER = 0xF0
private const val LOW_ORDER = 0x0F
private const val BIT_COUNT = 4

fun md5zip(source: File, destination: File, exception: (File) -> ByteArray? = { null }): ByteArray {
    source.md5zip(destination)

    val instance = MessageDigest.getInstance("MD5")
    val hashMap = TreeMap<String, ByteArray>()

    destination.walkTopDown().forEach {
        if (destination != it && it.isFile) {
            val hash = exception(it) ?: it.md5(instance)
            val path = it.toRelativeString(destination)
            hashMap[path] = hash
        }
    }

    instance.reset()
    hashMap.values.forEach {
        instance.update(it)
    }

    return instance.digest()
}


internal fun ByteArray.hexString(): String {
    val result = StringBuffer()

    forEach {
        val octet = it.toInt()
        val firstIndex = (octet and HIGH_ORDER).ushr(BIT_COUNT)
        val secondIndex = octet and LOW_ORDER
        result.append(hexArray[firstIndex])
        result.append(hexArray[secondIndex])
    }

    return result.toString()
}