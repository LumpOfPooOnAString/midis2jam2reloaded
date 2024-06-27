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

package org.wysko.midis2jam2.instrument.family.percussion

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Node
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.EventCollector
import org.wysko.midis2jam2.instrument.family.percussion.GuiroStickSpeed.LONG
import org.wysko.midis2jam2.instrument.family.percussion.GuiroStickSpeed.SHORT
import org.wysko.midis2jam2.midi.LONG_GUIRO
import org.wysko.midis2jam2.midi.SHORT_GUIRO
import org.wysko.midis2jam2.world.modelD
import kotlin.math.abs
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

private val GUIRO_SLIDE_RANGE = 0f..1f
private val BASE_POSITION = Vector3f(30f, 55f, -51f)
private val BASE_ROTATION = Quaternion().fromAngles(0f, -0.7f, 0f)
private const val LONG_TIME_SCALE = 3.6f
private const val QUICK_TIME_SCALE = 9f
private const val STICK_MOTION_SCALE = 5f
private val STICK_BASE_POSITION = Vector3f(-2.5f, 1.94f, 2f)
private const val EASING_POWER = 2

/**
 * The Guiro.
 *
 * The Guiro is composed of two main animation components:
 * * The gourd (attached to [guiroNode])
 * * The stick (attached to [stickNode])
 *
 * The Guiro animates by moving the stick left and right across the gourd. The direction of movement alternates for each
 * note played. The animation is the same for both [LONG_GUIRO] and [SHORT_GUIRO], but scaled to different speeds
 * ([LONG_TIME_SCALE] and [QUICK_TIME_SCALE], respectively).
 *
 * The horizontal component of animation for the stick is defined with [stickPosition]. However, the actual horizontal
 * component is finally applied through [easedStickPosition]. This adds a slight speed increase through the motion of
 * the stick. The total amount of horizontal movement is defined by [STICK_MOTION_SCALE].
 *
 * The rotational component of animation for the stick is defined by the [verticalTransform] to define a "U" shape.
 *
 * The gourd is slightly lowered during the motion of the stick. This vertical offset is defined by [verticalTransform].
 */
class Guiro(
    context: Midis2jam2,
    shortHits: MutableList<NoteEvent.NoteOn>,
    longHits: MutableList<NoteEvent.NoteOn>,
) : AuxiliaryPercussion(context, (shortHits + longHits).sortedBy { it.tick }.toMutableList()) {
    private val shortCollector = EventCollector(context, shortHits)
    private val longCollector = EventCollector(context, longHits)

    private val guiroNode =
        Node().apply {
            attachChild(context.modelD("DrumSet_Guiro.obj", "GuiroSkin.png"))
        }.also {
            geometry.attachChild(it)
        }

    private val stickNode =
        Node().apply {
            attachChild(context.modelD("DrumSet_GuiroStick.obj", "Wood.bmp"))
        }.also {
            geometry.attachChild(it)
        }

    private var isMoving = false
    private var isMovingLeft = true
    private var movingSpeed = LONG
    private var stickPosition = 0f

    init {
        geometry.localTranslation = BASE_POSITION
        geometry.localRotation = BASE_ROTATION
    }

    override fun tick(
        time: Duration,
        delta: Duration,
    ) {
        super.tick(time, delta)

        shortCollector.advanceCollectOne(time)?.let {
            isMoving = true
            movingSpeed = SHORT
            isMovingLeft = !isMovingLeft // Alternate on each stroke
            if (isMovingLeft) stickPosition = 1f else 0f // Reset position to start of motion
        }

        longCollector.advanceCollectOne(time)?.let {
            isMoving = true
            movingSpeed = LONG
            isMovingLeft = !isMovingLeft // Alternate on each stroke
            if (isMovingLeft) stickPosition = 1f else 0f // Reset position to start of motion
        }

        if (isMoving) {
            stickPosition += ((if (isMovingLeft) -1f else 1f) * (if (movingSpeed == LONG) LONG_TIME_SCALE else QUICK_TIME_SCALE) * delta.toDouble(
                SECONDS
            )).toFloat()

            if (stickPosition !in GUIRO_SLIDE_RANGE) isMoving = false // If exceeded range, we've reached the end
            stickPosition = stickPosition.coerceIn(GUIRO_SLIDE_RANGE) // Don't ever show the stick outside the range
        }

        with(stickNode) {
            localTranslation =
                STICK_BASE_POSITION.add(easedStickPosition() * STICK_MOTION_SCALE, verticalTransform(), 0f)
            localRotation = Quaternion().fromAngles(verticalTransform() / 4, 0f, 0f)
        }
        guiroNode.setLocalTranslation(0f, verticalTransform() / 4, 0f)
    }

    private fun easedStickPosition(): Float =
        when (movingSpeed) {
            LONG -> {
                if (isMovingLeft) {
                    1 - (1 - stickPosition).pow(EASING_POWER)
                } else {
                    stickPosition.pow(EASING_POWER)
                }
            }

            SHORT -> {
                if (isMovingLeft) {
                    stickPosition.pow(EASING_POWER)
                } else {
                    1 - (1 - stickPosition).pow(EASING_POWER)
                }
            }
        }

    private fun verticalTransform(): Float = abs((2 * stickPosition - 1).pow(5)) // "U" shape in 0..1

    override fun toString(): String {
        return super.toString() +
                buildString {
                    appendLine(formatProperty("stickPosition", stickPosition))
                    appendLine(formatProperty("isMovingLeft", isMovingLeft.toString()))
                    appendLine(formatProperty("isMoving", isMoving.toString()))
                }
    }
}

private enum class GuiroStickSpeed {
    SHORT,
    LONG,
}
