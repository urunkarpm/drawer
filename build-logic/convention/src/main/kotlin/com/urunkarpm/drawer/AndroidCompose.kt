package com.urunkarpm.drawer

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

internal fun Project.configureAndroidComposeApp(
    extension: ApplicationExtension,
) {
    extension.buildFeatures.compose = true
    addComposeDependencies()
}

internal fun Project.configureAndroidComposeLibrary(
    extension: LibraryExtension,
) {
    extension.buildFeatures.compose = true
    addComposeDependencies()
}

private fun Project.addComposeDependencies() {
    val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
    dependencies {
        val bom = libs.findLibrary("compose-bom").get()
        add("implementation", platform(bom))
        add("androidTestImplementation", platform(bom))
        add("implementation", libs.findLibrary("compose-ui").get())
        add("implementation", libs.findLibrary("compose-ui-graphics").get())
        add("implementation", libs.findLibrary("compose-ui-tooling-preview").get())
        add("implementation", libs.findLibrary("compose-material3").get())
        add("debugImplementation", libs.findLibrary("compose-ui-tooling").get())
        add("debugImplementation", libs.findLibrary("compose-ui-test-manifest").get())
    }
}
