plugins {
    id("queda.android.application")
    id("queda.android.compose")
    id("queda.android.hilt")
    alias(libs.plugins.baselineprofile)
}

android {
    namespace = "com.luisete.queda"

    // One Firebase project for all three build types. The production Android
    // registration supplies the app ID and key; E2E never signs in remotely.
    val firebaseConfigFile = file("google-services.json")
    require(firebaseConfigFile.isFile) {
        "Falta app/google-services.json (aplicacion Android com.luisete.queda)."
    }
    val firebaseConfig = groovy.json.JsonSlurper().parse(firebaseConfigFile) as Map<*, *>
    val firebaseProject = firebaseConfig["project_info"] as Map<*, *>
    val firebaseClient =
        (firebaseConfig["client"] as List<*>).map { it as Map<*, *> }.firstOrNull { client ->
            val info = client["client_info"] as Map<*, *>
            val androidInfo = info["android_client_info"] as Map<*, *>
            androidInfo["package_name"] == "com.luisete.queda"
        } ?: error("google-services.json no contiene com.luisete.queda")
    val info = firebaseClient["client_info"] as Map<*, *>
    val keys = firebaseClient["api_key"] as List<*>
    val key = (keys.first() as Map<*, *>)["current_key"] as String

    defaultConfig {
        resValue("string", "google_app_id", info["mobilesdk_app_id"] as String)
        resValue("string", "google_api_key", key)
        resValue("string", "project_id", firebaseProject["project_id"] as String)
    }

    defaultConfig {
        applicationId = "com.luisete.queda"
        testInstrumentationRunner = "com.luisete.queda.QuedaTestRunner"
    }

    buildTypes {
        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
        }
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(project(":core:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:designsystem"))

    implementation(project(":feature:onboarding"))
    implementation(project(":feature:today"))
    implementation(project(":feature:inventory"))
    implementation(project(":feature:shopping"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.hilt.navigation.compose)

    "e2eImplementation"(project(":core:testing"))
    "e2eImplementation"(project(":core:database"))
    "e2eImplementation"(project(":core:domain"))
    "e2eImplementation"(project(":core:model"))
    "e2eImplementation"(libs.androidx.room.runtime)
    baselineProfile(project(":baselineprofile"))

    androidTestImplementation(project(":core:database"))
    androidTestImplementation(libs.androidx.room.runtime)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.hilt.testing)
    kspAndroidTest(libs.hilt.compiler)
}
