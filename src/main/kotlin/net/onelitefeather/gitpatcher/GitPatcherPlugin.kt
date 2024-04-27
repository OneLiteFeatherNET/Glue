package net.onelitefeather.gitpatcher

import java.io.File
import net.onelitefeather.gitpatcher.tasks.ApplyGitPatches
import net.onelitefeather.gitpatcher.tasks.CheckoutRepo
import net.onelitefeather.gitpatcher.tasks.RebuildGitPatches
import net.onelitefeather.gitpatcher.upstream.PatcherUpstream
import net.onelitefeather.gitpatcher.upstream.RepoPatcherUpstream
import net.onelitefeather.gitpatcher.utils.configureTask
import net.onelitefeather.gitpatcher.utils.fileExists
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering

class GitPatcherPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val patcher = target.extensions.create("gitpatcher", GitPatcherExtension::class, target)

        val applyPatches by target.tasks.registering { group = "gitpatcher" }
        val rebuildPatches by target.tasks.registering { group = "gitpatcher" }

        patcher.upstreams.all {
            val upstreamTask = target.createUpstreamTask(this, patcher)
            val downstreamTask = target.createDownstreamTask(this, patcher, upstreamTask)
            val patchTask = target.createPatchTask(this, patcher, downstreamTask, applyPatches)
            target.rebuildPatchTask(this, rebuildPatches)
        }

    }

    private fun Project.createUpstreamTask(
        upstream: PatcherUpstream,
        ext: GitPatcherExtension,
    ): TaskProvider<CheckoutRepo>? {
        val cloneTask = (upstream as? RepoPatcherUpstream)?.let { repo ->
            val cloneTask = tasks.configureTask<CheckoutRepo>(repo.cloneTaskName) {
                group = "gitpatcher"
                repoName.convention(repo.name)
                url.convention(repo.url)
                ref.convention(repo.ref)
                workDir.convention(repo.upstreamDir)
            }

            return@let cloneTask
        }
        return cloneTask
    }

    private fun Project.createDownstreamTask(
        upstream: PatcherUpstream,
        ext: GitPatcherExtension,
        upstreamTask: TaskProvider<CheckoutRepo>?,
    ): TaskProvider<CheckoutRepo>? {
        val cloneTask = (upstream as? RepoPatcherUpstream)?.let { repo ->
            val cloneTask = tasks.configureTask<CheckoutRepo>(repo.upstreamTaskName) {
                dependsOn(upstreamTask)
                group = "gitpatcher"
                repoName.convention(repo.name)
                url.convention(repo.url)
                ref.convention(repo.ref)
                workDir.convention(repo.outputDir)
            }

            return@let cloneTask
        }
        return cloneTask
    }

    private fun Project.createPatchTask(
        config: PatcherUpstream,
        ext: GitPatcherExtension,
        downstreamTask: TaskProvider<CheckoutRepo>?,
        applyPatches: TaskProvider<Task>
    ): TaskProvider<ApplyGitPatches> {
        val project = this
        val patchTask = (config as? RepoPatcherUpstream)?.let { repo ->
            val patchTask = tasks.configureTask<ApplyGitPatches>(config.patchTaskName) {
                group = "gitpatcher"
                dependsOn(downstreamTask)

                if (downstreamTask != null) {
                    input.convention(repo.upstreamDir)
                } else {
                    input.convention(config.upstreamDir)
                }

                patches.convention(config.patchDir.fileExists(project))
                output.convention(config.outputDir)
                branchName.convention(config.ref)
                upstreamName.set("upstream")
                upstreamUri.convention(config.url)
            }
            return@let patchTask
        }
        applyPatches {
            dependsOn(patchTask)
        }

        return patchTask!!
    }

    private fun Project.rebuildPatchTask(config: PatcherUpstream, rebuildPatches: TaskProvider<Task>): TaskProvider<RebuildGitPatches> {
        val rebuildTask = tasks.configureTask<RebuildGitPatches>(config.rebuildTaskName) {
            group = "gitpatcher"

            base.convention(config.upstreamDir)
            patches.convention(config.patchDir)
            input.convention(config.outputDir)
        }

        rebuildPatches {
            dependsOn(rebuildTask)
        }

        return rebuildTask
    }
}