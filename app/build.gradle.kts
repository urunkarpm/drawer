plugins {
    id("drawer.android.application")
    id("drawer.android.application.compose")
    id("drawer.android.hilt")
}

android {
    namespace = "com.urunkarpm.drawer"

    defaultConfig {
        applicationId = "com.urunkarpm.drawer"
        versionCode = 3
        versionName = "1.1.1"
    }

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("keystore/drawer-release.jks")
            storePassword = "DrawerReleaseKey2026!"
            keyAlias = "drawer"
            keyPassword = "DrawerReleaseKey2026!"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            applicationIdSuffix = ""
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
