import nu.studer.gradle.jooq.JooqEdition.OSS
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.internal.deprecation.DeprecatableConfiguration
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jooq.meta.jaxb.ForcedType
import org.jooq.meta.jaxb.Logging.INFO
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import nu.studer.gradle.jooq.JooqGenerate
import org.flywaydb.gradle.task.FlywayMigrateTask

plugins {
	id("org.springframework.boot") version "2.7.1"
	id("io.spring.dependency-management") version "1.0.11.RELEASE"

	id("com.gorylenko.gradle-git-properties") version "2.4.1"
	id("org.flywaydb.flyway") version "8.5.5"
	id("nu.studer.jooq") version "7.1.1"
	id("com.google.cloud.tools.jib") version "3.2.1"
	id("de.jansauer.printcoverage") version "2.0.0"
	id("org.jetbrains.kotlinx.kover") version "0.5.0"

	kotlin("jvm") version "1.6.21"
	kotlin("plugin.spring") version "1.6.21"
	jacoco
}

group = "com.github.davinkevin.podcastserver"
version = "2022.6.0"
java.sourceCompatibility = JavaVersion.VERSION_11

apply(from = "gradle/profile-default.gradle.kts")
apply(from = "gradle/profile-ci.gradle.kts")
apply(from = "gradle/profile-skaffold.gradle.kts")

@Suppress("UNCHECKED_CAST")
val db = (project.extra["databaseParameters"] as Map<String, String>)
	.let(::DatabaseConfiguration)

repositories {
	mavenCentral()
	maven { url = uri("https://jitpack.io") }
}

extra["jooq.version"] = "3.16.5"
dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-jooq")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("io.micrometer:micrometer-registry-prometheus")

	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	implementation("org.springframework:spring-r2dbc")
	implementation("org.postgresql:postgresql")
	runtimeOnly("org.postgresql:r2dbc-postgresql")
	jooqGenerator("org.postgresql:postgresql:" + dependencyManagement.importedProperties["postgresql.version"])
	jooqGenerator("jakarta.xml.bind:jakarta.xml.bind-api:3.0.1")

	implementation("org.jdom:jdom2:2.0.6")
	implementation("org.jsoup:jsoup:1.13.1")
	implementation("com.github.pedroviniv:youtubedl-java:ef7110605d23eaaae4796312163bcf84c7099311")
	implementation("net.bramp.ffmpeg:ffmpeg:0.6.1")

	implementation("io.r2dbc:r2dbc-pool")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

	implementation(platform("software.amazon.awssdk:bom:2.17.100"))
	implementation("software.amazon.awssdk:s3")
	implementation("software.amazon.awssdk:netty-nio-client")


	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.mockito.kotlin:mockito-kotlin:2.2.11")
	testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.10.0")
	testImplementation("com.github.tomakehurst:wiremock-jre8:2.32.0")
	testImplementation("org.awaitility:awaitility:3.1.6")

}

configure<com.gorylenko.GitPropertiesPluginExtension> {
	(dotGitDirectory as DirectoryProperty).set(projectDir)
	customProperty("git.build.host", "none")
	customProperty("git.build.user.email", "none")
	customProperty("git.build.user.name", "none")
}

tasks.register<Copy>("copyMigrations") {
	from("${project.rootDir}/database/migrations/")
	include("*.sql")
	into(db.sqlFiles)
	outputs.cacheIf { true }
}

normalization {
	runtimeClasspath {
		ignore("git.properties")
	}
}

flyway {
	url = db.jdbc()
	user = db.user
	password = db.password
	locations = arrayOf("filesystem:${db.sqlFiles}")
}

tasks.register<FlywayMigrateTask>("flywayMigrateForJOOQ") {
	dependsOn("copyMigrations")
}

