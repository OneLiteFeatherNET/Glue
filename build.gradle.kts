plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.2.2"
    id("net.kyori.indra") version "3.1.3"
    id("net.kyori.indra.publishing") version "3.1.3"
    signing
}

group = "dev.onelitefeather.glue"
val baseVersion = "0.0.3"
version = System.getenv("TAG_VERSION") ?: "$baseVersion-dev"

repositories {
    mavenCentral()
    maven("https://eldonexus.de/repository/maven-public/")
}

dependencies {
    implementation("dev.onelitefeather:DiffPatch:1.5.3")
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.0.0.202409031743-r")

}

java {
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    jvmToolchain(11)
}

tasks.withType<AbstractPublishToMaven>().configureEach {
    val signingTasks = tasks.withType<Sign>()
    dependsOn(signingTasks)
}


gradlePlugin {
    vcsUrl.set("https://github.com/OneLiteFeatherNET/Glue")
    website.set("https://github.com/OneLiteFeatherNET/Glue")
    plugins {
        create("Glue") {
            id = "dev.onelitefeather.glue"
            displayName = "Plugin for glue patches together into a git repo"
            description = "Glue is a Git patcher that makes file-based changes to repositories. It combines patch files and a Git sub module to a new state. \n" +
                    "The project is inspired by SpoftSpoon v2 from PaperMC"
            tags = listOf("git", "glue", "paper", "patches")
            implementationClass = "dev.onelitefeather.glue.GluePlugin"
        }
    }
}

indra {
    publishReleasesTo("eldo", "https://eldonexus.de/repository/maven-releases/")
    publishSnapshotsTo("eldo", "https://eldonexus.de/repository/maven-snapshots/")
    javaVersions {
        target(11)
        testWith(11)
    }

    github("OneLiteFeatherNET", "Glue") {
        ci(true)
        publishing(false)
    }
    mitLicense()
    signWithKeyFromPrefixedProperties("onelitefeather")
    configurePublications {
        pom {
            developers {
                developer {
                    id.set("themeinerlp")
                    name.set("Phillipp Glanz")
                    email.set("p.glanz@madfix.me")
                }
            }
        }
    }
}
