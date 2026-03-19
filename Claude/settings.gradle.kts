pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Meta Wearables SDK — uncomment and add credentials when available:
        // maven { url = uri("https://sdk.developer.facebook.com/android/maven") }
    }
}

rootProject.name = "MetaGlassesRecorder"
include(":mobile")
include(":wear")
