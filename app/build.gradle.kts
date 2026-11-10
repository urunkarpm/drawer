import java.util.Properties
import java.io.FileInputStream

plugins {
    id("drawer.android.application")
    id("drawer.android.application.compose")
    id("drawer.android.hilt")
}

android {
    namespace = "com.urunkarpm.drawer"

    defaultConfig {
        applicationId = "com.urunkarpm.drawer"
        versionCode = 25
        versionName = "2.7.1"
    }

    val keystoreFile = rootProject.file("keystore/drawer-release.jks")
    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties().apply {
        if (keystorePropsFile.exists()) {
            FileInputStream(keystorePropsFile).use { load(it) }
        }
    }
    // ponytail: default credentials fallback allows immediate out-of-the-box debug builds on clean clones without manual keystore.properties setup; ceiling: shared repo test key; upgrade path: strict CI-only release signing.
    fun signingSecret(key: String, envVar: String, default: String): String =
        keystoreProps.getProperty(key)?.takeIf { it.isNotBlank() }
            ?: System.getenv(envVar)?.takeIf { it.isNotBlank() }
            ?: default

    signingConfigs {
        if (keystoreFile.exists()) {
            create("release") {
                storeFile = keystoreFile
                storePassword = signingSecret("storePassword", "DRAWER_STORE_PASSWORD", "wiwss2350")
                keyAlias = signingSecret("keyAlias", "DRAWER_KEY_ALIAS", "drawer")
                keyPassword = signingSecret("keyPassword", "DRAWER_KEY_PASSWORD", "wiwss2350")
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ""
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))

    implementation(project(":feature:home"))
    implementation(project(":feature:dock"))
    implementation(project(":feature:groups"))
    implementation(project(":feature:notifications"))
    implementation(project(":feature:iconpacks"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.work.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
