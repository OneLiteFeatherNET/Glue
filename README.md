# Glue
A code patcher that glues source code changes together with patches

## How to use in your project:

Add this in your `build.gradle.kts`:
```kt
plugins {
    id("dev.onelitefeather.glue") version "<current-version>"
}
group = "me.yourgroupname.yourplugin"
version = "1.0.0-SNAPSHOT"

glue {
    upstreams {
        useStandardUpstream("YourProjectOrUpstreamName") {
            // This line sets the git clone url, for example: https://github.com/organisation/repository.git
            url.set(github("organisation", "repository"))

            // Always use the long git commit hash to check out the wanted state
            ref = "<commit-sha1-hash>"

            // This is where the user changes are
            patchDir =  layout.projectDirectory.dir("patches")

            // This is the base repository to generate the patches (diffs are made in the background against the output dir)
            upstreamDir = layout.projectDirectory.dir("upstream")

            // The repository with all included changes also called work directory
            outputDir = layout.projectDirectory.dir("downstream")
        }
    }
}
```

Inspired by [paperweight](https://github.com/PaperMC/paperweight/tree/softspoon-v2) 
