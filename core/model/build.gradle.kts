plugins {
    id("queda.kotlin.library")
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotlinx.coroutines.test)
}
