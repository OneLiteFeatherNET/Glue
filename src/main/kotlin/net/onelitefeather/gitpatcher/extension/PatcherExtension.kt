package net.onelitefeather.gitpatcher.extension

import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.tasks.Nested

open class PatcherExtension(project: Project) {

    @Nested val modules: NamedDomainObjectContainer<PatchModule> = project.container(PatchModule::class.java)

    // For Groovy DSL
    fun modules(closure: Closure<Unit>) = modules.configure(closure)

}