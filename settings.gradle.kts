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
    }
}

rootProject.name = "ai-finance-android"

include(":app")

include(":core:designsystem")
include(":core:ui")
include(":core:model")
include(":core:database")
include(":core:data")

include(":feature:home")
include(":feature:transactions")
include(":feature:add_transaction")
include(":feature:statistics")
include(":feature:settings")
include(":feature:category_management")
include(":feature:importer")
include(":feature:ai")
include(":feature:ocr")
include(":feature:scheduled")
