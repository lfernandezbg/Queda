plugins {
    id("queda.android.feature")
}

android {
    namespace = "com.luisete.queda.feature.onboarding"
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
