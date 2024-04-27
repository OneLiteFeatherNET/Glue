package dev.onelitefeather.glue.tasks

import codechicken.diffpatch.cli.DiffOperation
import codechicken.diffpatch.util.LogLevel
import codechicken.diffpatch.util.LoggingOutputStream
import java.io.PrintStream
import kotlin.io.path.createDirectory
import dev.onelitefeather.glue.utils.convertToPath
import dev.onelitefeather.glue.utils.ensureClean
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.util.FS
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class RebuildGitPatches : BaseTask() {

    @get:Input
    @get:Option(
        option = "verbose",
        description = "Prints out more info about the patching process",
    )
    @get:Optional
    abstract val verbose: Property<Boolean>

    @get:InputDirectory
    abstract val input: DirectoryProperty

    @get:InputDirectory
    abstract val base: DirectoryProperty

    @get:OutputDirectory
    abstract val patches: DirectoryProperty

    @get:Input
    abstract val contextLines: Property<Int>

    init {
        verbose.convention(false)
        contextLines.convention(3)
    }

    @TaskAction
    fun run() {
        val patchDir = patches.convertToPath().ensureClean()
        patchDir.createDirectory()
        val downstreamDir = input.convertToPath()
        val upstreamDir = base.convertToPath()

        val key = RepositoryCache.FileKey.lenient(downstreamDir.toFile(), FS.DETECTED)
        val downstreamDb = RepositoryBuilder().setFS(FS.DETECTED).setGitDir(key.file).setMustExist(true).build()
        downstreamDb.config.setBoolean("commit", null, "gpgsign", false)
        downstreamDb.config.setBoolean("core", null, "safecrlf", false)
        val downstreamGit = Git(downstreamDb)
        val downstreamCurrentBranch = downstreamGit.repository.fullBranch
        downstreamGit.checkout()
            .setName(downstreamCurrentBranch)
            .call()

        val printStream = PrintStream(LoggingOutputStream(logger, org.gradle.api.logging.LogLevel.LIFECYCLE))

        val result = DiffOperation.builder()
            .logTo(printStream)
            .aPath(upstreamDir)
            .bPath(downstreamDir)
            .outputPath(patchDir)
            .autoHeader(true)
            .level(if (verbose.get()) LogLevel.ALL else LogLevel.INFO)
            .lineEnding("\n")
            .ignorePrefix(".git")
            .ignorePrefix("**/.gradle")
            .context(contextLines.get())
            .summary(verbose.get())
            .build()
            .operate()

        logger.lifecycle("Rebuilt ${result.summary.changedFiles} patches")
    }
}