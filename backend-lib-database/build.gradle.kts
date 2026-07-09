import nu.studer.gradle.jooq.JooqEdition.OSS
import org.jooq.meta.jaxb.ForcedType
import org.jooq.meta.jaxb.LambdaConverter
import org.jooq.meta.jaxb.Logging.INFO
import nu.studer.gradle.jooq.*
import com.gitlab.davinkevin.podcastserver.database.*
import org.gradle.internal.deprecation.DeprecatableConfiguration

buildscript {
    dependencies {
        classpath("org.flywaydb:flyway-database-postgresql:12.11.0")
    }
}

plugins {
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"

    id("java")

    id("org.flywaydb.flyway") version "12.11.0"
    id("nu.studer.jooq") version "10.2.1"
    id("build-plugin-database")
}

group = "com.gitlab.davinkevin.podcastserver.database"
version = "2026.1.0"

repositories {
    mavenCentral()
}

val db = project.extensions.getByType<DatabaseConfiguration>()

dependencies {
    jooqGenerator("org.postgresql:postgresql")
    compileOnly("org.postgresql:postgresql")
}

abstract class GenerateFlywayExpectedVersion : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val migrations: ConfigurableFileCollection

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val versionRegex = Regex("""^V(\d+)__.*\.sql$""")
        val highest = migrations.files
            .mapNotNull { versionRegex.matchEntire(it.name)?.groupValues?.get(1)?.toInt() }
            .maxOrNull()
            ?: error("No Flyway migration found in src/main/migrations")

        outputFile.get().asFile.writeText("flyway.expected.version=$highest\n")
    }
}

val generateFlywayExpectedVersion = tasks.register<GenerateFlywayExpectedVersion>("generateFlywayExpectedVersion") {
    migrations.from(layout.projectDirectory.dir("src/main/migrations").asFileTree.matching { include("V*.sql") })
    outputFile.set(layout.buildDirectory.file("generated/resources/flyway-expected/flyway-expected.properties"))
}

sourceSets.main {
    resources.srcDir(generateFlywayExpectedVersion.map { it.outputFile.get().asFile.parentFile })
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
					url = db.jdbc().get()
					user = db.user.get()
					password = db.password.get()
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
	url = db.jdbc().get()
	user = db.user.get()
	password = db.password.get()
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

    val buildDeps = buildscript
        .configurations
        .onEach { it.incoming.artifactView { lenient(true) }.artifacts }
        .sumOf { it.resolve().size }

    val allDeps = configurations
        .filter { it.isCanBeResolved && !it.isDeprecated() }
        .onEach { it.incoming.artifactView { lenient(true) }.artifacts }
        .sumOf { it.resolve().size }

    val total = allDeps + buildDeps
    doLast {
        println("Downloaded all dependencies: $total")
    }
}
