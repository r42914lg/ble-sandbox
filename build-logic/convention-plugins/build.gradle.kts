import org.gradle.kotlin.dsl.`kotlin-dsl`
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

private val projectJavaVersion: JavaVersion = JavaVersion.toVersion(libs.versions.projectJavaVersion.get())

plugins {
    `kotlin-dsl`
}

group = "com.r42914lg.blesandbox.buildlogic"

java {
    sourceCompatibility = projectJavaVersion
    targetCompatibility = projectJavaVersion
}
tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(projectJavaVersion.toString()))
    }
}

dependencies {
    implementation(libs.android.build.gradle.plugin)
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.kotlin.composeCompiler.gradlePlugin)
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

gradlePlugin {
    plugins {
        register("AndroidApplication") {
            id = "android.application"
            implementationClass = "com.r42914lg.blesandbox.plugins.AndroidApplicationConventionPlugin"
        }
        register("AndroidLibrary") {
            id = "android.library"
            implementationClass = "com.r42914lg.blesandbox.plugins.AndroidLibraryConventionPlugin"
        }
        register("KotlinLibrary") {
            id = "kotlin.library"
            implementationClass = "com.r42914lg.blesandbox.plugins.KotlinLibraryConventionPlugin"
        }
        register("ComposeLibrary") {
            id = "compose.library"
            implementationClass = "com.r42914lg.blesandbox.plugins.ComposeAndroidLibraryConventionPlugin"
        }
    }
}