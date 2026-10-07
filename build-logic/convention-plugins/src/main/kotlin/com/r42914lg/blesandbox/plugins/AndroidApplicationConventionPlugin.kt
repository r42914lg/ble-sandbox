package com.r42914lg.blesandbox.plugins

import com.android.build.api.dsl.ApplicationExtension
import com.r42914lg.blesandbox.plugins.ext.libs
import com.r42914lg.blesandbox.plugins.ext.projectJavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import kotlin.text.toInt

open class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            with(pluginManager) {
                apply("com.android.application")
                apply("org.jetbrains.kotlin.plugin.compose")
                apply("kotlinx-serialization")
            }
            extensions.configure<ApplicationExtension> {
                namespace = "com.r42914lg.blesandbox"
                compileSdk = libs.versions.compileSdk.get().toInt()
                defaultConfig {
                    applicationId = "com.r42914lg.blesandbox"
                    minSdk = libs.versions.minSdk.get().toInt()
                    targetSdk = libs.versions.targetSdk.get().toInt()
                    versionCode = libs.versions.versionCode.get().toInt()
                    versionName = libs.versions.versionName.get()

                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                }
                compileOptions {
                    sourceCompatibility = projectJavaVersion
                    targetCompatibility = projectJavaVersion
                }
                configureBuildTypes()
                configureKotlinJvmCompiler()

                buildFeatures {
                    compose = true
                }
            }
        }
    }
}