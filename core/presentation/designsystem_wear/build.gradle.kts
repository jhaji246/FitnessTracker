plugins {
    alias(libs.plugins.fitnesstracker.android.library.compose)
}

android {
    namespace = "com.avi.core.presentation.designsystem_wear"

    defaultConfig {
        minSdk = 30
    }
}

dependencies {
    api(projects.core.presentation.designsystem)

    implementation(libs.androidx.wear.compose.material)
}