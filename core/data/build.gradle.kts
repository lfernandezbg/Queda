plugins {
    id("queda.android.library")
    id("queda.android.hilt")
}

android {
    namespace = "com.luisete.queda.core.data"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:database"))
    
    implementation(libs.androidx.datastore.preferences)
}
