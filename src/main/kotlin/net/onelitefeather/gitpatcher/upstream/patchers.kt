package net.onelitefeather.gitpatcher.upstream

import net.onelitefeather.gitpatcher.tasks.CheckoutRepo
import net.onelitefeather.gitpatcher.utils.capitalized
import net.onelitefeather.gitpatcher.utils.providerFor
import org.gradle.api.file.ProjectLayout
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.property

open class DefaultRepoPatcherUpstream(
    name: String,
    objects: ObjectFactory,
    tasks: TaskContainer,
    protected val layout: ProjectLayout
) : DefaultPatcherUpstream(name, objects, tasks), RepoPatcherUpstream {

    override val url: Property<String> = objects.property()
    override val ref: Property<String> = objects.property()

    override val cloneTaskName: String
        get() = "clone${name.capitalized()}Repo"
    override val cloneTask: TaskProvider<CheckoutRepo>
        get() = tasks.providerFor(cloneTaskName)
}