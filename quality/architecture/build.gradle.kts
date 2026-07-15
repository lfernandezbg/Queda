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
    testImplementation(project(":core:database"))
    testImplementation(project(":core:data"))
    testImplementation(project(":core:designsystem"))
    testImplementation(project(":core:testing"))
    testImplementation(project(":feature:onboarding"))
    testImplementation(project(":feature:today"))
    testImplementation(project(":feature:inventory"))
    testImplementation(project(":feature:shopping"))
    testImplementation(project(":feature:settings"))
}

tasks.withType<Test>().configureEach {
    systemProperty("project.root", rootProject.projectDir.absolutePath)
}
