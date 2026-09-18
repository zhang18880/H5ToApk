plugins {
    id("com.android.application")
}

android {
    namespace = "cn.zhangjb.h5pack"
    compileSdk = 34

    defaultConfig {
        applicationId = "cn.zhangjb.h5pack"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}


dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    // WebViewAssetLoader 依赖
    implementation("androidx.webkit:webkit:1.8.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}

