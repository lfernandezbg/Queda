import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.jlleitschuh.gradle.ktlint.KtlintPlugin

class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(io.gitlab.arturbosch.detekt.DetektPlugin::class.java)
            pluginManager.apply(KtlintPlugin::class.java)

            extensions.configure<KtlintExtension> {
                android.set(true)
                outputToConsole.set(true)
                ignoreFailures.set(false)
                reporters {
                    reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.PLAIN)
                    reporter(org.jlleitschuh.gradle.ktlint.reporter.ReporterType.CHECKSTYLE)
                }
            }

            extensions.configure<DetektExtension> {
                toolVersion = "1.23.8"
                // Basic configuration
            }

            tasks.withType<Detekt>().configureEach {
                reports {
                    html.required.set(true)
                    xml.required.set(true)
                }
            }
        }
    }
}
