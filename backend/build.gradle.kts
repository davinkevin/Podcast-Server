import com.gitlab.davinkevin.podcastserver.database.DatabaseConfiguration
import com.gitlab.davinkevin.podcastserver.dockerimages.DockerImagesConfiguration
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.internal.deprecation.DeprecatableConfiguration
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"

	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"

	id("com.gorylenko.gradle-git-properties") version "3.0.3"
	id("com.google.cloud.tools.jib") version "3.5.3"
	id("org.jetbrains.kotlinx.kover") version "0.9.8"

	id("build-plugin-database")
	id("build-plugin-docker-images")
}

group = "com.github.davinkevin.podcastserver"
version = "2026.1.0"

java {
	sourceCompatibility = JavaVersion.VERSION_25
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

tasks.withType<KotlinCompile> {
	compilerOptions {
		freeCompilerArgs.addAll(
				"-Xjsr305=strict",
			"-Xannotation-default-target=param-property"
		)

		jvmTarget = JvmTarget.JVM_25
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.springframework.boot:spring-boot-starter-restclient")
	implementation("org.springframework.boot:spring-boot-starter-jooq")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-webflux") {
		exclude("org.springframework.boot:spring-boot-starter-reactor-netty")
	}
	implementation("io.micrometer:micrometer-registry-prometheus")
	implementation("org.postgresql:postgresql")
	implementation(project(":backend-lib-database"))

	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.jdom:jdom2:2.0.6.1")
	implementation("org.jsoup:jsoup:1.22.2")

	implementation(project(":backend-lib-youtubedl"))
	implementation("net.bramp.ffmpeg:ffmpeg:0.9.2")

	implementation(platform("software.amazon.awssdk:bom:2.44.14"))
	implementation("software.amazon.awssdk:s3")
	implementation("software.amazon.awssdk:netty-nio-client")

	testImplementation("io.projectreactor:reactor-test")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webflux-test")
	testImplementation("org.springframework.boot:spring-boot-starter-restclient-test")
	testImplementation("org.springframework.boot:spring-boot-starter-micrometer-metrics-test")
	testImplementation("org.springframework.boot:spring-boot-starter-jooq-test")
	testImplementation("org.mockito.kotlin:mockito-kotlin:6.3.0")
	testImplementation("net.javacrumbs.json-unit:json-unit-assertj:5.1.1")
	testImplementation("org.wiremock:wiremock-standalone:3.13.2")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configure<com.gorylenko.GitPropertiesPluginExtension> {
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

// Jib plugin (3.5.x) is not yet compatible with the Gradle configuration cache.
// Track: https://github.com/GoogleContainerTools/jib/issues/3132
listOf("jib", "jibDockerBuild", "jibBuildTar").forEach {
	tasks.named(it) { notCompatibleWithConfigurationCache("Jib plugin is not yet configuration-cache compatible") }
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
		.map { runCatching { it.resolve() }.getOrElse { emptySet() } }
		.sumOf { it.size }

	val total = allDeps + buildDeps
	doLast {
		println("Downloaded all dependencies: $total")
	}
}

dependencyManagement {
	applyMavenExclusions(false)
}
