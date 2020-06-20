package com.gitlab.davinkevin.podcastserver.backend.build

import org.gradle.api.Project

data class DatabaseParameters(
        val url: String,
        val user: String,
        val password: String,
        val sqlFiles: List<String> = listOf("filesystem:../database/migrations")
)

fun databaseParameters(project: Project): DatabaseParameters {
    return project.extensions.extraProperties["databaseParameters"] as DatabaseParameters
}
