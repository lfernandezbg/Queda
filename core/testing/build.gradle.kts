plugins {
    id("queda.android.library")
}

android {
    namespace = "com.luisete.queda.core.testing"
}

dependencies {
    testImplementation(libs.junit)
}
