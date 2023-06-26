package net.onelitefeather.gitpatcher

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input


abstract class Git(repo: File) {

    companion object {
        private val LOGGER = Logging.getLogger(Git::class.java)
    }

    @get:Input
    var repo: File = repo
        set(value) {
            field = value
            assert(value.exists())
        }

    @get:Input
    val committerNameOverride: String? = null
    @get:Input
    val committerEmailOverride: String? = null

    private fun decorateEnv(env: MutableMap<String, String>) {
        if (this.committerNameOverride != null) {
            env["GIT_COMMITTER_NAME"] = this.committerNameOverride!!
        }

        if (this.committerEmailOverride != null) {
            env["GIT_COMMITTER_EMAIL"] = this.committerEmailOverride!!
        }
    }

    fun getStatus(): String {
        return run("status", "z").text()
    }

    fun getRef(): String {
        return run("git", "rev-parse", "HEAD").text().lines().first().trim()
    }

    fun run(name: String, vararg input: String): Command {
        val args = arrayOf("git", "--no-pager", name.replace('_', '-'), *input)
        LOGGER.info("gitpatcher: executing {}", args)
        val builder = ProcessBuilder(*args)
        this.decorateEnv(builder.environment())
        builder.directory(repo)
        return Command(builder.start())
    }

    class Command(private val process: Process) {
        fun run(): Int {
            return process.waitFor()
        }

        fun execute() {
            val result = run()
            assert(result == 0) { "Process returned error code" }
        }

        fun text(): String {
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            return reader.readText().trim()
        }
    }
}
