import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.newsbite"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.newsbite"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Load API key from local.properties
        val localProperties = project.rootProject.file("local.properties")
        val properties = Properties()
        if (localProperties.exists()) {
            properties.load(localProperties.inputStream())
        }
        buildConfigField("String", "NEWS_API_KEY", "\"${properties.getProperty("NEWS_API_KEY", "")}\"")
        buildConfigField("String", "GEMINI_API_KEY", "\"${properties.getProperty("GEMINI_API_KEY", "")}\"")
    }
    
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }

    signingConfigs {
        create("release") {
            // Configure these in local.properties or CI/CD environment
            val localProperties = project.rootProject.file("local.properties")
            val properties = Properties()
            if (localProperties.exists()) {
                properties.load(localProperties.inputStream())
            }
            
            storeFile = file(properties.getProperty("KEYSTORE_FILE", "keystore.jks"))
            storePassword = properties.getProperty("KEYSTORE_PASSWORD", "")
            keyAlias = properties.getProperty("KEY_ALIAS", "")
            keyPassword = properties.getProperty("KEY_PASSWORD", "")
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        
        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Uncomment when you have signing configured
            // signingConfig = signingConfigs.getByName("release")
        }
    }
    
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

android {
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.swiperefreshlayout)
    
    // Splash Screen API
    implementation(libs.splashscreen)
    
    // ViewModel & LiveData
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    
    // Retrofit + Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    
    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    ksp(libs.room.compiler)
    
    // Paging 3
    implementation(libs.paging.runtime)
    implementation(libs.paging.common)
    
    // Shimmer
    implementation(libs.shimmer)
    
    // Picasso
    implementation(libs.picasso)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    
    // Gemini AI
    implementation(libs.generativeai)
    
    testImplementation(libs.junit)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.property)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.room.testing)
    testImplementation(libs.paging.testing)
    testImplementation(libs.paging.common)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
