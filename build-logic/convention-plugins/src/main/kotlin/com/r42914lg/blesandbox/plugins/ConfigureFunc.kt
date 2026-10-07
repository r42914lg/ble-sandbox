package com.r42914lg.blesandbox.plugins

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.r42914lg.blesandbox.plugins.ext.kotlinJvmCompilerOptions
import com.r42914lg.blesandbox.plugins.ext.projectJavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

fun Project.configureKotlinJvmCompiler() {
    kotlinJvmCompilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(projectJavaVersion.toString()))
    }
}
fun LibraryExtension.configureBuildTypes() {
    buildTypes {
        debug {}
        release {}
    }
}

fun ApplicationExtension.configureBuildTypes() {
    buildTypes {
        debug {}
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}