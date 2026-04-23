package org.flux.opengl.renderer

import org.flux.core.renderer.Shader
import org.flux.core.util.memScoped
import org.joml.Matrix2fc
import org.joml.Matrix3fc
import org.joml.Matrix4fc
import org.joml.Vector2ic
import org.lwjgl.opengl.GL46C.*

class GLShader : Shader {

    override val rendererId = glCreateProgram()

    private val uniformLocationCache = mutableMapOf<String, Int>()

    constructor(vertexSrc: String, fragmentSrc: String, defines: Map<String, Any>) {
        val finalVert = injectDefines(vertexSrc, defines)
        val finalFrag = injectDefines(fragmentSrc, defines)
        setup(finalVert, finalFrag)
    }

    constructor(src: String, defines: Map<String, Any>) {
        val (vert, frag) = parseCombinedSource(src, defines)
        setup(vert, frag)
    }

    private fun setup(vertexSrc: String, fragmentSrc: String) {
        val vertexShader = compileShader(GL_VERTEX_SHADER, vertexSrc)
        val fragmentShader = compileShader(GL_FRAGMENT_SHADER, fragmentSrc)

        glAttachShader(rendererId, vertexShader)
        glAttachShader(rendererId, fragmentShader)
        glLinkProgram(rendererId)
        if (glGetProgrami(rendererId, GL_LINK_STATUS) == GL_FALSE) {
            val log = glGetProgramInfoLog(rendererId)

            glDeleteProgram(rendererId)
            glDeleteShader(vertexShader)
            glDeleteShader(fragmentShader)

            throw RuntimeException("Failed to link shader:\n$log")
        }

        glDetachShader(rendererId, vertexShader)
        glDetachShader(rendererId, fragmentShader)
        glDeleteShader(vertexShader)
        glDeleteShader(fragmentShader)
    }

    private fun parseCombinedSource(src: String, defines: Map<String, Any>): Pair<String, String> {
        val commonCode = StringBuilder()
        val vertexCode = StringBuilder()
        val fragmentCode = StringBuilder()

        var currentStage = "common"
        src.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("#stage"))
                currentStage = trimmed.substringAfter("#stage").trim()
            else {
                when (currentStage) {
                    "common"   -> commonCode.appendLine(line)
                    "vertex"   -> vertexCode.appendLine(line)
                    "fragment" -> fragmentCode.appendLine(line)
                }
            }
        }

        val vert = injectDefines(commonCode.toString() + vertexCode.toString(), defines)
        val frag = injectDefines(commonCode.toString() + fragmentCode.toString(), defines)
        return vert to frag
    }

    private fun injectDefines(src: String, defines: Map<String, Any>): String {
        if (defines.isEmpty())
            return src

        val defineLines = defines.map { (key, value) -> "#define $key $value" }.joinToString("\n")
        val lines = src.lines().toMutableList()
        val versionIndex = lines.indexOfFirst { it.trim().startsWith("#version") }
        if (versionIndex != -1)
            lines.add(versionIndex + 1, defineLines)
        else
            lines.add(0, defineLines)

        return lines.joinToString("\n")
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = glCreateShader(type)
        glShaderSource(shader, src)
        glCompileShader(shader)
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            val log = glGetShaderInfoLog(shader)
            glDeleteShader(shader)

            val typeStr = when (type) {
                GL_VERTEX_SHADER -> "vertex"
                GL_FRAGMENT_SHADER -> "fragment"
                else -> "unknown"
            }
            throw RuntimeException("Failed to compile $typeStr shader:\n$log")
        }

        return shader
    }

    override fun bind() = glUseProgram(rendererId)
    override fun unbind() = glUseProgram(0)

    private fun getUniformLocation(name: String): Int {
        if (uniformLocationCache.containsKey(name))
            return uniformLocationCache[name]!!

        val loc = glGetUniformLocation(rendererId, name)
        if (loc == -1)
            println("Warning: Uniform '$name' doesn't exist in shader")

        uniformLocationCache[name] = loc
        return loc
    }

    override fun setInt(name: String, value: Int) =
        glProgramUniform1i(rendererId, getUniformLocation(name), value)

    override fun setIntArray(name: String, values: IntArray) =
        glProgramUniform1iv(rendererId, getUniformLocation(name), values)

    override fun setInt2(name: String, x: Int, y: Int) =
        glProgramUniform2i(rendererId, getUniformLocation(name), x, y)

    override fun setInt3(name: String, x: Int, y: Int, z: Int) =
        glProgramUniform3i(rendererId, getUniformLocation(name), x, y, z)

    override fun setInt4(name: String, x: Int, y: Int, z: Int, w: Int) =
        glProgramUniform4i(rendererId, getUniformLocation(name), x, y, z, w)

    override fun setFloat(name: String, value: Float) =
        glProgramUniform1f(rendererId, getUniformLocation(name), value)

    override fun setFloatArray(name: String, values: FloatArray) =
        glProgramUniform1fv(rendererId, getUniformLocation(name), values)

    override fun setFloat2(name: String, x: Float, y: Float) =
        glProgramUniform2f(rendererId, getUniformLocation(name), x, y)

    override fun setFloat3(name: String, x: Float, y: Float, z: Float) =
        glProgramUniform3f(rendererId, getUniformLocation(name), x, y, z)

    override fun setFloat4(name: String, x: Float, y: Float, z: Float, w: Float) =
        glProgramUniform4f(rendererId, getUniformLocation(name), x, y, z, w)

    override fun setMat2(name: String, matrix: Matrix2fc) = memScoped {
        val buf = mallocFloat(2 * 2)
        matrix.get(buf)
        glProgramUniformMatrix2fv(rendererId, getUniformLocation(name), false, buf)
    }

    override fun setMat3(name: String, matrix: Matrix3fc) = memScoped {
        val buf = mallocFloat(3 * 3)
        matrix.get(buf)
        glProgramUniformMatrix3fv(rendererId, getUniformLocation(name), false, buf)
    }

    override fun setMat4(name: String, matrix: Matrix4fc) = memScoped {
        val buf = mallocFloat(4 * 4)
        matrix.get(buf)
        glProgramUniformMatrix4fv(rendererId, getUniformLocation(name), false, buf)
    }

    override fun dispose() = glDeleteProgram(rendererId)
}
