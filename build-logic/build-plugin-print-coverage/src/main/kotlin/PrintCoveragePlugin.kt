package com.gitlab.davinkevin.podcastserver.printcoverage

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jdom2.input.SAXBuilder
import java.io.File
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.math.round

/**
 * Created by kevin on 14/02/2023
 */
class PrintCoveragePlugin: Plugin<Project> {

    override fun apply(project: Project) {
        val reports = project.tasks.withType(JacocoReport::class.java)

        reports.configureEach {r ->
            r.reports { rc ->
                rc.xml.required.set(true)
            }
        }

        val task = project.tasks.create("printCoverage", PrintCoverageTask::class.java)
        task.dependsOn(reports)
    }

}

abstract class PrintCoverageTask: DefaultTask() {

    private val coverageType = "INSTRUCTION"

    @TaskAction
    fun printCoverage() {
        val report = File("${project.buildDir}/reports/jacoco/test/jacocoTestReport.xml").toPath()

        if (!report.exists()) {
            logger.error("Jacoco test report is missing")
            throw GradleException("Jacoco test report is missing.")
        }

        val counter = SAXBuilder().apply {
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        }
            .build(Files.newInputStream(report)).rootElement.children
            .firstOrNull { it.name == "counter" && it.getAttribute("type").value == coverageType }
            ?: throw GradleException("counter of type $coverageType not found.")

        val missed = counter.getAttribute("missed").doubleValue
        val covered = counter.getAttribute("covered").doubleValue

        val coverage = (100 / (missed + covered) * covered).round(2)
        println("Coverage: $coverage%")
    }
}

private fun Double.round(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return round(this * multiplier) / multiplier
}
