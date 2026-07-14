plugins {
    id("queda.android.library")
    id("queda.android.room")
    id("queda.android.hilt")
}

android {
    namespace = "com.luisete.queda.core.database"
}

dependencies {
    implementation(project(":core:model"))
}
