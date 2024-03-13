plugins {
    id("com.android.library")
    id("kotlin-android")
}

group = 'com.github.FabitMobile'

android {
    namespace = "ru.fabit.remoteservicecoroutines"
    compileSdk = 33
    defaultConfig {
        minSdk = 26
        versionCode = 2
        versionName = "1.1.0"
        resourceConfigurations += mutableSetOf("en", "ru")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
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
        jvmTarget = JavaVersion.VERSION_17.toString()
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.21")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("com.github.FabitMobile:exception:1.0.1")
    implementation("com.github.ihsanbal:LoggingInterceptor:3.1.0")
//    {
//        exclude group: 'org.json', module: 'json'
//    }
}