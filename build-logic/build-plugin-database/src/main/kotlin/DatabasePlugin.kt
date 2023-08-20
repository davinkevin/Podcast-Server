@file:Suppress("unused")

package com.gitlab.davinkevin.podcastserver.database

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Created by kevin on 26/12/2022
 */
class DatabasePlugin: Plugin<Project> {
    override fun apply(project: Project) {

        val backendLibDb: Project = project.rootProject.allprojects.firstOrNull { it.name == "backend-lib-database" }
            ?: error("Project 'backend-lib-database' not found")

        val flywayMigrate: Task = backendLibDb
            .getTasksByName("flywayMigrate", false)
            .firstOrNull() ?: error("Task 'flywayMigrate' not found")

        val env: Map<String, String> = System.getenv()!!
        val configuration = when {
            env.containsKey("POSTGRES_DB") && env.containsKey("POSTGRES_USER") && env.containsKey("POSTGRES_PASSWORD") -> DatabaseConfiguration(
                url = "postgresql://postgres:5432/${env["POSTGRES_DB"]}",
                user = env["POSTGRES_USER"]!!,
                password = env["POSTGRES_PASSWORD"]!!,
                migrateDbTask = flywayMigrate,
            )

            else -> DatabaseConfiguration(
                url = "postgresql://postgres:5432/podcast-server",
                user = env["PG_ALTERNATE_USER"] ?: "podcast-server-user",
                password = env["PG_ALTERNATE_PASSWORD"] ?: "nAAdo5wNs7WEF1UxUobpJDfS9Si62PHa",
                migrateDbTask = flywayMigrate,
            )
        }

        project.extensions.add(DatabaseConfiguration::class.java, "databaseConfiguration", configuration)
    }

}

data class DatabaseConfiguration(
    val url: String,
    val user: String,
    val password: String,
    val migrateDbTask: Any? = null,
) {
    fun jdbc() = "jdbc:$url"
    fun r2dbc() = "r2dbc:$url"
}
