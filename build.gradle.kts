plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.2.1"
}


group = "dev.onelitefeather.glue"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        mavenContent {
            includeGroup("codechicken")
        }
    }
}

dependencies {
    implementation("codechicken:DiffPatch:1.5.0.30")
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.9.0.202403050737-r")

}

kotlin {
    jvmToolchain(11)
}

gradlePlugin {
    plugins {
        create("Glue") {
            id = "dev.onelitefeather.glue"
            implementationClass = "dev.onelitefeather.glue.GluePlugin"
        }
    }
}

publishing {
    repositories {
    }
}
