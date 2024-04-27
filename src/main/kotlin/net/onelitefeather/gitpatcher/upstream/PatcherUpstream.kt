package net.onelitefeather.gitpatcher.upstream

import net.onelitefeather.gitpatcher.tasks.ApplyFilePatches
import net.onelitefeather.gitpatcher.tasks.RebuildGitPatches
import org.gradle.api.Named
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider

interface PatcherUpstream : Named {

    val upstreamDirPath: Property<String>
    val upstreamDir: DirectoryProperty
    val upstreamTaskName: String

    val patchDir: DirectoryProperty
    val outputDir: DirectoryProperty

    val patchTaskName: String
    val rebuildTaskName: String
    val patchTask: TaskProvider<ApplyFilePatches>
    val rebuildTask: TaskProvider<RebuildGitPatches>
}