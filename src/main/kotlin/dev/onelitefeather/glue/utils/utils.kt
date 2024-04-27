@file:OptIn(ExperimentalPathApi::class)

package dev.onelitefeather.glue.utils

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.attribute.DosFileAttributeView
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.fileAttributesView
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.notExists
import kotlin.streams.asSequence
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.*

fun Any.convertToPath(): Path {
    return when (this) {
        is Path -> this
        is File -> this.toPath()
        is FileSystemLocation -> this.path
        is Provider<*> -> this.get().convertToPath()
        else -> throw IllegalArgumentException("Unknown type representing a file: ${this.javaClass.name}")
    }
}

val FileSystemLocation.path: Path
    get() = asFile.toPath()
val Provider<out FileSystemLocation>.path: Path
    get() = get().path

fun Path.deleteRecursive(excludes: Iterable<PathMatcher> = emptyList()) {
    if (!exists()) {
        return
    }
    if (!isDirectory()) {
        if (excludes.any { it.matches(this) }) {
            return
        }
        fixWindowsPermissionsForDeletion()
        deleteIfExists()
        return
    }

    val fileList = Files.walk(this).use { stream ->
        stream.asSequence().filterNot { file -> excludes.any { it.matches(file) } }.toList()
    }

    fileList.forEach { f -> f.fixWindowsPermissionsForDeletion() }
    fileList.asReversed().forEach { f ->
        // Don't try to delete directories where the excludes glob has caused files to not get deleted inside it
        if (f.isRegularFile()) {
            f.deleteIfExists()
        } else if (f.isDirectory() && f.listDirectoryEntries().isEmpty()) {
            f.deleteIfExists()
        }
    }
}

private val isWindows = System.getProperty("os.name").contains("windows", ignoreCase = true)
private fun Path.fixWindowsPermissionsForDeletion() {
    if (!isWindows || notExists()) {
        return
    }

    runCatching {
        val dosAttr = fileAttributesView<DosFileAttributeView>()
        dosAttr.setHidden(false)
        dosAttr.setReadOnly(false)
    }
}

fun Path.ensureClean(): Path {
    try {
        deleteRecursively()
    } catch (e: Exception) {
        println("Failed to delete $this: ${e.javaClass.name}: ${e.message}")
        e.suppressedExceptions.forEach { println("Suppressed exception: $it") }
        throw IllegalStateException("Failed to delete $this", e)
    }
    parent.createDirectories()
    return this
}

fun String.capitalized(): String {
    return replaceFirstChar(Char::uppercase)
}

inline fun <reified T : Task> TaskContainer.providerFor(name: String): TaskProvider<T> {
    return if (names.contains(name)) {
        named<T>(name)
    } else {
        register<T>(name)
    }
}

inline fun <reified T : Task> TaskContainer.configureTask(name: String, noinline configure: T.() -> Unit): TaskProvider<T> {
    return if (names.contains(name)) {
        named(name, configure)
    } else {
        register(name, configure)
    }
}

fun <T : FileSystemLocation> Provider<out T>.fileExists(project: Project): Provider<out T?> {
    return flatMap { project.provider { it.takeIf { f -> f.path.exists() } } }
}