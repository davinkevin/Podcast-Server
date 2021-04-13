import com.gitlab.davinkevin.podcastserver.backend.build.DatabaseParameters
import nu.studer.gradle.jooq.JooqEdition.OSS
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.internal.deprecation.DeprecatableConfiguration
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jooq.meta.jaxb.ForcedType
import org.jooq.meta.jaxb.Logging.INFO
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

plugins {
	id("org.springframework.boot") version "2.4.3"
	id("io.spring.dependency-management") version "1.0.10.RELEASE"
	id("com.gorylenko.gradle-git-properties") version "2.2.4"
	id("org.flywaydb.flyway") version "7.3.2"
	id("nu.studer.jooq") version "5.2"
	id("com.google.cloud.tools.jib") version "2.7.0"
	id("de.jansauer.printcoverage") version "2.0.0"

	kotlin("jvm") version "1.4.21"
	kotlin("plugin.spring") version "1.4.21"
	jacoco
}

group = "com.github.davinkevin.podcastserver"
version = "2019.1.0"
java.sourceCompatibility = JavaVersion.VERSION_11

apply(from = "gradle/profile-default.gradle.kts")
apply(from = "gradle/profile-ci.gradle.kts")
apply(from = "gradle/profile-skaffold.gradle.kts")

val db = project.extra["databaseParameters"] as DatabaseParameters

repositories {
	mavenCentral()
	maven { url = uri("https://jitpack.io") }
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-webflux")
	implementation("org.springframework.boot:spring-boot-starter-jooq")
	implementation("org.springframework.boot:spring-boot-starter-actuator")

	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	implementation("org.postgresql:postgresql:42.2.18")
	jooqGenerator("org.postgresql:postgresql:42.2.18")

	implementation("org.jdom:jdom2:2.0.6")
	implementation("org.apache.tika:tika-core:1.24.1")
	implementation("org.jsoup:jsoup:1.13.1")
	implementation("com.github.pedroviniv:youtubedl-java:ef7110605d23eaaae4796312163bcf84c7099311")
	implementation("commons-io:commons-io:2.4")
	implementation("net.bramp.ffmpeg:ffmpeg:0.6.1")

	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.1.0")
	testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.10.0")
	testImplementation("com.github.tomakehurst:wiremock:2.25.1")
	testImplementation("org.awaitility:awaitility:3.1.6")

}

gitProperties {
	dotGitDirectory = "${project.rootDir}/../.git"
}

tasks.register<Copy>("copyMigrations") {
	from("${project.rootDir}/../database/migrations/")
	include("*.sql")
	into(db.sqlFiles)
}

flyway {
	url = db.url
	user = db.user
	password = db.password
	locations = arrayOf("filesystem:${db.sqlFiles}")
}

jooq {

	version.set(dependencyManagement.importedProperties["jooq.version"])
	edition.set(OSS)

	configurations {
		create("main") {
			jooqConfiguration.apply {
				logging = INFO
				jdbc.apply {
					driver = "org.postgresql.Driver"
					url = db.url
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
										.withIncludeExpression("""ITEM\.STATUS""")
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

project.tasks["flywayMigrate"].dependsOn("copyMigrations")
project.tasks["generateJooq"].dependsOn("flywayMigrate")

tasks.jacocoTestReport {
	reports {
		xml.isEnabled = true
		html.isEnabled = true
	}
	executionData(File("$buildDir/jacoco/test.exec"))
	finalizedBy(tasks.printCoverage)
}

tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty("user.timezone", "UTC")
	systemProperty("spring.datasource.url", db.url)
	systemProperty("spring.datasource.username", db.user)
	systemProperty("spring.datasource.password", db.password)

	finalizedBy(tasks.jacocoTestReport)
	testLogging {
		exceptionFormat = TestExceptionFormat.FULL
	}
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf(
				"-Xjsr305=strict",
				"-Xallow-result-return-type"
		)
		jvmTarget = "11"
	}
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
				.map { it.resolve().size }
				.sum()

		val allDeps = configurations
				.filter { it.isCanBeResolved && !it.isDeprecated() }
				.map { it.resolve().size }
				.sum()

		println("Downloaded all dependencies: ${allDeps + buildDeps}")
	}
}
