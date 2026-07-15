import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.android")
                apply("queda.quality")
            }

            extensions.configure<ApplicationExtension> {
                compileSdk = AndroidSdk.COMPILE_SDK

                defaultConfig {
                    applicationId = "com.luisete.queda"
                    minSdk = AndroidSdk.MIN_SDK
                    targetSdk = AndroidSdk.TARGET_SDK
                    versionCode = 1
                    versionName = "1.0"

                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    vectorDrawables {
                        useSupportLibrary = true
                    }
                }

                buildFeatures {
                    buildConfig = true
                }

                buildTypes {
                    release {
                        isMinifyEnabled = true
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro"
                        )
                    }
                    debug {
                        applicationIdSuffix = ".debug"
                        isDebuggable = true
                        isMinifyEnabled = false
                    }
                    create("e2e") {
                        initWith(getByName("debug"))
                        applicationIdSuffix = ".e2e"
                        isDebuggable = true
                        isMinifyEnabled = false
                        matchingFallbacks += listOf("debug")
                    }
                }

                compileOptions {
                    sourceCompatibility = JavaVersion.toVersion(AndroidSdk.JVM_VERSION)
                    targetCompatibility = JavaVersion.toVersion(AndroidSdk.JVM_VERSION)
                }

                packaging {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                    }
                }
            }
            
            extensions.configure<KotlinAndroidProjectExtension> {
                jvmToolchain(AndroidSdk.JVM_VERSION)
            }
        }
    }
}
