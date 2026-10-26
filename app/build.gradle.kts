plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "org.lynos"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.lynos"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            // CI signs with a disposable key so the artifact can be installed for testing.
            // For durable updates, configure a private, persistent release keystore.
            val ciStore = System.getenv("LYNOS_KEYSTORE")
            if (!ciStore.isNullOrBlank()) {
                signingConfig = signingConfigs.create("ciRelease") {
                    storeFile = file(ciStore)
                    storePassword = System.getenv("LYNOS_STORE_PASSWORD")
                    keyAlias = System.getenv("LYNOS_KEY_ALIAS")
                    keyPassword = System.getenv("LYNOS_KEY_PASSWORD")
                }
            }
        }
    }

    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
