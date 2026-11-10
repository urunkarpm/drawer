pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Drawer"

include(":app")
include(":core:common")
include(":core:model")
include(":core:designsystem")
include(":core:datastore")
include(":core:database")
include(":core:data")
include(":feature:dock")
include(":feature:groups")
include(":feature:notifications")
include(":feature:iconpacks")
include(":feature:settings")
include(":feature:home")
