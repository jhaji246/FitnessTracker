package com.avi.convention

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.BuildType
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.DynamicFeatureExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

internal fun Project.configureBuildTypes(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
    extensionType: ExtensionType
) {
    commonExtension.run {
        buildFeatures {
            buildConfig = true
        }

        val apiKey = gradleLocalProperties(rootDir, rootProject.providers).getProperty("API_KEY")
        
        // Warn if API key is not found
        if (apiKey.isNullOrBlank()) {
            logger.warn("API_KEY not found in local.properties. Using default placeholder value.")
        }
        
        // Use a default placeholder if API key is not available
        val finalApiKey = apiKey?.takeIf { it.isNotBlank() } ?: "YOUR_API_KEY_HERE"
        
        when(extensionType) {
            ExtensionType.APPLICATION -> {
                extensions.configure<ApplicationExtension> {
                    buildTypes {
                        debug {
                            configureDebugBuildType(finalApiKey)
                        }
                        release {
                            configureReleaseBuildType(commonExtension, finalApiKey)
                        }
                    }
                }
            }
            ExtensionType.LIBRARY -> {
                extensions.configure<LibraryExtension> {
                    buildTypes {
                        debug {
                            configureDebugBuildType(finalApiKey)
                        }
                        release {
                            configureReleaseBuildType(commonExtension, finalApiKey)
                        }
                    }
                }
            }
            ExtensionType.DYNAMIC_FEATURE -> {
                extensions.configure<DynamicFeatureExtension> {
                    buildTypes {
                        debug {
                            configureDebugBuildType(finalApiKey)
                        }
                        release {
                            configureReleaseBuildType(commonExtension, finalApiKey)
                            isMinifyEnabled = false
                        }
                    }
                }
            }
        }
    }
}

private fun BuildType.configureDebugBuildType(apiKey: String) {
    buildConfigField("String", "API_KEY", "\"$apiKey\"")
    buildConfigField("String", "BASE_URL", "\"https://fitnesstracker.pl-coding.com:8080\"")
}

private fun BuildType.configureReleaseBuildType(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
    apiKey: String
) {
    buildConfigField("String", "API_KEY", "\"$apiKey\"")
    buildConfigField("String", "BASE_URL", "\"https://fitnesstracker.pl-coding.com:8080\"")

    isMinifyEnabled = true
    // Only apply ProGuard rules for application modules
    if (commonExtension is ApplicationExtension) {
        proguardFiles(
            commonExtension.getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}