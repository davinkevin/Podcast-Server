import com.github.gradle.node.yarn.task.YarnTask

plugins {
  base
  id("com.github.node-gradle.node") version "3.1.1"
}

group = "com.github.davinkevin.podcastserver"
version = "2019.1.0"
description = "frontend-angular"

node {
  download.set(true)
  version.set("9.11.2")
  yarnVersion.set("1.7.0")
}

project.tasks["yarn_test"].dependsOn("yarn")

tasks.register("downloadDependencies") {
  dependsOn("nodeSetup", "yarnSetup", "yarn")
}

tasks.named<YarnTask>("yarn_build") {
  inputs.dir(file("src"))
    .withPropertyName("source")
    .withPathSensitivity(PathSensitivity.RELATIVE)

  outputs.dir(file("$projectDir/dist"))
    .withPropertyName("dist")

  dependsOn("yarn")
}

tasks.named<Delete>("clean") {
  delete.add("node_modules")
  delete.add("dist")
}
