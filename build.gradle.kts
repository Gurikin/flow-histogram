import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    id("org.jetbrains.compose") version "1.7.3" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.10" apply false
    kotlin("plugin.spring") version "2.3.0"
}

subprojects {

    group = rootProject.group
    version = rootProject.version

    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(21)
            target {
                compilerOptions {
                    languageVersion.set(KotlinVersion.KOTLIN_2_1)
                    optIn.add("kotlin.time.ExperimentalTime")
                }
            }
        }
        tasks.withType<Test> {
            workingDir = rootProject.projectDir
            useJUnitPlatform()
        }
    }
}
