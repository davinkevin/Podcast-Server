rootProject.name = "Podcast-Server"

val buildCacheUsernameProperty = "BUILD_CACHE__PODCAST_SERVER__USERNAME"
val buildCachePasswordProperty = "BUILD_CACHE__PODCAST_SERVER__PASSWORD"

val cacheUsername = System.getenv(buildCacheUsernameProperty)
        ?: System.getProperty(buildCacheUsernameProperty)
        ?: error("$buildCacheUsernameProperty is not defined")
val cachePassword = System.getenv(buildCachePasswordProperty)
        ?: System.getProperty(buildCachePasswordProperty)
        ?: error("$buildCachePasswordProperty is not defined")

val isCI = System.getenv("CI") == "true"

buildCache {
    local { isEnabled = !isCI }

    remote<HttpBuildCache> {
        credentials {
            username = cacheUsername
            password = cachePassword
        }
        url = uri("https://build-cache.ci.davinkevin.fr/cache/")
        isPush = isCI
        isEnabled = true
    }
}

include("backend-lib-database")
include("backend-lib-youtubedl")
include("backend")
include("frontend-angular")
include("frontend-angularjs")

includeBuild("build-logic/build-plugin-database") { name = "build-plugin-database" }
includeBuild("build-logic/build-plugin-docker-images") { name = "build-plugin-docker-images" }
includeBuild("build-logic/build-plugin-print-coverage") { name = "build-plugin-print-coverage" }
