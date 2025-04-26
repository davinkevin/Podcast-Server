@file:Suppress("unused")

package com.gitlab.davinkevin.podcastserver.database

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider

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

        val pg = project.providers.environmentVariablesPrefixedBy("POSTGRES_")
        val dbProperties: Provider<DatabaseProperties> = pg.map {
            when {
                it.containsKey("POSTGRES_DB") && it.containsKey("POSTGRES_USER") && it.containsKey("POSTGRES_PASSWORD") -> DatabaseProperties(
                    url = "postgresql://postgres:5432/${it["POSTGRES_DB"]}",
                    user = it["POSTGRES_USER"]!!,
                    password = it["POSTGRES_PASSWORD"]!!,
                )
                else -> DatabaseProperties(
                    url = "postgresql://postgres:5432/podcast-server",
                    user = it["POSTGRES_ALTERNATE_USER"] ?: "podcast-server-user",
                    password = it["POSTGRES_ALTERNATE_PASSWORD"] ?: "nAAdo5wNs7WEF1UxUobpJDfS9Si62PHa",
                )
            }
        }

        project.extensions.create("databaseConfiguration", DatabaseConfiguration::class.java).apply {
            url.set(dbProperties.map { it.url })
            user.set(dbProperties.map { it.user })
            password.set(dbProperties.map { it.password })
            migrateDbTask.set(flywayMigrate)
        }
    }
}

private data class DatabaseProperties(
    val url: String,
    val user: String,
    val password: String,
)

abstract class DatabaseConfiguration {
    abstract val url: Property<String>
    abstract val user: Property<String>
    abstract val password: Property<String>
    abstract val migrateDbTask: Property<Task>

    fun jdbc(): Provider<String> = url.map { "jdbc:$it" }
}