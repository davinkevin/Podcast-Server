val databaseParameters by extra(mapOf(
        "url" to "jdbc:postgresql://postgres:5432/podcast-server",
        "user" to "podcast-server-user",
        "password" to "nAAdo5wNs7WEF1UxUobpJDfS9Si62PHa",
        "sqlFiles" to "$buildDir/flyway/migrations/"
))
