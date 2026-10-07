package com.r42914lg.blesandbox.plugins.ext

import org.gradle.accessors.dm.LibrariesForLibs
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

fun Project.kotlinJvmCompilerOptions(block: KotlinJvmCompilerOptions.() -> Unit) {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions(block)
    }
}

val Project.libs: LibrariesForLibs
    get() = the<LibrariesForLibs>()

val Project.projectJavaVersion: JavaVersion
    get() = JavaVersion.toVersion(libs.versions.projectJavaVersion.get().toInt())