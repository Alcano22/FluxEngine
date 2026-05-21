plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.shadow)
    application
}

val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch").lowercase()

val lwjglNatives = when {
    osName.contains("win") -> "natives-windows"
    osName.contains("mac") -> {
        if (osArch.contains("aarch64"))
            "natives-macos-arm64"
        else
            "natives-macos"
    }
    else -> "natives-linux"
}

val lwjglPlatforms = listOf(
    "natives-windows",
    "natives-linux",
    "natives-macos",
    "natives-macos-arm64"
)

dependencies {
    implementation(project(":engine-core"))
    implementation(project(":engine-backend-glfw"))
    implementation(project(":engine-backend-opengl"))
    implementation(project(":engine-scripting"))

    implementation(libs.kotlinx.coroutines)

    implementation(platform(libs.lwjgl.bom))
    lwjglPlatforms.forEach { platform ->
        runtimeOnly("org.lwjgl:lwjgl::$platform")
        runtimeOnly("org.lwjgl:lwjgl-glfw::$platform")
        runtimeOnly("org.lwjgl:lwjgl-opengl::$platform")
        runtimeOnly("org.lwjgl:lwjgl-stb::$platform")
    }
}

tasks.named<JavaExec>("run") {
    if (osName.contains("linux")) {
        environment("__NV_PRIME_RENDER_OFFLOAD", "1")
        environment("__GLX_VENDOR_LIBRARY_NAME", "nvidia")
    }
}

tasks.shadowJar {
    archiveBaseName.set("FluxEngine")
    archiveClassifier.set("")
    archiveVersion.set("1.0")
    mergeServiceFiles()
}

application {
    mainClass.set("org.flux.editor.MainKt")
}