jooq {
	version.set(extra["jooq.version"].toString())
	edition.set(OSS)

	configurations {
		create("main") {
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
								.withUserType("com.github.davinkevin.podcastserver.entity.Status")
								.withConverter("com.github.davinkevin.podcastserver.database.StatusConverter")
								.withIncludeExpression("""ITEM\.STATUS"""),
							ForcedType()
								.withUserType("java.nio.file.Path")
								.withConverter("com.github.davinkevin.podcastserver.database.PathConverter")
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
						directory = "${project.buildDir}/generated/jooq"
					}
				}
			}
		}
	}
}

tasks.named<JooqGenerate>("generateJooq") {
	inputs.dir(file("../database/migrations"))
		.withPropertyName("migrations")
		.withPathSensitivity(PathSensitivity.RELATIVE)

	allInputsDeclared.set(true)

	dependsOn("flywayMigrateForJOOQ")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf(
			"-Xjsr305=strict",
			"-Xallow-result-return-type",
		)
		jvmTarget = "11"
	}
}

jacoco {
	toolVersion = "0.8.7"
}

tasks.register<FlywayMigrateTask>("flywaySetupDbForTests") { dependsOn("copyMigrations") }
tasks.test {
	useJUnitPlatform()
	systemProperty("user.timezone", "UTC")
	systemProperty("spring.r2dbc.url", db.r2dbc())
	systemProperty("spring.r2dbc.username", db.user)
	systemProperty("spring.r2dbc.password", db.password)
	jvmArgs = listOf("--add-opens", "java.base/java.time=ALL-UNNAMED")
	dependsOn(tasks.named("flywaySetupDbForTests"))
	finalizedBy(tasks.jacocoTestReport)
	testLogging {
		exceptionFormat = TestExceptionFormat.FULL
	}
}

tasks.jacocoTestReport {
	reports {
		html.required.value(true)
		xml.required.value(true)
	}
	executionData(layout.buildDirectory.file("jacoco/test.exec"))

	dependsOn(tasks.test)
	finalizedBy(tasks.printCoverage)
}

tasks.printCoverage {
	dependsOn(tasks.jacocoTestReport)

	outputs.upToDateWhen { true }
	outputs.cacheIf { !System.getenv("CI_JOB_STAGE").contains("test") }

	inputs.dir(file(layout.buildDirectory.dir("reports/jacoco")))
		.withPropertyName("reports")
		.withPathSensitivity(PathSensitivity.RELATIVE)
}

jib {
	val imageTags: Set<String>? by project.extra
	val ciTags = imageTags?.toList() ?: emptyList()
	val providedTag = project.findProperty("tag")?.toString()
	val jibTags = when {
		ciTags.isNotEmpty() -> ciTags
		providedTag != null -> listOf(providedTag)
		else -> listOf(OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmssSSS"))!!)
	}

	val (firstTag) = jibTags
	val others = jibTags.drop(1)
	val baseImageTag =  firstTag

	from {
		image = "podcastserver/backend-base-image:${baseImageTag}"
	}
	to {
		image = "podcastserver/backend:${firstTag}"
		tags = others.toSet()
		auth {
			username = System.getenv("DOCKER_IO_USER")
			password = System.getenv("DOCKER_IO_PASSWORD")
		}
	}
}

tasks.register("downloadDependencies") {
	fun Configuration.isDeprecated(): Boolean = when (this) {
		is DeprecatableConfiguration -> resolutionAlternatives != null
		else -> false
	}
	doLast {
		val buildDeps = buildscript
			.configurations
			.sumOf { it.resolve().size }

		val allDeps = configurations
			.filter { it.isCanBeResolved && !it.isDeprecated() }
			.sumOf { it.resolve().size }

		println("Downloaded all dependencies: ${allDeps + buildDeps}")
	}
}

dependencyManagement {
	applyMavenExclusions(false)
}

data class DatabaseConfiguration(val url: String, val user: String, val password: String, val sqlFiles: String) {

	constructor(map: Map<String, String>): this(
		url = map["url"]!!,
		user = map["user"]!!,
		password = map["password"]!!,
		sqlFiles = map["sqlFiles"]!!,
	)

	fun jdbc() = "jdbc:$url"
	fun r2dbc() = "r2dbc:$url"
}
