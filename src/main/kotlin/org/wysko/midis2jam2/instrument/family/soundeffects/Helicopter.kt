/*
 * Copyright (C) 2024 Jacob Wysko
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
package org.wysko.midis2jam2.instrument.family.soundeffects

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import com.jme3.scene.Spatial.CullHint.Always
import com.jme3.scene.Spatial.CullHint.Dynamic
import org.spongepowered.noise.Noise
import org.spongepowered.noise.NoiseQuality
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.modelD
import java.util.*
import kotlin.math.cos
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

/**
 * The helicopter has several animation components.
 *
 * The first is the rotor that spins on each frame. The rotor has 12 different layers of lights that can be turned on
 * and off, representing the 12 notes in an octave.
 *
 * The second is the random wobble when it is flying. Uses [org.spongepowered.noise.Noise] to get some perlin-like noise
 * for smooth wobbling.
 *
 * Finally, the helicopter moves down when not playing and moves up when playing. This motion is eased with a sinusoidal
 * function.
 */
class Helicopter(context: Midis2jam2, eventList: List<MidiEvent>) :
    SustainedInstrument(context, eventList) {
    /** "Seed" for X-value generation. */
    private val rotXRand: Float

    /** "Seed" for Y-value generation. */
    private val rotYRand: Float

    /** "Seed" for Z-value generation. */
    private val rotZRand: Float

    /** Contains all geometry for animation. */
    private val animNode = Node()

    /** The rotor which spins. */
    private val rotor = Node()

    /** Each set of lights on the helicopter. Each spatial is a plane that has the light texture for that note. */
    private val lights: Array<Spatial>

    /** The amount of height and random movement. */
    private var force = 0.0

    override fun tick(
        time: Duration,
        delta: Duration,
    ) {
        super.tick(time, delta)

        // Turn all lights off
        lights.forEach { it.cullHint = Always }

        // Turn on active lights
        collector.currentTimedArcs.forEach { lights[11 - (it.note + 3) % 12].cullHint = Dynamic }

        // If playing a note, increase the force, but cap it at 1.
        if (collector.currentTimedArcs.isNotEmpty()) {
            force += delta.toDouble(SECONDS)
            force = force.coerceAtMost(1.0)
        } else {
            // Otherwise, decrease force but cup at 0.
            force -= delta.toDouble(SECONDS)
            force = force.coerceAtLeast(0.0)
        }

        // Vroom
        rotor.rotate(Quaternion().fromAngles(0f, rad((3141 * delta.toDouble(SECONDS))), 0f))

        // Slight wobble
        animNode.localRotation =
            Quaternion().fromAngles(
                (force * 0.5f *
                        rad(
                            (
                                    Noise.gradientCoherentNoise3D(
                                        0.0,
                                        0.0,
                                        time.toDouble(SECONDS),
                                        (rotXRand * 1000).toInt(),
                                        NoiseQuality.STANDARD,
                                    ) - 0.4
                                    ) * 10,
                        )).toFloat(),
                (force * 0.5f *
                        rad(
                            (
                                    Noise.gradientCoherentNoise3D(
                                        0.0,
                                        0.0,
                                        time.toDouble(SECONDS),
                                        (rotYRand * 1000).toInt(),
                                        NoiseQuality.STANDARD,
                                    ) - 0.4
                                    ) * 10,
                        )).toFloat(),
                (force * 0.5f *
                        rad(
                            (
                                    Noise.gradientCoherentNoise3D(
                                        0.0,
                                        0.0,
                                        time.toDouble(SECONDS),
                                        (rotZRand * 1000).toInt(),
                                        NoiseQuality.STANDARD,
                                    ) - 0.4
                                    ) * 10,
                        )).toFloat(),
            )
        placement.localRotation = Quaternion().fromAngles(rad(5.0), rad(120.0), rad(11.0))
        animNode.setLocalTranslation(
            0f,
            (
                force * (
                    Noise.gradientCoherentNoise3D(
                        0.0,
                        0.0,
                        time.toDouble(SECONDS),
                        (rotZRand * 1000).toInt(),
                        NoiseQuality.STANDARD,
                    ) - 0.4
                )
            ).toFloat() * 10,
            0f,
        )
        placement.setLocalTranslation(0f, -120 + 120 * easeInOutSine(force), 0f)
    }

    override fun adjustForMultipleInstances(delta: Duration) {
        root.setLocalTranslation(20 + 50 * updateInstrumentIndex(delta), 40f, -300f)
    }

    companion object {
        /**
         * easeInOutSine, ripped from [easings.net](https://easings.net/#easeInOutSine).
         *
         * @param x in
         * @return out
         */
        private fun easeInOutSine(x: Number): Float {
            return (-(cos(Math.PI * x.toDouble()) - 1) / 2).toFloat()
        }
    }

    init {
        // Load helicopter
        val copter = context.modelD("HelicopterBody.obj", "Helicopter.png")
        rotor.attachChild(
            context.assetLoader.fakeShadow(
                "Assets/HelicopterRotorPlane.obj",
                "Assets/HelicopterRotor.png",
            ),
        )

        // Load lights
        lights =
            Array(12) {
                context.assetLoader.fakeShadow(
                    "Assets/HelicopterRotorPlane.obj",
                    "Assets/HelicopterLights${it + 1}.png"
                )
                    .apply {
                        rotor.attachChild(this)
                        this.cullHint = Always
                    }
            }

        rotor.setLocalTranslation(40f, 36f, 0f)
        animNode.attachChild(copter)
        animNode.attachChild(rotor)

        // Load rotor cap
        val cap = context.modelD("HelicopterRotorCap.obj", "Helicopter.png")
        cap.setLocalTranslation(0f, 0f, 0.5f)
        animNode.attachChild(cap)

        // Initialize RNG
        val random = Random()
        rotXRand = random.nextFloat()
        rotYRand = random.nextFloat()
        rotZRand = random.nextFloat()
        geometry.attachChild(animNode)
    }
}
