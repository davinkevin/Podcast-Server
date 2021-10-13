rootProject.name = "backend"

plugins {
    id("com.gradle.enterprise") version("3.7")
    id("com.gradle.common-custom-user-data-gradle-plugin") version "1.4.2"
}

val isCI = System.getenv("CI") == "true"

gradleEnterprise {
    server = "https://ge.3.227.211.215.nip.io"
    allowUntrustedServer = true

    buildScan {
        publishAlways()
        allowUntrustedServer = true

        capture {
            isTaskInputFiles = true
            isUploadInBackground = !isCI
        }
    }

}

buildCache {
    local { isEnabled = !isCI }
    remote<HttpBuildCache> {
        url = uri("https://ge.3.227.211.215.nip.io/cache/")
        isPush = isCI
        isEnabled = true
        isAllowUntrustedServer = true
    }
}
