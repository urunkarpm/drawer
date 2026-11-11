plugins {
    id("drawer.android.library")
    id("drawer.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.urunkarpm.drawer.core.data"

    lint {
        // The baseline freezes the issues that existed before this cleanup so that new
        // ones fail the build. The errors it covers are MissingPermission false positives:
        // these call sites are guarded at runtime by explicit ContextCompat.checkSelfPermission
        // checks (see WeatherRepository fetchLocationTriple) and wrapped in runCatching.
        // Lint cannot follow the boolean guard across function boundaries.
        // NewApi is deliberately NOT baselined: those are real crash risks and are fixed in code.
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))

    implementation(libs.work.runtime.ktx)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
