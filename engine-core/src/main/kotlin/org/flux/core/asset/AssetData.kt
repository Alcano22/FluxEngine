package org.flux.core.asset

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.flux.core.scene.AnimationClip

@Serializable
sealed class AssetData {

    @Serializable
    @SerialName("ANIMATION")
    data class Animation(
        val clips: List<AnimationClip> = emptyList()
    ) : AssetData()

    @Serializable
    @SerialName("SPRITESHEET")
    data class Spritesheet(
        val texture: TextureHandle,
        val cellWidth: Int,
        val cellHeight: Int,
        val paddingX: Int = 0,
        val paddingY: Int = 0,
        val offsetX: Int = 0,
        val offsetY: Int = 0
    ) : AssetData() {

        @Transient
        private var _emptyFrames: Set<Int>? = null

        fun computeUVs(frameIndex: Int): FloatArray {
            val tex = texture.resolve()
            val cols = ((tex.width - offsetX) / (cellWidth + paddingX).coerceAtLeast(1)).coerceAtLeast(1)

            val col = frameIndex % cols
            val row = frameIndex / cols

            val u0 = (offsetX + col * (cellWidth + paddingX)).toFloat() / tex.width
            val v0 = (offsetY + row * (cellHeight + paddingY)).toFloat() / tex.height
            val u1 = u0 + cellWidth.toFloat() / tex.width
            val v1 = v0 + cellHeight.toFloat() / tex.height

            return floatArrayOf(u0, 1f - v1, u1, 1f - v0)
        }

        fun getEmptyFrames(): Set<Int> {
            _emptyFrames?.let { return it }

            val tex = runCatching { texture.resolve() }.getOrNull()
                ?: return emptySet<Int>().also { _emptyFrames = it }

            val pixels = runCatching { tex.getPixels() }.getOrNull()
                ?: return emptySet<Int>().also { _emptyFrames = it }

            val channels = pixels.size / (tex.width * tex.height)
            val hasAlpha = channels == 4
            if (!hasAlpha)
                return emptySet<Int>().also { _emptyFrames = it }

            val cols = ((tex.width - offsetX) / (cellWidth + paddingX).coerceAtLeast(1)).coerceAtLeast(1)
            val rows = ((tex.height - offsetY) / (cellHeight + paddingY).coerceAtLeast(1)).coerceAtLeast(1)

            val empty = mutableSetOf<Int>()

            for (row in 0 until rows) {
                for (col in 0 until cols) {
                    val frameIndex = row * cols + col
                    val startX = offsetX + col * (cellWidth + paddingX)
                    val startY = offsetY + row * (cellHeight + paddingY)

                    var allTransparent = true
                    outer@ for (py in startY until startY + cellHeight) {
                        for (px in startX until startX + cellWidth) {
                            if (px >= tex.width || py >= tex.height) continue

                            val flippedY = tex.height - 1 - py
                            val idx = (flippedY * tex.width + px) * channels + 3
                            if (idx < pixels.size && pixels[idx] != 0.toByte()) {
                                allTransparent = false
                                break@outer
                            }
                        }
                    }

                    if (allTransparent)
                        empty.add(frameIndex)
                }
            }

            _emptyFrames = empty
            return empty
        }
    }
}
