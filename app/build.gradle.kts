import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val nativeConfig = Properties().apply {
    val configFile = rootProject.file("local.properties")
    if (configFile.exists()) configFile.inputStream().use { load(it) }
}
fun publicConfig(name: String): String = providers.gradleProperty(name).orNull
    ?: providers.environmentVariable(name).orNull ?: nativeConfig.getProperty(name, "")
fun configLiteral(value: String): String = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""

android {
    namespace = "com.taurotechnologies.godseye.nativeapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.taurotechnologies.godseye.nativeapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        manifestPlaceholders["appLabel"] = "Suvidha Radar"
        // Optional server-hosted Explore catalog (same JSON as assets/explore-catalog.v1.json). Empty: bundled only.
        buildConfigField("String", "EXPLORE_CATALOG_URL", configLiteral(publicConfig("EXPLORE_CATALOG_URL")))
        // Optional Cesium ion token for world terrain on the CesiumJS globe. Empty: free Esri imagery, no terrain.
        buildConfigField("String", "CESIUM_ION_TOKEN", configLiteral(publicConfig("CESIUM_ION_TOKEN")))
        buildConfigField("String", "SUPABASE_URL", configLiteral(publicConfig("SUPABASE_URL")))
        buildConfigField("String", "SUPABASE_ANON_KEY", configLiteral(publicConfig("SUPABASE_ANON_KEY")))
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", configLiteral(publicConfig("GOOGLE_WEB_CLIENT_ID").ifBlank { "456678940843-9jnvlfk0jg3thjtd4gihu6kjsq65fnb6.apps.googleusercontent.com" }))
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-dev"
            manifestPlaceholders["appLabel"] = "Suvidha Radar Local"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.08.00"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.1")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
