package com.github.gradle.node.yarn.task

import com.github.gradle.node.NodeExtension
import com.github.gradle.node.NodePlugin
import groovy.lang.Closure
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.property
import java.io.File

/**
 * yarn install that only gets executed if gradle decides so.
 */
open class YarnInstallTask : YarnTask() {
    private val nodeExtension by lazy { NodeExtension[project] }

    @get:Internal
    val nodeModulesOutputFilter =
            project.objects.property<(ConfigurableFileTree.() -> Unit)>()

    init {
        group = NodePlugin.NODE_GROUP
        description = "Install node packages using Yarn."
        dependsOn(YarnSetupTask.NAME)

        project.afterEvaluate {
            val nodeModulesDirectory = nodeExtension.nodeModulesDir.get().dir("node_modules")
            val filter = nodeModulesOutputFilter.orNull
            if (filter != null) {
                val nodeModulesFileTree = project.fileTree(nodeModulesDirectory)
                filter.invoke(nodeModulesFileTree)
                outputs.files(nodeModulesFileTree)
            } else {
                outputs.dir(nodeModulesDirectory)
            }
        }
    }

    @PathSensitive(RELATIVE)
    @Optional
    @InputFile
    protected fun getPackageJsonFile(): Provider<File?> {
        return projectFileIfExists("package.json")
    }

    @PathSensitive(RELATIVE)
    @Optional
    @InputFile
    protected fun getYarnLockFile(): Provider<File?> {
        return projectFileIfExists("yarn.lock")
    }

    @Optional
    @OutputFile
    protected fun getYarnLockFileAsOutput(): Provider<File?> {
        return projectFileIfExists("yarn.lock")
    }

    private fun projectFileIfExists(name: String): Provider<File?> {
        return nodeExtension.nodeModulesDir.map { it.file(name).asFile }
                .flatMap { if (it.exists()) project.providers.provider { it } else project.providers.provider { null } }
    }

    // For Groovy DSL
    @Suppress("unused")
    fun setNodeModulesOutputFilter(nodeModulesOutputFilter: Closure<ConfigurableFileTree>) {
        this.nodeModulesOutputFilter.set { nodeModulesOutputFilter.invoke(this) }
    }

    companion object {
        const val NAME = "yarn"
    }
}