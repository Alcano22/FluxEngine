package org.flux.scripting.compiler

import org.flux.core.logging.logger
import org.flux.core.util.TaskManager
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import java.io.File

object ScriptCompiler {

    private val logger = logger()

    fun compile(
        scriptsDir: File,
        outputDir: File,
        classpathJars: List<File>
    ): Boolean {
        val sources = scriptsDir.walkTopDown()
            .filter { it.extension == "kt" }
            .toList()

        if (sources.isEmpty()) {
            logger.info { "No scripts found in ${scriptsDir.absolutePath}" }
            return true
        }

        if (!needsRecompile(sources, outputDir)) {
            logger.info { "Scripts up-to-date, skipping compilation" }
            return true
        }

        outputDir.mkdirs()

        val classpath = classpathJars.joinToString(File.pathSeparator) { it.absolutePath }

        val args = buildList {
            addAll(sources.map { it.absolutePath })
            add("-classpath");  add(classpath)
            add("-d");          add(outputDir.absolutePath)
            add("-jvm-target"); add("21")
        }

        logger.info { "Compiling ${sources.size} script(s)..." }
        TaskManager.begin("scripting_compile", "Compiling ${sources.size} script(s)...")

        return try {
            val exitCode = K2JVMCompiler()
                .exec(System.err, *args.toTypedArray())
                .code
            if (exitCode == 0) {
                logger.info { "Compilation successful" }
                true
            } else {
                logger.error { "Compilation failed (exit code $exitCode)" }
                false
            }
        } catch (e: Exception) {
            logger.error(e) { "Compiler error" }
            false
        } finally {
            TaskManager.finish("scripting_compile")
        }
    }

    private fun needsRecompile(sources: List<File>, outputDir: File): Boolean {
        val classFiles = outputDir.walkTopDown()
            .filter { it.extension == "class" }
            .toList()

        if (classFiles.isEmpty())
            return true

        val newestClass = classFiles.maxOf { it.lastModified() }
        val newestSource = sources.maxOf { it.lastModified() }
        return newestSource > newestClass
    }
}
