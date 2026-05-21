package org.flux.core.asset

import kotlinx.serialization.Serializable
import org.flux.core.renderer.Texture2D
import org.flux.core.serialization.AssetHandleSerializer

@Serializable(with = AssetHandleSerializer::class)
@JvmInline
value class AssetHandle<T>(val path: String)

typealias TextureHandle     = AssetHandle<Texture2D>
typealias AnimationHandle   = AssetHandle<AssetData.Animation>
typealias SpritesheetHandle = AssetHandle<AssetData.Spritesheet>

fun AssetHandle<Texture2D>.resolve() = AssetManager.getTexture(path)
fun AssetHandle<AssetData.Animation>.resolve() = AssetManager.getAsset<AssetData.Animation>(path)
fun AssetHandle<AssetData.Spritesheet>.resolve() = AssetManager.getAsset<AssetData.Spritesheet>(path)
