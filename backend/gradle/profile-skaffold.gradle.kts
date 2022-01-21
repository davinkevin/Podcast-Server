val skaffold: String? by project

if (System.getenv("SKAFFOLD") != null || (skaffold?.toBoolean() == true)) {
    val databaseParameters by extra(mapOf(
            "url" to "postgresql://postgres:${System.getenv("DATABASE_PORT")}/${System.getenv("DATABASE_NAME")}",
            "user" to System.getenv("DATABASE_USERNAME"),
            "password" to System.getenv("DATABASE_PASSWORD"),
            "sqlFiles" to "$buildDir/flyway/migrations/"
    ))

    val imageTags by extra(setOf("main"))
}

