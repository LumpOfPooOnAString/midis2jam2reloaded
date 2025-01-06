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

package org.wysko.midis2jam2.instrument.algorithmic

import com.jme3.math.Matrix3f
import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.NoteEvent.NoteOn
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.util.node
import org.wysko.midis2jam2.util.unaryPlus
import org.wysko.midis2jam2.world.Axis
import org.wysko.midis2jam2.world.modelD
import java.lang.Math.toRadians
import kotlin.time.Duration
import kotlin.time.DurationUnit.SECONDS

/**
 * The maximum angle, in degrees, at which the stick will rest and recoil to.
 */
const val MAX_STICK_IDLE_ANGLE: Double = 50.0

private const val DEFAULT_STRIKE_SPEED = 3.0

/**
 * Provides common logic and functionality for objects that animate with a "strike" motion, such as drum sticks or
 * mallets.
 *
 * @param context context to the main class
 * @param strikeEvents the list of events that determine when this stick will strike
 * @param stickModel the actual model used for display
 * @param strikeSpeed the speed at which the stick will animate downwards to strike
 * @param maxIdleAngle the maximum angle, in degrees, at which the stick will rest and recoil to
 * @param rotationAxis the axis on which the stick will rotate
 * @param sticky true if the stick should remain visible between strikes up to a certain tolerance, false otherwise
 * @param actualStick true if the spatial used in this object is actually a stick, and not something that is does not
 * appear as a stick but uses the same motion, false otherwise. When false, this object will always remain visible when
 * the "never_hidden" midis2jam2 property is enabled.
 */
class Striker(
    private val context: Midis2jam2,
    private val strikeEvents: List<NoteOn>,
    private val stickModel: Spatial,
    private val strikeSpeed: Double = DEFAULT_STRIKE_SPEED,
    private val maxIdleAngle: Double = MAX_STICK_IDLE_ANGLE,
    private val rotationAxis: Axis = Axis.X,
    private val sticky: Boolean = true,
    private val actualStick: Boolean = true,
) {
    /** Secondary constructor allowing for a predefined type of stick passed as a [StickType]. */
    constructor(
        context: Midis2jam2,
        strikeEvents: List<NoteOn>,
        stickModel: StickType,
        strikeSpeed: Double = DEFAULT_STRIKE_SPEED,
        maxIdleAngle: Double = MAX_STICK_IDLE_ANGLE,
        rotationAxis: Axis = Axis.X,
        sticky: Boolean = true,
        actualStick: Boolean = true,
    ) : this(
        context,
        strikeEvents,
        stickModel.let {
            context.modelD(it.modelName, it.textureName)
        },
        strikeSpeed,
        maxIdleAngle,
        rotationAxis,
        sticky,
        actualStick,
    )

    /**
     * The global node for this object. Feel free to translate, rotate, and scale this freely.
     */
    val node: Node = node()

    private val eventCollector = EventCollector(context, strikeEvents)
    private val rotationNode = with(node) {
        +node {
            +stickModel
        }
    }

    /**
     * Updates animation, given the current [time] and the amount of time since the last frame ([delta]).
     */
    fun tick(
        time: Duration,
        delta: Duration,
    ): StickStatus {
        // Collect an event if a strike is occurring now
        val strike = eventCollector.advanceCollectOne(time)

        // See what the proposed rotation angle is
        val proposedRotation = proposedRotation(time)

        // If the proposed angle is greater than the maximum idle angle (i.e., we need to wait a bit longer until we
        // show the stick coming down
        if (proposedRotation > maxIdleAngle) {
            // If it is also the case that the stick is currently at an angle less than the proposed (recoiling)
            val currentAngles = rotationNode.localRotation.toAngles(null)
            if (currentAngles[rotationAxis.componentIndex] <= maxIdleAngle) {
                // Recoil the stick by slightly raising it
                setRotation(
                    axis = rotationAxis,
                    angle = (currentAngles[rotationAxis.componentIndex] + 5f * delta.toDouble(SECONDS)).coerceAtMost(
                        toRadians(maxIdleAngle)
                    ),
                )
            }
        } else {
            // The proposed angle is simply less than the maximum, so we just set the angle to be that
            setRotation(
                axis = rotationAxis,
                angle = toRadians(proposedRotation.coerceIn(0.0..maxIdleAngle)),
            )
        }

        // After all the rotations have been completed, write down the new angles
        val finalAngles = rotationNode.localRotation.toAngles(null)

        // Determine visibility based on current rotation
        if (finalAngles[rotationAxis.componentIndex] >= rad(maxIdleAngle)) {
            // Not yet ready to strike, hide it
            node.cullHint = Spatial.CullHint.Always
        } else {
            // Striking or recoiling, show it1
            node.cullHint = Spatial.CullHint.Dynamic
        }

        // If the stick is sticky (should appear in between hits)
        if (sticky) {
            val peek = eventCollector.peek()
            val prev = eventCollector.prev()

            if (peek != null && prev != null && peek.tick - prev.tick <= context.sequence.smf.tpq * 2.1) {
                node.cullHint = Spatial.CullHint.Dynamic
            }
        }

        /* If this is not an actual stick, we will delegate visibility calculations to `Instrument` and just never
         * modify our cull hint. */
        if (!actualStick) {
            node.cullHint = Spatial.CullHint.Dynamic
        }

        // Return some information back to the caller
        return StickStatus(
            strike = strike,
            rotationAngle = finalAngles[rotationAxis.componentIndex],
            strikingFor = if (proposedRotation > maxIdleAngle) null else eventCollector.peek(),
        )
    }

    /**
     * Sets the parent of this object.
     */
    fun setParent(parent: Node) {
        parent.attachChild(this.node)
    }

    /** Returns [EventCollector.peek]. */
    fun peek(): NoteOn? = eventCollector.peek()

    /**
     * If the stick needs to be moved to change the point of rotation (or otherwise operated on), you can modify it with
     * this function.
     */
    fun offsetStick(operation: (stick: Spatial) -> Unit): Unit = operation(stickModel)

    private fun proposedRotation(time: Duration): Double {
        return eventCollector.peek()?.let {
            // The rotation is essentially defined by the amount of time between NOW and the next hit.
            var rot = (context.sequence.getTimeOf(it) - time).toDouble(SECONDS)

            /* The rotation should be dependent on the current tempo, i.e., if the tempo is faster, the rotation should
             * happen quicker, v.v. */
            rot *= context.sequence.getTempoBeforeTick(it.tick).beatsPerMinute

            // We may wish to scale the entire rotation to hasten/delay the animation.
            rot *= strikeSpeed

            return rot
        } ?: (maxIdleAngle + 1) // We have nothing to hit, so idle just above the max angle
    }

    private fun setRotation(
        axis: Axis,
        angle: Double,
    ) {
        rotationNode.localRotation =
            Quaternion().fromAngles(Matrix3f.IDENTITY.getRow(axis.componentIndex).mult(angle.toFloat()).toArray(null))
    }
}

