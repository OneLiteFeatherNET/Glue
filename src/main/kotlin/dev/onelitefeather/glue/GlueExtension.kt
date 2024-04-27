package dev.onelitefeather.glue

import dev.onelitefeather.glue.upstream.DefaultPatcherUpstream
import dev.onelitefeather.glue.upstream.DefaultRepoPatcherUpstream
import dev.onelitefeather.glue.upstream.PatcherUpstream
import dev.onelitefeather.glue.upstream.RepoPatcherUpstream
import org.gradle.api.Action
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.TaskContainer
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.polymorphicDomainObjectContainer
import org.gradle.kotlin.dsl.register

open class GlueExtension(project: Project, private val objects: ObjectFactory, layout: ProjectLayout, tasks: TaskContainer) {
    val upstreams: ExtensiblePolymorphicDomainObjectContainer<PatcherUpstream> = objects.polymorphicDomainObjectContainer(
        PatcherUpstream::class)

    init {
        upstreams.registerFactory(PatcherUpstream::class.java) { name -> DefaultPatcherUpstream(name, objects, tasks) }
        upstreams.registerFactory(RepoPatcherUpstream::class.java) { name -> DefaultRepoPatcherUpstream(name, objects, tasks, layout) }
    }

    fun useStandardUpstream(name: String, action: Action<RepoPatcherUpstream>) {
        upstreams {
            register<RepoPatcherUpstream>(name) {
                action.execute(this)
            }
        }
    }
}