val databaseParameters by extra(com.gitlab.davinkevin.podcastserver.backend.build.DatabaseParameters(
        url = "jdbc:postgresql://postgres:5432/podcast-server",
        user = "podcast-server-user",
        password = "nAAdo5wNs7WEF1UxUobpJDfS9Si62PHa",
        sqlFiles = "$buildDir/flyway/migrations/"
))
