package com.r42914lg.blesandbox.plugins

import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

class ComposeAndroidLibraryConventionPlugin : AndroidLibraryConventionPlugin() {
    override fun apply(project: Project) {
        super.apply(project)
        with(project) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.plugin.compose")
            }
            extensions.configure<LibraryExtension> {
                buildFeatures {
                    compose = true
                }
            }
            extensions.configure<ComposeCompilerGradlePluginExtension> {
                val tskReqStr = gradle.startParameter.taskRequests.toString()
                if (tskReqStr.lowercase().contains("devProd", ignoreCase = true)) {
                    includeSourceInformation.set(true)
                    reportsDestination.set(project.layout.buildDirectory.dir("compose"))
                    metricsDestination.set(project.layout.buildDirectory.dir("compose"))
                }
            }
        }
    }
}