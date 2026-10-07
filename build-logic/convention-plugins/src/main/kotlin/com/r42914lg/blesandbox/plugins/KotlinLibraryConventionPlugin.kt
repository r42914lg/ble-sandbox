package com.r42914lg.blesandbox.plugins

import com.r42914lg.blesandbox.plugins.ext.projectJavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure

class KotlinLibraryConventionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            with(pluginManager) {
                apply("java-library")
                apply("org.jetbrains.kotlin.jvm")
                apply("kotlinx-serialization")
            }
            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = projectJavaVersion
                targetCompatibility = projectJavaVersion
            }
            configureKotlinJvmCompiler()
        }
    }
}