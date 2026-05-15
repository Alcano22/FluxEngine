import java.net.URLClassLoader

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    `java-library`
}

dependencies {
    api(project(":engine-core"))
    api(libs.luaj)
    implementation(libs.reflections)
}

tasks.register("generateLuaApi") {
    group = "flux"
    description = "Generates flux_api.lua EmmyLua definitions from @LuaApi annotations"

    dependsOn("compileKotlin")

    doLast {
        val outputPath = project.rootDir
            .resolve("engine-editor/assets/scripts/.definitions/flux_api.lua")
            .absolutePath

        val classesDir = layout.buildDirectory.dir("classes/kotlin/main").get().asFile
        val compileClasspath = configurations.getByName("compileClasspath").files

        val urls = (compileClasspath + classesDir).map { it.toURI().toURL() }.toTypedArray()
        val classLoader = URLClassLoader(urls, ClassLoader.getPlatformClassLoader())

        val generatorClass = classLoader.loadClass("org.flux.scripting.annotation.LuaApiGenerator")
        val instance = generatorClass.kotlin.objectInstance
            ?: generatorClass.getDeclaredField("INSTANCE").get(null)
        val method = generatorClass.getMethod("generate", String::class.java)
        method.invoke(instance, outputPath)
    }
}
