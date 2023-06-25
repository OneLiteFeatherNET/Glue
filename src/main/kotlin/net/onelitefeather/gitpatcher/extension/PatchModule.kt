package net.onelitefeather.gitpatcher.extension

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import java.io.File

abstract class PatchModule {

    @get:Input
    abstract val root: Property<File>

    @get:Input
    abstract val target: Property<File>
    abstract val patches: Property<File>

}