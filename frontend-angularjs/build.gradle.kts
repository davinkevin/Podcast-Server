import com.github.gradle.node.npm.task.NpmTask
import java.util.Base64

plugins {
  base
  id("com.github.node-gradle.node") version "3.5.1"
}

group = "com.github.davinkevin.podcastserver"
version = "2023.6.0"
description = "frontend-angularjs"

node {
  download.set(true)
  version.set("6.2.0")
  npmVersion.set("2.15.6")
}

tasks.register("downloadDependencies") {
  dependsOn("nodeSetup", "jspm_install")
}

tasks.register<NpmTask>("jspm_config") {
  args.addAll("run", "--silent", "jspm", "config", "registries.github.auth", System.getenv("JSPM_GITHUB_AUTH_TOKEN"))
  onlyIf { System.getenv("JSPM_GITHUB_AUTH_TOKEN") != null }
  dependsOn("npmInstall")
}

tasks.register<NpmTask>("jspm_install") {
  args.addAll("run", "jspm", "install")

  inputs.files("package.json", "www/config.js")
    .withPathSensitivity(PathSensitivity.RELATIVE)

  outputs.dir(file("$projectDir/www/jspm_packages"))

  dependsOn("jspm_config")
}

tasks.register<NpmTask>("build_app") {
  args.addAll("run", "build-app")

  inputs.dir(file("www/app"))
    .withPropertyName("source")
    .withPathSensitivity(PathSensitivity.RELATIVE)

  outputs.dir(file("$projectDir/target/dist"))
    .withPropertyName("dist")

  dependsOn("downloadDependencies")
}

tasks.named("build") {
  dependsOn("build_app")
}

tasks.register<NpmTask>("skaffold_build") {
  args.addAll("run", "skaffold-build")

  inputs.dir(file("www/app"))
    .withPropertyName("source")
    .withPathSensitivity(PathSensitivity.RELATIVE)

  inputs.files(file("www/index.html"), file("www/config.js"))
    .withPropertyName("index")
    .withPathSensitivity(PathSensitivity.RELATIVE)

  outputs.dir(file("$projectDir/target/dist"))
    .withPropertyName("dist")

  dependsOn("downloadDependencies")
}

tasks.register<NpmTask>("skaffold_watch") {
  args.addAll("run", "skaffold-watch")

  dependsOn("downloadDependencies")
}

tasks.named<Delete>("clean") {
  delete.add("node_modules")
  delete.add("www/jspm_packages")
  delete.add("target/dist")
}
