import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport

class JacocoConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("jacoco")

            extensions.configure<JacocoPluginExtension> {
                toolVersion = "0.8.12"
            }

            val fileFilter = listOf(
                "**/R.class",
                "**/R$*.class",
                "**/BuildConfig.*",
                "**/Manifest*.*",
                "**/*Test*.*",
                "android/**/*.*",
                "**/Hilt_*.class",
                "**/Dagger*.*",
                "**/*_Factory*.*",
                "**/*_MembersInjector*.*",
                "**/*_HiltModules*.*",
                "**/*Preview*.*",
                "**/ComposableSingletons$*.*"
            )

            val reportTaskName = "jacocoTestReport"
            
            if (tasks.names.contains(reportTaskName)) {
                tasks.named<JacocoReport>(reportTaskName).configure {
                    configureReport(this, fileFilter)
                }
            } else {
                tasks.register<JacocoReport>(reportTaskName) {
                    configureReport(this, fileFilter)
                }
            }

            val verificationTaskName = "jacocoTestCoverageVerification"
            if (tasks.names.contains(verificationTaskName)) {
                tasks.named<JacocoCoverageVerification>(verificationTaskName).configure {
                    dependsOn(reportTaskName)
                }
            } else {
                tasks.register<JacocoCoverageVerification>(verificationTaskName) {
                    dependsOn(reportTaskName)
                    violationRules {
                        rule {
                            limit {
                                minimum = "0.0".toBigDecimal()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun configureReport(task: JacocoReport, fileFilter: List<String>) {
        val project = task.project
        task.dependsOn(project.tasks.withType<Test>())
        task.reports {
            xml.required.set(true)
            html.required.set(true)
        }

        val mainSrc = "${project.projectDir}/src/main/java"
        val kotlinSrc = "${project.projectDir}/src/main/kotlin"

        task.sourceDirectories.setFrom(project.files(mainSrc, kotlinSrc))
        task.classDirectories.setFrom(
            project.fileTree("${project.buildDir}/intermediates/javac/debug") { exclude(fileFilter) } +
            project.fileTree("${project.buildDir}/tmp/kotlin-classes/debug") { exclude(fileFilter) } +
            project.fileTree("${project.buildDir}/classes/kotlin/main") { exclude(fileFilter) }
        )
        task.executionData.setFrom(project.fileTree(project.buildDir) { 
            include("jacoco/*.exec", "outputs/unit_test_code_coverage/**/*.exec") 
        })
    }
}
