plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.r42914lg.blesandbox.blewrapper"
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.nordic.ble)
    implementation(libs.nordic.ble.ktx)
    implementation(libs.nordic.ble.common)
    implementation(libs.koin.core)
}
