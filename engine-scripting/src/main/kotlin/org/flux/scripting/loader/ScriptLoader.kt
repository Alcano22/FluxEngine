package org.flux.scripting.loader

import kotlinx.serialization.modules.polymorphic
import org.flux.core.logging.logger
import org.flux.core.scene.Component
import org.flux.core.serialization.SceneSerializer
import org.flux.scripting.Script
import org.flux.scripting.ScriptSerializer
import java.io.File
import java.net.URLClassLoader
import kotlin.reflect.KClass

object ScriptLoader {

    private val logger = logger()

    private var classLoader: URLClassLoader? = null

    fun init(outputDir: File) {
        classLoader = URLClassLoader(
            arrayOf(outputDir.toURI().toURL()),
            Script::class.java.classLoader
        )

        outputDir.walkTopDown()
            .filter { it.extension == "class" && !it.name.contains('$') }
            .forEach { classFile ->
                val className = classFile.relativeTo(outputDir)
                    .path
                    .removeSuffix(".class")
                    .replace(File.separatorChar, '.')
                runCatching {
                    val cls = classLoader!!.loadClass(className)
                    if (Script::class.java.isAssignableFrom(cls) && cls != Script::class.java) {
                        SceneSerializer.additionalSerializers += {
                            polymorphic(Component::class) {
                                @Suppress("UNCHECKED_CAST")
                                subclass(
                                    cls.kotlin as KClass<Script>,
                                    ScriptSerializer.withSerialName(className)
                                )
                            }
                        }
                    }
                }
            }

        logger.info { "ScriptLoader initialized: ${outputDir.absolutePath}" }
    }

    fun instantiate(className: String): Script {
        val loader = classLoader
            ?: error("Not initialized")

        val cls = loader.loadClass(className)
        return cls.getDeclaredConstructor().newInstance() as Script
    }

    fun instantiateOrNull(className: String): Script? = runCatching {
        instantiate(className)
    }.getOrElse {
        logger.error { "Could not instantiate script '$className': ${it.message}" }
        null
    }

    fun reload(outputDir: File) {
        classLoader?.close()
        init(outputDir)
    }
}
