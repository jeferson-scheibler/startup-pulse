plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.startuppulse"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.startuppulse"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Vetores em devices antigos
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            // Ativa ofuscação e remoção de recursos — menor APK e mais segurança
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Útil pra instalar debug e release lado a lado e identificar versão
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    // Habilita ViewBinding no projeto Java (adeus findViewById)
    buildFeatures {
        viewBinding = true
    }

    // Java 11 + desugaring para usar java.time, etc.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    // Evita conflitos de licenças/arquivos META-INF em libs (ex.: osmdroid)
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

    // (Opcional) Credentials/GoogleID se usar login mais moderno
    // implementation(libs.credentials)
    // implementation(libs.credentials.play.services.auth)
    // implementation(libs.googleid)

    val navigationVersion = "2.7.7"
    implementation("androidx.navigation:navigation-fragment:$navigationVersion")
    implementation("androidx.navigation:navigation-ui:$navigationVersion")

    implementation("com.google.firebase:firebase-storage")

    // Desugaring (java.time, etc.)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    // Testes
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}