/**
 * Defines properties for a [Striker].
 *
 * @property stickModel The model used for the stick.
 * @property strikeSpeed The speed at which the stick will animate downwards to strike.
 * @property maxIdleAngle The maximum angle, in degrees, at which the stick will rest and recoil to.
 * @property rotationAxis The axis on which the stick will rotate.
 * @property sticky `true` if the stick should remain visible between strikes up to a certain tolerance, `false`
 * otherwise.
 * @property actualStick `true` if the spatial used in this object is actually a stick, and not something that does
 * not appear as a stick but uses the same motion, `false` otherwise.
 * When `false`, this object will always remain visible when the "never_hidden"
 * midis2jam2 property is enabled.
 */
data class StickProperties(
    val stickModel: Spatial,
    val strikeSpeed: Double = DEFAULT_STRIKE_SPEED,
    val maxIdleAngle: Double = MAX_STICK_IDLE_ANGLE,
    val rotationAxis: Axis = Axis.X,
    val sticky: Boolean = true,
    val actualStick: Boolean = true,
)

/**
 * Returns data describing what the status of the stick is.
 *
 * @property strike If the stick just struck, this is the strike it struck for. `null` otherwise.
 * @property rotationAngle The current rotation angle of the stick.
 * @property strikingFor If the stick is rotating to strike a note, this is the note it's striking for. `null`
 * otherwise.
 */
data class StickStatus(
    val strike: NoteOn?,
    val rotationAngle: Float,
    val strikingFor: NoteOn?,
) {
    /** The velocity at which the striker struck at, or `0` if it did not strike this frame. */
    val velocity: Byte
        get() = strike?.velocity ?: 0
}

/**
 * Defines common stick models.
 */
enum class StickType(
    internal val modelName: String,
    internal val textureName: String,
) {
    /** The drum set stick. */
    DRUM_SET_STICK(
        modelName = "DrumSet_Stick.obj",
        textureName = "StickSkin.bmp",
    ),

    /** The left hand. */
    HAND_LEFT(
        modelName = "hand_left.obj",
        textureName = "hands.bmp",
    ),

    /** The right hand. */
    HAND_RIGHT(
        modelName = "hand_right.obj",
        textureName = "hands.bmp",
    ),
}
