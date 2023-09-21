import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.internal.deprecation.DeprecatableConfiguration
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.gitlab.davinkevin.podcastserver.database.*
import com.gitlab.davinkevin.podcastserver.dockerimages.*
import com.gradle.enterprise.gradleplugin.testretry.retry

plugins {
	id("org.springframework.boot") version "3.1.4"
	id("io.spring.dependency-management") version "1.1.3"

	id("com.gorylenko.gradle-git-properties") version "2.4.1"
	id("com.google.cloud.tools.jib") version "3.3.2"
	id("org.jetbrains.kotlinx.kover") version "0.7.3"
	id("org.graalvm.buildtools.native") version "0.9.27"

	kotlin("jvm") version "1.8.22"
	kotlin("plugin.spring") version "1.8.22"

	jacoco

	id("build-plugin-database")
	id("build-plugin-docker-images")
	id("build-plugin-print-coverage")
}

group = "com.github.davinkevin.podcastserver"
version = "2023.9.0"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
	mavenCentral()
}

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
	implementation(project(":backend-lib-database"))

	implementation("org.jdom:jdom2:2.0.6.1")
	implementation("org.jsoup:jsoup:1.15.3")

	implementation(project(":backend-lib-youtubedl"))
	implementation("net.bramp.ffmpeg:ffmpeg:0.7.0")

	implementation("io.r2dbc:r2dbc-pool")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")

	implementation(platform("software.amazon.awssdk:bom:2.20.67"))
	implementation("software.amazon.awssdk:s3")
	implementation("software.amazon.awssdk:netty-nio-client")

	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
	testImplementation("net.javacrumbs.json-unit:json-unit-assertj:2.37.0")
	testImplementation("com.github.tomakehurst:wiremock-jre8-standalone:2.35.0")
	testImplementation("org.awaitility:awaitility:4.2.0")

}

configure<com.gorylenko.GitPropertiesPluginExtension> {
	(dotGitDirectory as DirectoryProperty).set(projectDir)
	customProperty("git.build.host", "none")
	customProperty("git.build.user.email", "none")
	customProperty("git.build.user.name", "none")
}

normalization {
	runtimeClasspath {
		ignore("git.properties")
	}
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs = listOf(
			"-Xjsr305=strict",
			"-Xallow-result-return-type",
		)
		jvmTarget = "17"
	}
}

tasks.test {
	useJUnitPlatform()

	val env = System.getenv()
	val isEnabledByEnv = env["PREDICTIVE_TEST_SELECTION_ENABLED"].toBoolean()
	val isCI = env["CI"].toBoolean()
	val isEnabledOnFeatureBranch = isCI && env["CI_COMMIT_REF_NAME"] != env["CI_DEFAULT_BRANCH"]

	retry {
		if (isCI) {
			maxRetries.set(3)
			failOnPassedAfterRetry.set(true)
		}
	}
	predictiveSelection {
		enabled.set(isEnabledByEnv || isEnabledOnFeatureBranch)
	}

	systemProperty("user.timezone", "UTC")
	project.extensions.getByType<DatabaseConfiguration>().apply {
		systemProperty("spring.r2dbc.url", r2dbc())
		systemProperty("spring.r2dbc.username", user)
		systemProperty("spring.r2dbc.password", password)
		dependsOn(migrateDbTask)
	}
	jvmArgs = listOf("--add-opens", "java.base/java.time=ALL-UNNAMED")

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
	val jibTags = project.extensions.getByType<DockerImagesConfiguration>().tags.toList()

	val (firstTag) = jibTags
	val others = jibTags.drop(1)

	from {
		image = "podcastserver/backend-base-image:$firstTag"
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
