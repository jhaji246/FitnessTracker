import com.android.build.api.dsl.LibraryExtension
import com.avi.convention.ExtensionType
import com.avi.convention.addUiLayerDependencies
import com.avi.convention.configureAndroidCompose
import com.avi.convention.configureBuildTypes
import com.avi.convention.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.kotlin

class AndroidFeatureUiConventionPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        target.run {
            pluginManager.run {
                apply("fitnesstracker.android.library.compose")
            }

            dependencies {
                addUiLayerDependencies(target)
            }
        }
    }
}