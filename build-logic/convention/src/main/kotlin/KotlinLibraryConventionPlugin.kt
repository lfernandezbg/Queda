import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.gradle.kotlin.dsl.configure

class KotlinLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.jvm")
                apply("queda.quality")
            }

            extensions.configure<KotlinJvmProjectExtension> {
                jvmToolchain(AndroidSdk.JVM_VERSION)
            }
        }
    }
}
