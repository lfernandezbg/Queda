plugins {
    id("queda.android.library")
    id("queda.android.hilt")
}

android {
    namespace = "com.luisete.queda.core.testing"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    
    implementation(libs.junit)
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.androidx.room.testing)
    implementation(libs.hilt.testing)
}
