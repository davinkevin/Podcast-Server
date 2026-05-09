import com.github.gradle.node.npm.task.NpmTask

plugins {
  base
  id("com.github.node-gradle.node") version "7.1.0"
}

group = "com.github.davinkevin.podcastserver"
version = "2026.1.0"
description = "frontend-v3"

node {
  download.set(true)
  version.set("20.19.4")
}

tasks.register("downloadDependencies") {
  dependsOn("nodeSetup", "npmSetup", "npmInstall")
}

tasks.named<NpmTask>("npm_run_build") {
  inputs.dir(file("src")).withPropertyName("source").withPathSensitivity(PathSensitivity.RELATIVE)
  inputs.file("package.json").withPathSensitivity(PathSensitivity.RELATIVE)
  inputs.file("package-lock.json").withPathSensitivity(PathSensitivity.RELATIVE)
  inputs.file("angular.json").withPathSensitivity(PathSensitivity.RELATIVE)
  inputs.file("tsconfig.json").withPathSensitivity(PathSensitivity.RELATIVE)
  inputs.file("tsconfig.app.json").withPathSensitivity(PathSensitivity.RELATIVE)
  inputs.dir(file("public")).withPropertyName("public").withPathSensitivity(PathSensitivity.RELATIVE)
  outputs.dir(file("$projectDir/dist")).withPropertyName("dist")
  dependsOn("npmInstall")
}

tasks.named<NpmTask>("npm_run_test") {
  dependsOn("npmInstall")
}

tasks.named("build") {
  dependsOn("npm_run_build")
}

tasks.named<Delete>("clean") {
  delete.add("node_modules")
  delete.add("dist")
  delete.add("coverage")
  delete.add(".angular")
}
