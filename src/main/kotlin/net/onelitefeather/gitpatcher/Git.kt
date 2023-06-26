package net.onelitefeather.gitpatcher

import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Input


abstract class Git(repo: File) {

    companion object {
        private val LOGGER = Logging.getLogger(Git::class.java)
        private const val GIT_COMMITTER_NAME = "GIT_COMMITTER_NAME"
        private const val GIT_COMMITTER_EMAIL = "GIT_COMMITTER_EMAIL"
        private const val STATUS = "STATUS"
        private const val GIT = "GIT"
        private const val REV_PARSE = "rev-parse"
        private const val HEAD = "HEAD"
        private const val NO_PAGER = "--no-pager"
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

    /**
     * Adds env variables to the given env map
     * @param env Map of env variables
     */
    private fun decorateEnv(env: MutableMap<String, String>) {
        if (this.committerNameOverride != null) {
            env[GIT_COMMITTER_NAME] = this.committerNameOverride!!
        }

        if (this.committerEmailOverride != null) {
            env[GIT_COMMITTER_EMAIL] = this.committerEmailOverride!!
        }
    }

    /**
     * Checks status between local and origin branch
     * @return Git status
     */
    fun getStatus(): String {
        return run(STATUS, "z").text()
    }

    /**
     * Gets current git commit hash
     * @return Current git commit hash
     */
    fun getRef(): String {
        return run(REV_PARSE, HEAD).text().lines().first().trim()
    }

    /**
     * Executes a git command
     * @param name Command name
     * @param input Command args
     * @return Command
     */
    private fun run(name: String, vararg input: String): Command {
        val args = arrayOf(GIT, NO_PAGER, name.replace('_', '-'), *input)
        LOGGER.info("gitpatcher: executing {}", args)
        val builder = ProcessBuilder(*args)
        this.decorateEnv(builder.environment())
        builder.directory(repo)
        return Command(builder.start())
    }

    class Command(private val process: Process) {

        /**
         * Runs the command
         * @return Exit value of the process
         */
        fun run(): Int {
            return process.waitFor()
        }

        /**
         * Executes the command
         * @throws AssertionError When exit code is not 0
         */
        fun execute() {
            val result = run()
            assert(result == 0) { "Process returned error code" }
        }

        /**
         * Gets the text from the process
         * @return Text from the process
         */
        fun text(): String {
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            return reader.readText().trim()
        }
    }
}
