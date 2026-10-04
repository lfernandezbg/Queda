plugins {
    id("queda.android.library")
    id("queda.android.hilt")
}

android {
    namespace = "com.luisete.queda.core.data"
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:database"))

    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    testImplementation(project(":core:testing"))
    androidTestImplementation(project(":core:testing"))
}
