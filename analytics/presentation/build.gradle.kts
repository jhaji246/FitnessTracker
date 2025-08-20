plugins {
    alias(libs.plugins.fitnesstracker.android.feature.ui)
}

android {
    namespace = "com.avi.analytics.presentation"
}

dependencies {
    implementation(projects.analytics.domain)
}