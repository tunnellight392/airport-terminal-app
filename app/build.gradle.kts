import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// Load signing credentials from keystore.properties (kept out of version control).
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    }
}

android {
    namespace = "com.tunnellight.airport_terminal"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.tunnellight.airport_terminal"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    lint {
        // EditedTargetSdkVersion is an IDE-only prompt that fires once when targetSdk is edited,
        // asking you to confirm you have reviewed that release's behaviour changes. It is not
        // reported by command-line lint and is not a defect.
        //
        // Reviewed when moving targetSdk 36 -> 37: this app declares only INTERNET and
        // ACCESS_NETWORK_STATE (both normal permissions), has no services, receivers, providers
        // or background work, and touches the platform only through HttpURLConnection over HTTPS
        // and app-private filesDir. None of the usual migration hazards (background execution
        // limits, scoped storage, foreground service types, notification permission, exact
        // alarms, receiver export flags) apply.
        //
        // The one behaviour worth tracking is edge-to-edge enforcement, which applies from
        // targetSdk 35 and predates this bump: the headers still clear the status bar with a
        // hardcoded paddingTop of 72dp rather than applying window insets. Re-check this if the
        // layouts change. Re-run the SDK Upgrade Assistant on the next targetSdk bump.
        disable += "EditedTargetSdkVersion"
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
