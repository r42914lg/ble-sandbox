// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.android.build.gradle.plugin)
        classpath(libs.kotlin.gradlePlugin)
        classpath(libs.kotlin.composeCompiler.gradlePlugin)
        classpath(libs.kotlin.serialization)
    }
}
