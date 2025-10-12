plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
    // ADICIONADO: Aplica o plugin Safe Args
    alias(libs.plugins.androidx.navigation.safeargs)
}

android {
    namespace = "com.example.startuppulse"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.startuppulse"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
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
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES"
            )
        }
    }
}

dependencies {
    // UI base
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.activity)
    implementation(libs.androidx.swiperefreshlayout)
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Firebase (BOM centraliza versões)
    implementation(platform(libs.firebase.bom))
    implementation(libs.google.firebase.auth)
    implementation(libs.firebase.firestore)

    // Google Sign-In / Location
    implementation(libs.play.services.auth)
    implementation(libs.play.services.location)

    // Imagens e animações
    implementation(libs.glide)
    implementation(libs.firebase.config)
    implementation(libs.androidx.preference)
    implementation(libs.cronet.embedded)
    annotationProcessor(libs.compiler)
    implementation(libs.lottie)

    // Mapas
    implementation(libs.osmdroid.android)

    // Networking
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)

    // CORRIGIDO: Dependências de Navegação via catálogo
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    implementation("com.google.firebase:firebase-storage")

    // Desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}