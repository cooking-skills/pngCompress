package com.zz.captain.compress

import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.tasks.ProcessAndroidResources
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

/**
 * Created by zhou on 2020/4/18.
 * company pa
 */
class CompressPlugin : Plugin<Project> {
    override fun apply(project: Project) {

        val isAppExtension = project.plugins.hasPlugin("com.android.application")
        val variants = if(isAppExtension) {
            (project.property("android") as AppExtension).applicationVariants
        } else {
            (project.property("android") as LibraryExtension).libraryVariants
        }

        /**
         * 1 第一步获取图片资源
         * 2 第二部创建任务
         * 3 寻找任务切入点 切入
         *
         */


        project.afterEvaluate {

            variants.all { variant ->
                variant as BaseVariant
                println("---- compress Plugin Start ----")

                // 获取 hook task
                val mergeResources = variant.mergeResourcesProvider.get()

//                val processRes = variant.mergeResourcesProvider.get()

                // 获取所有图片资源
//                var resources = variant.mergeResourcesProvider.get().outputDir.asFile.get()
                var resources = variant.allRawAndroidResources.files
                // 定义task
                var compressTask = project.task("compressPicResource${variant.name}")

                // 收集 图片资源
                compressTask.doLast {
                    var imageList = arrayListOf<String>()

                    println("压缩文件路径 $resources")
                    resources.forEach {
                        val fileTree: FileTreeWalk = it.walk()
                        fileTree
                                .filter { it.isFile } //只挑选文件，不处理文件夹
                                .filter { it.extension == "png" || it.extension == "jpg" }
                                .forEach { imageList.add(it.path) }
                    }
                        // 处理图片资源
                    imageList.forEach {
                        println("压缩文件路径 $it 是否是文件 ${File(it).isFile}")
//                        println(project.rootDir.path)
                        // 脚本处理
                        if (File(it).isFile){
                            var file = File(it)
                            println("压缩前文件size ${file.length()}")
                            if (file.length()>10*1000) {
                                try {
                                    val rc = project.exec { spec ->
                                        spec.workingDir = File( project.rootDir.path + File.separator + "cmds")
                                        spec.commandLine = listOf("./pngquant", "--skip-if-larger", "--speed", "1", "--nofs", "--force", "--output", file.path, "--", file.path)
                                    }

//                            (rc as ExecResult ).
                                    if (rc.exitValue!=0){
                                        println("parse ERROR")
                                    }else{
                                        println("压缩后文件size ${file.length()}")
                                        println("压缩文件路径 $it")
                                    }
                                } catch (e: Exception) {
                                    println("压缩文件异常 $it")
                                }
                            }

                        }

                    }
                }

                // 挂载切入点
                var processRes = project.tasks.withType(ProcessAndroidResources::class.java).findByName("process${variant.name.capitalize()}Resources")

//                compressTask.dependsOn(mergeResources.taskDependencies.getDependencies(mergeResources))
//                mergeResources.dependsOn(compressTask)
                compressTask.dependsOn(mergeResources)
                processRes!!.dependsOn(compressTask)

                print("task finish")
            }
        }
    }
}