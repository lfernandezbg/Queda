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

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
