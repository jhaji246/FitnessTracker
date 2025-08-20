@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    `kotlin-dsl`
}

group = "com.avi.convention"

dependencies {
    implementation(libs.android.gradlePlugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.ksp.gradlePlugin)
    implementation(libs.room.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("fitnesstracker.android.application") {
            id = "fitnesstracker.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("fitnesstracker.android.application.compose") {
            id = "fitnesstracker.android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("fitnesstracker.android.application.wear.compose") {
            id = "fitnesstracker.android.application.wear.compose"
            implementationClass = "AndroidApplicationWearComposeConventionPlugin"
        }
        register("fitnesstracker.android.library") {
            id = "fitnesstracker.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("fitnesstracker.android.library.compose") {
            id = "fitnesstracker.android.library.compose"
            implementationClass = "AndroidLibraryComposeConventionPlugin"
        }
        register("fitnesstracker.android.feature.ui") {
            id = "fitnesstracker.android.feature.ui"
            implementationClass = "AndroidFeatureUiConventionPlugin"
        }
        register("fitnesstracker.android.room") {
            id = "fitnesstracker.android.room"
            implementationClass = "AndroidRoomConventionPlugin"
        }
        register("fitnesstracker.android.dynamic.feature") {
            id = "fitnesstracker.android.dynamic.feature"
            implementationClass = "AndroidDynamicFeatureConventionPlugin"
        }
        register("fitnesstracker.jvm.library") {
            id = "fitnesstracker.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("fitnesstracker.jvm.ktor") {
            id = "fitnesstracker.jvm.ktor"
            implementationClass = "JvmKtorConventionPlugin"
        }
    }
}