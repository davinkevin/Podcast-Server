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

include("backend")
include("frontend-angular")
include("frontend-angularjs")
