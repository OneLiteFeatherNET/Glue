package dev.onelitefeather.glue.upstream

import dev.onelitefeather.glue.tasks.CheckoutRepo
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskProvider

interface RepoPatcherUpstream : PatcherUpstream {
    val url: Property<String>
    val ref: Property<String>

    val cloneTaskName: String
    val cloneTask: TaskProvider<CheckoutRepo>

    fun github(owner: String, repo: String): String {
        return "https://github.com/$owner/$repo.git"
    }

}