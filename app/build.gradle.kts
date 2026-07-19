plugins {
    id("queda.android.application")
    id("queda.android.compose")
    id("queda.android.hilt")
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.luisete.queda"

    defaultConfig {
        applicationId = "com.luisete.queda"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
        }
    }
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))

    implementation(project(":feature:onboarding"))
    implementation(project(":feature:today"))
    implementation(project(":feature:inventory"))
    implementation(project(":feature:shopping"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    "e2eImplementation"(project(":core:testing"))
    "e2eImplementation"(project(":core:database"))
    "e2eImplementation"(libs.androidx.room.runtime)
    baselineProfile(project(":baselineprofile"))
}
