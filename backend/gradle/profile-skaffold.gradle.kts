import com.gitlab.davinkevin.podcastserver.backend.build.*

val skaffold: String? by project

if (System.getenv("SKAFFOLD") != null || (skaffold?.toBoolean() == true)) {
    val databaseParameters by extra(DatabaseParameters(
            url = "jdbc:postgresql://postgres:${System.getenv("DATABASE_PORT")}/${System.getenv("DATABASE_NAME")}",
            user = System.getenv("DATABASE_USERNAME"),
            password = System.getenv("DATABASE_PASSWORD"),
            sqlFiles = "$buildDir/flyway/migrations/"
    ))

    val imageTags by extra(setOf("master"))
}

