package net.onelitefeather.gitpatcher.upstream

import net.onelitefeather.gitpatcher.tasks.ApplyGitPatches
import net.onelitefeather.gitpatcher.tasks.RebuildGitPatches
import net.onelitefeather.gitpatcher.utils.capitalized
import net.onelitefeather.gitpatcher.utils.providerFor
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.property

open class DefaultPatcherUpstream(
    private val name: String,
    protected val objects: ObjectFactory,
    protected val tasks: TaskContainer
) : PatcherUpstream  {

    override val upstreamDirPath: Property<String> = objects.property()
    override val upstreamDir: DirectoryProperty = objects.directoryProperty()
    override val patchDir: DirectoryProperty = objects.directoryProperty()
    override val outputDir: DirectoryProperty = objects.directoryProperty()

    override val upstreamTaskName: String
        get() = "clone${name.capitalized()}Upstream"

    override val patchTaskName: String
        get() = "apply${name.capitalized()}Patches"

    override val rebuildTaskName: String
        get() = "rebuild${name.capitalized()}Patches"

    override val patchTask: TaskProvider<ApplyGitPatches>
        get() = tasks.providerFor(patchTaskName)

    override val rebuildTask: TaskProvider<RebuildGitPatches>
        get() = tasks.providerFor(rebuildTaskName)

    override fun getName(): String = name
}