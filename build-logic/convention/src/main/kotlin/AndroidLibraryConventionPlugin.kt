import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.library")
                apply("org.jetbrains.kotlin.android")
                apply("queda.quality")
                apply("queda.jacoco")
            }

            extensions.configure<LibraryExtension> {
                compileSdk = AndroidSdk.COMPILE_SDK

                defaultConfig {
                    minSdk = AndroidSdk.MIN_SDK
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }

                buildTypes {
                    release {
                        isMinifyEnabled = false
                    }
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.toVersion(AndroidSdk.JVM_VERSION)
                    targetCompatibility = JavaVersion.toVersion(AndroidSdk.JVM_VERSION)
                }
                
                testOptions {
                    unitTests.isIncludeAndroidResources = true
                }
            }
            
            extensions.configure<KotlinAndroidProjectExtension> {
                jvmToolchain(AndroidSdk.JVM_VERSION)
            }
        }
    }
}
