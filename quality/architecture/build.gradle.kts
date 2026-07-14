plugins {
    id("queda.android.library")
}

android {
    namespace = "com.luisete.queda.quality.architecture"
}

dependencies {
    testImplementation(libs.archunit)
    testImplementation(libs.junit)
    
    testImplementation(project(":core:model"))
    testImplementation(project(":core:domain"))
    testImplementation(project(":core:data"))
    testImplementation(project(":core:database"))
}
