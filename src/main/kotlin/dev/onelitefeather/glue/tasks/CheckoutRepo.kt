package dev.onelitefeather.glue.tasks

import java.time.Instant
import kotlin.io.path.createDirectories
import kotlin.io.path.notExists
import dev.onelitefeather.glue.utils.deleteRecursive
import dev.onelitefeather.glue.utils.path
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.lib.RepositoryCache
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.util.FS
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class CheckoutRepo : BaseTask() {

    @get:Input
    abstract val repoName: Property<String>

    @get:Input
    abstract val url: Property<String>

    @get:Input
    abstract val ref: Property<String>

    @get:Input
    abstract val shallowClone: Property<Boolean>

    @get:Input
    abstract val initializeSubmodules: Property<Boolean>

    @get:Input
    abstract val initializeSubmodulesShallow: Property<Boolean>

    @get:Internal
    abstract val workDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    init {
        @Suppress("LeakingThis")
        run {
            repoName.finalizeValueOnRead()
            url.finalizeValueOnRead()
            ref.finalizeValueOnRead()
            shallowClone.convention(false).finalizeValueOnRead()
            initializeSubmodules.convention(true).finalizeValueOnRead()
            initializeSubmodulesShallow.convention(false).finalizeValueOnRead()

            outputDir.convention(workDir.dir(repoName)).finalizeValueOnRead()
        }
    }

    @TaskAction
    fun run() {
        val dir = workDir.path
        val urlText = url.get().trim()

        if (dir.resolve(".git").notExists()) {
            dir.deleteRecursive()
            dir.createDirectories()

            Git.cloneRepository()
                .setDirectory(dir.toFile())
                .setURI(urlText)
                .setRemote("origin")
                .call()
        }

        val key = RepositoryCache.FileKey.lenient(dir.toFile(), FS.DETECTED)
        val db = RepositoryBuilder().setFS(FS.DETECTED).setGitDir(key.file).setMustExist(true).build()
        val repo = Git(db)
        repo.remoteRemove().setRemoteName("origin").call()
        repo.remoteAdd().setName("origin").setUri(URIish(urlText)).call()

        if (shallowClone.get()) {
            repo.fetch().setDepth(1).setRemote("origin").call()
        } else {
            repo.fetch().setShallowSince(Instant.MIN).setRemote("origin").call()
        }
        repo.checkout()
            .setForced(true)
            .setAllPaths(true)
            .setName(ref.get())
            .call()
        if (initializeSubmodules.get()) {
            if (initializeSubmodulesShallow.get()) {
                repo.submoduleInit().call()
                repo.submoduleUpdate().call()
            } else {
                repo.submoduleInit().call()
                repo.submoduleUpdate().call()
            }
        }

    }
}