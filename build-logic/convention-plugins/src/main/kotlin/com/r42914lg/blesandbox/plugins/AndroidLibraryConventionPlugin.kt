package com.r42914lg.blesandbox.plugins

import com.android.build.api.dsl.LibraryExtension
import com.r42914lg.blesandbox.plugins.ext.libs
import com.r42914lg.blesandbox.plugins.ext.projectJavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

open class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            with(pluginManager) {
                apply("com.android.library")
            }
            extensions.configure<LibraryExtension> {
                compileSdk = libs.versions.compileSdk.get().toInt()
                defaultConfig {
                    minSdk = libs.versions.minSdk.get().toInt()
                }
                compileOptions {
                    sourceCompatibility = projectJavaVersion
                    targetCompatibility = projectJavaVersion
                }
                configureBuildTypes()
            }
            configureKotlinJvmCompiler()
        }
    }
}