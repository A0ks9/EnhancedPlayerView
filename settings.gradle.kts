@file:Suppress("UnstableApiUsage")
import org.gradle.api.initialization.resolve.RepositoriesMode.*

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        mavenLocal()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo1.maven.org/maven2") }
        maven { url = uri("https://mvnrepository.com/repos/central") }
        maven { url = uri("https://storage.googleapis.com/r8-releases/raw") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo1.maven.org/maven2") }
        maven { url = uri("https://mvnrepository.com/repos/central") }
        maven { url = uri("https://storage.googleapis.com/r8-releases/raw") }
    }
}

rootProject.name = "exoplayer"

include(":EnhancedPlayerView", ":app")