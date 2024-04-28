package dev.onelitefeather.glue.tasks

import codechicken.diffpatch.cli.PatchOperation
import codechicken.diffpatch.match.FuzzyLineMatcher
import codechicken.diffpatch.util.LoggingOutputStream
import codechicken.diffpatch.util.PatchMode
import java.io.PrintStream
import java.time.Instant
import dev.onelitefeather.glue.utils.convertToPath
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.ResetCommand
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.util.FS
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.logging.LogLevel
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option

abstract class ApplyFilePatches : BaseTask() {

    @get:Input
    @get:Option(
        option = "verbose",
        description = "Prints out more info about the patching process",
    )
    @get:Optional
    abstract val verbose: Property<Boolean>

    @get:Input
    abstract val upstreamName: Property<String>

    @get:Input
    abstract val upstreamUri: Property<String>

    @get:Input
    abstract val branchName: Property<String>

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputDirectory
    abstract val input: DirectoryProperty

    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputDirectory
    abstract val patches: DirectoryProperty

    init {
        verbose.convention(true)
    }

    @TaskAction
    open fun run() {
        val downstreamPath = output.convertToPath()
        val key = RepositoryCache.FileKey.lenient(downstreamPath.toFile(), FS.DETECTED)
        val downstreamDb = RepositoryBuilder().setFS(FS.DETECTED).setGitDir(key.file).setMustExist(true).build()
        downstreamDb.config.setBoolean("commit", null, "gpgsign", false)
        downstreamDb.config.setBoolean("core", null, "safecrlf", false)
        val oldDownstreamGit = Git.wrap(downstreamDb)

        oldDownstreamGit.clean().setForce(true).setCleanDirectories(true).setIgnore(true).call()
        oldDownstreamGit.reset().setMode(ResetCommand.ResetType.HARD).call()

        val downstreamGit = Git.wrap(downstreamDb)
        downstreamGit.remoteRemove()
            .setRemoteName(upstreamName.getOrElse("upstream"))
            .call()

        downstreamGit.remoteAdd()
            .setName(upstreamName.get())
            .setUri(URIish(upstreamUri.get()))
            .call()

        downstreamGit.fetch()
            .setRemote(upstreamName.get())
            .setShallowSince(Instant.MIN)
            .setRemoveDeletedRefs(true)
            .call()
        downstreamGit.checkout()
            .addPath(upstreamName.get())
            .setName(branchName.getOrElse("master"))
            .call()
        downstreamGit
            .reset()
            .setMode(ResetCommand.ResetType.HARD)
            .setRef(branchName.getOrElse("master"))
            .call()


        val printStream = PrintStream(LoggingOutputStream(logger, LogLevel.LIFECYCLE))
        val result = PatchOperation.builder()
            .logTo(printStream)
            .basePath(output.convertToPath())
            .patchesPath(patches.convertToPath())
            .outputPath(output.convertToPath())
            .level(if (verbose.get()) codechicken.diffpatch.util.LogLevel.ALL else codechicken.diffpatch.util.LogLevel.INFO)
            .mode(PatchMode.OFFSET)
            .minFuzz(FuzzyLineMatcher.DEFAULT_MIN_MATCH_SCORE)
            .summary(verbose.get())
            .lineEnding("\n")
            .ignorePrefix(".git")
            .ignorePrefix("*.jar")
            .build()
            .operate()
        downstreamGit.close()
        commit()
        if (result.exit != 0) {
            val total = result.summary.failedMatches + result.summary.exactMatches +
                    result.summary.accessMatches + result.summary.offsetMatches + result.summary.fuzzyMatches
            throw Exception("Failed to apply ${result.summary.failedMatches}/$total hunks")
        }


        if (!verbose.get()) {
            logger.lifecycle("Applied ${result.summary.changedFiles} patches")
        }
    }

    private fun commit() {
        val ident = PersonIdent("File", "filepatches@automated.onelitefeather.net")
        val git = Git.open(output.convertToPath().toFile())
        git.add().addFilepattern(".").call()
        val ref = git.commit()
            .setMessage("File Patches")
            .setAuthor(ident)
            .setSign(false)
            .call()
        git.tagDelete().setTags("file").call()
        git.tag().setName("file").setTagger(ident).setSigned(false).call()
        git.close()
    }
}