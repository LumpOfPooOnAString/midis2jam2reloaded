/*
 * Copyright (C) 2025 Jacob Wysko
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package org.wysko.midis2jam2.world.background

import com.jme3.asset.AssetManager
import com.jme3.bounding.BoundingSphere
import com.jme3.material.Material
import com.jme3.math.Vector3f
import com.jme3.renderer.queue.RenderQueue
import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import com.jme3.scene.shape.Sphere
import com.jme3.texture.Image
import com.jme3.texture.Texture
import com.jme3.texture.Texture2D
import com.jme3.texture.TextureCubeMap
import com.jme3.texture.plugins.AWTLoader
import com.jme3.util.SkyFactory
import jme3tools.converters.ImageToAwt
import org.wysko.midis2jam2.starter.configuration.BackgroundConfiguration
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp

/**
 * Background factory for different background types.
 *
 * @property assetManager The asset manager to use.
 */
sealed class BackgroundFactory(internal val assetManager: AssetManager) {

    /**
     * Creates a background.
     *
     * The background is not attached to the scene graph.
     * The caller must do this.
     */
    abstract fun create(): Spatial

    /**
     * Loads a texture from the given [assetPath] and rotates it 180 degrees, such that it is right-side up.
     */
    internal fun loadTexture(assetPath: String) = rotateTexture(assetManager.loadTexture(assetPath))

    /**
     * Loads the default checkerboard background.
     *
     * @param assetManager The asset manager to use.
     * @see BackgroundConfiguration.DefaultBackground
     */
    class Default(assetManager: AssetManager) : BackgroundFactory(assetManager) {
        override fun create(): Geometry =
            Geometry("Sky", Sphere(10, 10, 10f, false, true)).apply {
                queueBucket = RenderQueue.Bucket.Sky
                cullHint = Spatial.CullHint.Never
                modelBound = BoundingSphere(Float.POSITIVE_INFINITY, Vector3f.ZERO)
                material = Material(assetManager, "Common/MatDefs/Misc/Sky.j3md").apply {
                    setVector3("NormalScale", Vector3f.UNIT_XYZ)
                    setTexture("Texture", loadSkyTexture())
                }
                shadowMode = RenderQueue.ShadowMode.Off
            }

        private fun loadSkyTexture(): TextureCubeMap =
            loadTexture("Assets/sky.png").let { texture ->
                Image(
                    texture.image.format,
                    texture.image.width,
                    texture.image.height,
                    null,
                    texture.image.colorSpace,
                ).apply { repeat(6) { addData(texture.image.data[0]) } }
            }.let { image ->
                TextureCubeMap(image).apply {
                    magFilter = Texture.MagFilter.Nearest
                    minFilter = Texture.MinFilter.NearestNoMipMaps
                    anisotropicFilter = 0
                    setWrap(Texture.WrapMode.EdgeClamp)
                }
            }
    }

    /**
     * Loads a unique cube map background.
     *
     * @param assetManager The asset manager to use.
     * @param config The configuration to use.
     * @see BackgroundConfiguration.UniqueCubeMapBackground
     */
    class UniqueCubeMap(
        assetManager: AssetManager,
        private val config: BackgroundConfiguration.UniqueCubeMapBackground,
    ) : BackgroundFactory(assetManager) {
        override fun create(): Spatial {
            val cubemap = config.cubemap
            val textures =
                listOf(
                    cubemap.west!!,
                    cubemap.east!!,
                    cubemap.north!!,
                    cubemap.south!!,
                    cubemap.up!!,
                    cubemap.down!!,
                ).map(::loadTexture)

            try {
                return SkyFactory.createSky(
                    assetManager,
                    textures[0],
                    textures[1],
                    textures[2],
                    textures[3],
                    textures[4],
                    textures[5],
                ).apply { shadowMode = RenderQueue.ShadowMode.Off }
            } catch (e: Exception) {
                if (e.message == "Images must have same format") {
                    throw BackgroundImageFormatException(textures.map { it.image.format })
                } else {
                    throw e
                }
            }
        }
    }

    /**
     * Loads a repeated cube map background.
     *
     * @see BackgroundConfiguration.RepeatedCubeMapBackground
     */
    class RepeatedCubeMap(
        assetManager: AssetManager,
        private val config: BackgroundConfiguration.RepeatedCubeMapBackground,
    ) : BackgroundFactory(assetManager) {
        override fun create(): Spatial = with(loadTexture(config.texture)) {
            SkyFactory.createSky(assetManager, this, this, this, this, this, this)
                .apply { shadowMode = RenderQueue.ShadowMode.Off }
        }
    }

    private fun rotateTexture(texture: Texture): Texture2D {
        val image = ImageToAwt.convert(texture.image, false, true, 0)
        val tx = AffineTransform.getScaleInstance(-1.0, -1.0).apply {
            translate(-image.width.toDouble(), -image.height.toDouble())
        }
        val rotatedImage = AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR).filter(image, null)
        return Texture2D(AWTLoader().load(rotatedImage, true))
    }
}