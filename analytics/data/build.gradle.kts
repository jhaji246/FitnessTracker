plugins {
    alias(libs.plugins.fitnesstracker.android.library)
    alias(libs.plugins.fitnesstracker.android.room)
}

android {
    namespace = "com.avi.analytics.data"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.bundles.koin)

    implementation(projects.core.database)
    implementation(projects.core.domain)
    implementation(projects.analytics.domain)
}