plugins {
    id("queda.android.library")
    id("queda.android.compose")
}

android {
    namespace = "com.luisete.queda.core.designsystem"
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.reflect)
}
