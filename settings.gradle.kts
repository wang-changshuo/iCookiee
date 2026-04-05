pluginManagement {
    repositories {
        // 加上圆括号，注意是双引号
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
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
