plugins {
    alias(libs.plugins.fitnesstracker.android.library)
    alias(libs.plugins.fitnesstracker.android.room)
}

android {
    namespace = "com.avi.core.database"
}

dependencies {
    implementation(libs.org.mongodb.bson)
    implementation(libs.bundles.koin)

    implementation(projects.core.domain)
}