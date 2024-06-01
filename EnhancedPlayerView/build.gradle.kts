plugins {
    id("com.android.library")
    id("maven-publish")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.better.player"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        multiDexEnabled = true

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.0"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.github.A0ks9"
            artifactId = "EnhancedPlayerView"
            version = "1.0-Beta"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
dependencies {
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation("androidx.mediarouter:mediarouter:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}