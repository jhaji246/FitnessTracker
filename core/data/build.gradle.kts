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
}