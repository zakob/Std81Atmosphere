plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.std81atmosphere"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.std81atmosphere"
        minSdk = 28
        targetSdk = 35
        versionCode = 100
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            // Values are provided by the CI workflow environment; without them a
            // local `assembleRelease` just uses an empty (unsigned) config.
            storeFile = file(System.getenv("RELEASE_KEYSTORE_FILE") ?: "release.keystore")
            storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("RELEASE_KEY_ALIAS") ?: ""
            keyPassword = System.getenv("RELEASE_KEY_ALIAS_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    testImplementation(libs.junit)

    debugImplementation(libs.androidx.compose.ui.tooling)
}