plugins {
    alias(libs.plugins.fitnesstracker.android.feature.ui)
}

android {
    namespace = "com.avi.auth.presentation"
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.auth.domain)
}