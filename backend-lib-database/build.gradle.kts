import nu.studer.gradle.jooq.JooqEdition.OSS
import org.jooq.meta.jaxb.ForcedType
import org.jooq.meta.jaxb.LambdaConverter
import org.jooq.meta.jaxb.Logging.INFO
import nu.studer.gradle.jooq.*
import com.gitlab.davinkevin.podcastserver.database.*
import org.gradle.internal.deprecation.DeprecatableConfiguration

buildscript {
    dependencies {
        classpath("org.flywaydb:flyway-database-postgresql:11.7.0")
    }
}

plugins {
    id("org.springframework.boot") version "3.4.4"
    id("io.spring.dependency-management") version "1.1.7"

    id("java")

    id("org.flywaydb.flyway") version "11.7.0"
    id("nu.studer.jooq") version "10.0"
    id("build-plugin-database")
}

group = "com.gitlab.davinkevin.podcastserver.database"
version = "2025.4.0"

repositories {
    mavenCentral()
}

val db = project.extensions.getByType<DatabaseConfiguration>()

dependencies {
    jooqGenerator("org.postgresql:postgresql")
    compileOnly("org.postgresql:postgresql")
}

jooq {
    version = dependencyManagement.importedProperties["jooq.version"]
    edition = OSS

    configurations {
        create("main") {
            generateSchemaSourceOnCompilation = false
            jooqConfiguration.apply {
                logging = INFO
                jdbc.apply {
					driver = "org.postgresql.Driver"
					url = db.jdbc()
					user = db.user
					password = db.password
                }

                generator.apply {
                    name = "org.jooq.codegen.DefaultGenerator"
                    database.apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public"
                        forcedTypes = listOf(
                            ForcedType()
                                .withUserType("java.nio.file.Path")
                                .withLambdaConverter(
                                    LambdaConverter()
                                        .withFrom("Path::of")
                                        .withTo("Path::toString")
                                )
                                .withIncludeExpression("""ITEM\.FILE_NAME""")
                        )
                        isIncludeTables = true
                        isIncludePackages = false
                        isIncludeUDTs = true
                        isIncludeSequences = true
                        isIncludePrimaryKeys = true
                        isIncludeUniqueKeys = true
                        isIncludeForeignKeys = true
                    }

                    target.apply {
                        packageName = "com.github.davinkevin.podcastserver.database"
                        directory = "${project.projectDir}/src/main/java"
                    }
                }
            }
        }
    }
}

flyway {
	url = db.jdbc()
	user = db.user
	password = db.password
	locations = arrayOf("filesystem:$projectDir/src/main/migrations/")
    cleanDisabled = false

}

tasks.named<JooqGenerate>("generateJooq") {
	inputs.dir(file("src/main/migrations"))
		.withPropertyName("migrations")
		.withPathSensitivity(PathSensitivity.RELATIVE)

	allInputsDeclared = false
    outputs.cacheIf { false }

	dependsOn("flywayMigrate")
}

tasks.register("downloadDependencies") {
    fun Configuration.isDeprecated(): Boolean = when (this) {
        is DeprecatableConfiguration -> resolutionAlternatives.isNotEmpty()
        else -> false
    }
    doLast {
        val buildDeps = buildscript
            .configurations
            .onEach { it.incoming.artifactView { lenient(true) }.artifacts }
            .sumOf { it.resolve().size }

        val allDeps = configurations
            .filter { it.isCanBeResolved && !it.isDeprecated() }
            .onEach { it.incoming.artifactView { lenient(true) }.artifacts }
            .sumOf { it.resolve().size }

        println("Downloaded all dependencies: ${allDeps + buildDeps}")
    }
}
