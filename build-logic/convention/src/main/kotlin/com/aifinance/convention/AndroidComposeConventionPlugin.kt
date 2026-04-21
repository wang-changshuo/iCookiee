package com.aifinance.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

            extensions.configure<CommonExtension<*, *, *, *, *, *>> {
                buildFeatures {
                    compose = true
                }

                composeOptions {
                    kotlinCompilerExtensionVersion = libs.findVersion("composeCompiler").get().toString()
                }

                dependencies {
                    val bom = libs.findLibrary("androidx-compose-bom").get()
                    add("implementation", platform(bom))
                    add("androidTestImplementation", platform(bom))

                    add("implementation", libs.findBundle("compose").get())
                    add("debugImplementation", libs.findBundle("compose-debug").get())
                }
            }
        }
    }
}
