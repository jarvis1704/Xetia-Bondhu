import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("kotlin-parcelize")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
}

//for gradle signin report
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("local.properties")

if (keystorePropertiesFile.exists()) {
    FileInputStream(keystorePropertiesFile).use {
        keystoreProperties.load(it)
    }
} else {
    throw GradleException("local.properties not found! Please create it with keystore details.")
}

android {
    namespace = "com.biprangshu.xetiabondhu"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.biprangshu.xetiabondhu"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties.getProperty("storeFile")
                ?: throw GradleException("storeFile not found in local.properties"))
            storePassword = keystoreProperties.getProperty("storePassword")
                ?: throw GradleException("storePassword not found in local.properties")
            keyAlias = keystoreProperties.getProperty("keyAlias")
                ?: throw GradleException("keyAlias not found in local.properties")
            keyPassword = keystoreProperties.getProperty("keyPassword")
                ?: throw GradleException("keyPassword not found in local.properties")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources=true
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.ai)
    implementation(libs.firebase.database)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)



    // Dagger Hilt
    implementation("com.google.dagger:hilt-android:2.51")
    kapt("com.google.dagger:hilt-compiler:2.51")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Kotlin Parcelize
    implementation("org.jetbrains.kotlin:kotlin-parcelize-runtime:2.0.21")

    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.1")
    kapt("androidx.lifecycle:lifecycle-compiler:2.9.1")

    implementation("androidx.navigation:navigation-fragment-ktx:2.9.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.0")
    // For Compose navigation
    implementation("androidx.navigation:navigation-compose:2.9.0")

    //splash screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    //firebase bom
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    //analytics
    implementation("com.google.firebase:firebase-analytics")

    // Credential Manager
    implementation ("androidx.credentials:credentials:1.2.2")
    implementation ("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation ("com.google.android.libraries.identity.googleid:googleid:1.1.0")

    implementation ("com.google.android.gms:play-services-auth:21.3.0")

    //datastore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    implementation("io.coil-kt:coil:2.6.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("androidx.activity:activity-ktx:1.9.0")


    implementation("androidx.compose.material:material-icons-extended:1.7.8")

    implementation("com.google.ai.client.generativeai:generativeai:0.2.2")
}