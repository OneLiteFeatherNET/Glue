package net.onelitefeather.gitpatcher

import net.onelitefeather.gitpatcher.extension.PatcherExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

class GitPatcherPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            val extension = extensions.create("patcher", PatcherExtension::class.java)

        }
    }
}