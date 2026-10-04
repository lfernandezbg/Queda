plugins {
    id("queda.android.feature")
}

android {
    namespace = "com.luisete.queda.feature.settings"
}

dependencies {
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
