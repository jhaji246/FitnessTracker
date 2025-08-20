plugins {
    alias(libs.plugins.fitnesstracker.android.library)
    alias(libs.plugins.fitnesstracker.jvm.ktor)
}

android {
    namespace = "com.avi.auth.data"
}

dependencies {
    implementation(libs.bundles.koin)

    implementation(projects.auth.domain)
    implementation(projects.core.domain)
    implementation(projects.core.data)
}