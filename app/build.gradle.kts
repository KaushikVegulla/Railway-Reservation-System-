import java.util.Properties

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
        versionCode = 4
        versionName = "1.3.0"

        // ── Masked secrets: set in local.properties (gitignored) ──
        val localPropsFile = rootProject.file("local.properties")
        val localProps = Properties()
        if (localPropsFile.exists()) {
            localPropsFile.inputStream().use { localProps.load(it) }
        }
        fun prop(key: String, fallback: String) =
            localProps.getProperty(key)?.trim()?.takeIf { it.isNotEmpty() } ?: fallback

        buildConfigField("String", "RAILRADAR_API_KEY", "\"${prop("railradar.api.key", "YOUR_RAILRADAR_API_KEY")}\"")
        buildConfigField("String", "RAZORPAY_KEY_ID", "\"${prop("razorpay.key.id", "rzp_test_XXXXXXXX")}\"")
        buildConfigField("String", "RAZORPAY_KEY_SECRET", "\"${prop("razorpay.key.secret", "YOUR_RAZORPAY_SECRET")}\"")
        buildConfigField("String", "BACKEND_BASE_URL", "\"${prop("backend.base.url", "http://10.0.2.2:8088")}\"")
        val useLocalKeys = prop("use.local.keys", "true").lowercase() in listOf("true", "1", "yes")
        buildConfigField("boolean", "USE_LOCAL_KEYS", if (useLocalKeys) "true" else "false")
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
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
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
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
}
