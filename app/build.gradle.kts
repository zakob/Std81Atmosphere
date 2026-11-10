plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.std81atmosphere"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "org.std81atmosphere"
        minSdk = 28
        targetSdk = 35
        versionCode = 101
        versionName = "1.0.1"
    }

    signingConfigs {
        create("release") {
            // Values are provided by the CI workflow environment; paths are
            // resolved from the repository root. Without them a local
            // `assembleRelease` just uses an empty (unsigned) config.
            storeFile = System.getenv("RELEASE_KEYSTORE_FILE")?.let { rootProject.file(it) }
                ?: rootProject.file("release.keystore")
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

    // Name the outputs like the classic per-ABI builds, e.g.
    // Std81Atmosphere-1.0.1-arm64-v8a-release.apk.
    applicationVariants.all {
        val variant = this
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName =
                "Std81Atmosphere-${variant.versionName}-arm64-v8a-${variant.buildType.name}.apk"
        }
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