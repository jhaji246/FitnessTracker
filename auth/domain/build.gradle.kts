plugins {
    alias(libs.plugins.fitnesstracker.jvm.library)
}

dependencies {
    implementation(projects.core.domain)
}