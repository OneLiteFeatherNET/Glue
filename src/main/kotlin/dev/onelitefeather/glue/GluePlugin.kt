package dev.onelitefeather.glue

import dev.onelitefeather.glue.tasks.ApplyFilePatches
import dev.onelitefeather.glue.tasks.CheckoutRepo
import dev.onelitefeather.glue.tasks.RebuildGitPatches
import dev.onelitefeather.glue.upstream.PatcherUpstream
import dev.onelitefeather.glue.upstream.RepoPatcherUpstream
import dev.onelitefeather.glue.utils.configureTask
import dev.onelitefeather.glue.utils.fileExists
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.registering

class GluePlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val patcher = target.extensions.create("glue", GlueExtension::class, target)

        val applyPatches by target.tasks.registering { group = "glue" }
        val rebuildPatches by target.tasks.registering { group = "glue" }

        patcher.upstreams.all {
            val upstreamTask = target.createUpstreamTask(this)
            val downstreamTask = target.createDownstreamTask(this, upstreamTask)
            target.createPatchTask(this, downstreamTask, applyPatches)
            target.rebuildPatchTask(this, rebuildPatches)
        }

    }

    private fun Project.createUpstreamTask(
        upstream: PatcherUpstream
    ): TaskProvider<CheckoutRepo>? {
        val cloneTask = (upstream as? RepoPatcherUpstream)?.let { repo ->
            val cloneTask = tasks.configureTask<CheckoutRepo>(repo.cloneTaskName) {
                group = "Glue"
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
        upstreamTask: TaskProvider<CheckoutRepo>?,
    ): TaskProvider<CheckoutRepo>? {
        val cloneTask = (upstream as? RepoPatcherUpstream)?.let { repo ->
            val cloneTask = tasks.configureTask<CheckoutRepo>(repo.upstreamTaskName) {
                dependsOn(upstreamTask)
                group = "glue"
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
        downstreamTask: TaskProvider<CheckoutRepo>?,
        applyPatches: TaskProvider<Task>
    ): TaskProvider<ApplyFilePatches> {
        val project = this
        val patchTask = (config as? RepoPatcherUpstream)?.let { repo ->
            val patchTask = tasks.configureTask<ApplyFilePatches>(config.patchTaskName) {
                group = "glue"
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
            group = "glue"

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