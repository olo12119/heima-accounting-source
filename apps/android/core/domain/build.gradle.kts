plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.heima.accounting.domain"
    compileSdk {
        version = release(37) { minorApiLevel = 0 }
    }
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        abortOnError = true
        warningsAsErrors = true
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit4)
}
