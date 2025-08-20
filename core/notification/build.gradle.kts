plugins {
    alias(libs.plugins.fitnesstracker.android.library)
}

android {
    namespace = "com.avi.core.notification"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.bundles.koin)

    implementation(projects.core.domain)
    implementation(projects.core.presentation.ui)
    implementation(projects.core.presentation.designsystem)
}