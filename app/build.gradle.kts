plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.foriad.telegramdrive"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.foriad.telegramdrive"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.0.1"
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
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // Networking (Telegram Bot API is plain HTTPS/JSON + multipart)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Google Mobile Ads SDK (AdMob) - see AdsConfig.kt for ad unit IDs
    implementation("com.google.android.gms:play-services-ads:23.2.0")
    // Google Play Billing - $10 lifetime Premium (removes all ads forever).
    // Product ID "premium_lifetime" - see PremiumConfig.kt and README.md
    // ("Premium - $10 forever"). NOTE: Play Billing only works in the
    // Google Play version of the app; sideloaded APKs get a friendly
    // "not available" message (see BillingManager.kt).
    implementation("com.android.billingclient:billing:7.1.1")
}
