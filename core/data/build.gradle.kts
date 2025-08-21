plugins {
    alias(libs.plugins.fitnesstracker.android.library)
    alias(libs.plugins.fitnesstracker.jvm.ktor)
}

android {
    namespace = "com.avi.core.data"
}

dependencies {
    implementation(libs.timber)
    implementation(libs.bundles.koin)

    implementation(projects.core.domain)
    implementation(projects.core.database)
    
    // Testing dependencies
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.androidx.test.rules)
    
    // JUnit for testing
    testImplementation("junit:junit:4.13.2")
}