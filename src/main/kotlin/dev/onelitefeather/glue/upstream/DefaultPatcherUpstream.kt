package dev.onelitefeather.glue.upstream

import dev.onelitefeather.glue.tasks.ApplyFilePatches
import dev.onelitefeather.glue.tasks.RebuildGitPatches
import dev.onelitefeather.glue.utils.capitalized
import dev.onelitefeather.glue.utils.providerFor
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
) : PatcherUpstream {

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

    override val patchTask: TaskProvider<ApplyFilePatches>
        get() = tasks.providerFor(patchTaskName)

    override val rebuildTask: TaskProvider<RebuildGitPatches>
        get() = tasks.providerFor(rebuildTaskName)

    override fun getName(): String = name
}