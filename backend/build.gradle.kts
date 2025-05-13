import com.gitlab.davinkevin.podcastserver.database.DatabaseConfiguration
import com.gitlab.davinkevin.podcastserver.dockerimages.DockerImagesConfiguration
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.internal.deprecation.DeprecatableConfiguration
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	id("org.springframework.boot") version "3.4.5"
	id("io.spring.dependency-management") version "1.1.7"

	id("com.gorylenko.gradle-git-properties") version "2.5.0"
	id("com.google.cloud.tools.jib") version "3.4.5"
	id("org.jetbrains.kotlinx.kover") version "0.9.1"

	kotlin("jvm") version "2.1.20"
	kotlin("plugin.spring") version "2.1.20"

	id("build-plugin-database")
	id("build-plugin-docker-images")
}

group = "com.github.davinkevin.podcastserver"
version = "2025.5.0"

java {
	sourceCompatibility = JavaVersion.VERSION_21
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

tasks.withType<KotlinCompile> {
	compilerOptions {
		freeCompilerArgs = listOf(
			"-Xjsr305=strict",
		)
		jvmTarget = JvmTarget.JVM_21
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-webflux") {
		exclude("org.springframework.boot:spring-boot-starter-reactor-netty")
	}
	implementation("org.springframework.boot:spring-boot-starter-jooq")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("io.micrometer:micrometer-registry-prometheus")

	implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

	implementation("org.postgresql:postgresql")
	implementation(project(":backend-lib-database"))

	implementation("org.jdom:jdom2:2.0.6.1")
	implementation("org.jsoup:jsoup:1.20.1")

	implementation(project(":backend-lib-youtubedl"))
	implementation("net.bramp.ffmpeg:ffmpeg:0.8.0")

	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	implementation(platform("software.amazon.awssdk:bom:2.31.42"))
	implementation("software.amazon.awssdk:s3")
	implementation("software.amazon.awssdk:netty-nio-client")

	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
	testImplementation("net.javacrumbs.json-unit:json-unit-assertj:4.1.1")
	testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:3.0.1")
}

configure<com.gorylenko.GitPropertiesPluginExtension> {
	dotGitDirectory = project.rootProject.layout.projectDirectory.dir(".git")
	customProperty("git.build.host", "none")
	customProperty("git.build.user.email", "none")
	customProperty("git.build.user.name", "none")
}

normalization {
	runtimeClasspath {
		ignore("git.properties")
	}
}

tasks.test {
	useJUnitPlatform()

	val env = System.getenv()
	val isEnabledByEnv = env["PREDICTIVE_TEST_SELECTION_ENABLED"].toBoolean()
	val isCI = env["CI"].toBoolean()
	val isEnabledOnFeatureBranch = isCI && env["CI_COMMIT_REF_NAME"] != env["CI_DEFAULT_BRANCH"]

	develocity {
		testRetry {
			if (isCI) {
				maxRetries = 3
				failOnPassedAfterRetry = true
			}
		}
		predictiveTestSelection {
			enabled = isEnabledByEnv || isEnabledOnFeatureBranch
		}
	}

	systemProperty("user.timezone", "UTC")
	project.extensions.getByType<DatabaseConfiguration>().apply {
		systemProperty("spring.datasource.url", jdbc().get())
		systemProperty("spring.datasource.username", user.get())
		systemProperty("spring.datasource.password", password.get())
		dependsOn(migrateDbTask)
	}
	jvmArgs = listOf("--add-opens", "java.base/java.time=ALL-UNNAMED")

	testLogging {
		exceptionFormat = TestExceptionFormat.FULL
	}
}

kover {
	reports {
		filters {
			excludes {
				classes("org.springframework.*", "*__*")
			}
		}
	}
}

jib {
	val jibTags = project.extensions.getByType<DockerImagesConfiguration>().tags.toList()

	val (firstTag) = jibTags
	val others = jibTags.drop(1)

	from {
		image = "podcastserver/backend-base-image:$firstTag"
		auth {
			username = System.getenv("DOCKER_IO_USER")
			password = System.getenv("DOCKER_IO_PASSWORD")
		}
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
			.map { runCatching { it.resolve() }.getOrElse { emptySet() } }
			.sumOf { it.size }

		println("Downloaded all dependencies: ${allDeps + buildDeps}")
	}
}

dependencyManagement {
	applyMavenExclusions(false)
}
