import com.github.gradle.node.util.Platform
import com.github.gradle.node.yarn.task.YarnTask

plugins {
  base
  id("com.github.node-gradle.node") version "7.1.0"
}

group = "com.github.davinkevin.podcastserver"
version = "2026.1.0"
description = "frontend-angular"

node {
  download.set(true)
  version.set("9.11.2")
  yarnVersion.set("1.7.0")

  // Node < 16 has no darwin-arm64 binary; force x64 on Apple Silicon (runs via Rosetta 2).
  // Both `resolvedPlatform` and `resolvedNodeDir` must be overridden because the plugin reads
  // the platform eagerly into `resolvedNodeDir` during apply, before this block runs.
  val osName = System.getProperty("os.name").lowercase()
  val osArch = System.getProperty("os.arch").lowercase()
  if (osName.contains("mac") && (osArch == "aarch64" || osArch == "arm64")) {
    resolvedPlatform.set(Platform("darwin", "x64"))
    resolvedNodeDir.set(workDir.zip(version) { wd, v -> wd.dir("node-v$v-darwin-x64") })
  }
}

tasks.named("yarn_test") {
  dependsOn("yarn")
}

tasks.register("downloadDependencies") {
  dependsOn("nodeSetup", "yarnSetup", "yarn")
}

tasks.named<YarnTask>("yarn") {
  args.addAll("--network-timeout", "100000")
}

tasks.named<YarnTask>("yarn_build") {
  inputs.dir(file("src"))
    .withPropertyName("source")
    .withPathSensitivity(PathSensitivity.RELATIVE)

  outputs.dir(file("$projectDir/dist"))
    .withPropertyName("dist")

  dependsOn("yarn")
}

tasks.named("build") {
  dependsOn("yarn_build")
}

tasks.named<Delete>("clean") {
  delete.add("node_modules")
  delete.add("dist")
}
