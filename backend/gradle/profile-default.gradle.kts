val env: Map<String, String> = System.getenv()!!

val databaseParameters by extra(mapOf(
        "url" to "postgresql://postgres:5432/podcast-server",
        "user" to (env["PG_ALTERNATE_USER"] ?: "podcast-server-user"),
        "password" to (env["PG_ALTERNATE_PASSWORD"] ?: "nAAdo5wNs7WEF1UxUobpJDfS9Si62PHa"),
        "sqlFiles" to "$buildDir/flyway/migrations/"
))
