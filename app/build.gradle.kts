plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.kaushik.railway"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kaushik.railway"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"

        // RailRadar API key — set in local.properties as railradar.api.key=rg_xxxxx
        // Never commit the real key.
        val localPropsFile = rootProject.file("local.properties")
        val localProps = java.util.Properties()
        if (localPropsFile.exists()) {
            localProps.load(localPropsFile.inputStream())
        }
        val railradarKey = localProps.getProperty("railradar.api.key") ?: "YOUR_RAILRADAR_API_KEY"
        buildConfigField("String", "RAILRADAR_API_KEY", "\"$railradarKey\"")

        // Backend base URL (emulator default). Override in local.properties if needed.
        val backendUrl = localProps.getProperty("backend.base.url") ?: "http://10.0.2.2:8088"
        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.razorpay.checkout)

    // Persistence
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}
