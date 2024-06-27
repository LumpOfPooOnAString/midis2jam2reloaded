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

import com.jme3.math.FastMath
import com.jme3.math.Quaternion
import com.jme3.scene.Node
import com.jme3.scene.Spatial
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.SustainedInstrument
import org.wysko.midis2jam2.instrument.algorithmic.StickType
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.instrument.family.percussion.CymbalAnimator
import org.wysko.midis2jam2.world.modelR
import kotlin.math.cos
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.SECONDS

/**
 * The Reverse Cymbal.
 *
 * The Reverse Cymbal animates using a custom cymbal animator class, [ReverseCymbalAnimator]. It animates the same way
 * as the standard [CymbalAnimator] class, but is a Kotlin object and provides one static method
 * [rotationAmount][ReverseCymbalAnimator.rotationAmount].
 *
 * The Reverse Cymbal discards the front end of each [NotePeriod] (the [MidiNoteOnEvent]) and instead animates solely
 * using the [MidiNoteOffEvent]. I call these "pseudo strikes" because they are not actually a strike, but rather a
 * point in time when the cymbal is struck and the animation is finished.
 *
 * For each note that the instrument plays, the cymbal is animated by [ReverseCymbalAnimator.rotationAmount]. Because
 * the value being passed to this method is decreasing, the cymbal will rotate in reverse.
 *
 * At the "end" of each note, the stick does a simple strike animation. The stick is rotated around the cymbal by the
 * type of note being played (C = 0 deg, C# = 30 deg, D = 60 deg, etc.).
 *
 * @constructor Constructs a new Reverse Cymbal.
 */
class ReverseCymbal(context: Midis2jam2, eventList: List<MidiEvent>) :
    SustainedInstrument(context, eventList) {
    /** The cymbal that animates backwards. */
    private val cymbal: Spatial =
        context.modelR("DrumSet_Cymbal.obj", "CymbalSkinSphereMap.bmp").also {
            geometry.attachChild(it)
            it.setLocalScale(2f)
        }

    /** A list of fake "pseudo" hits that correlate to the end times. */
    private val pseudoHits: MutableList<NoteEvent.NoteOn> =
        timedArcs.map {
            NoteEvent.NoteOn(it.end, it.noteOn.channel, it.note, 127)
        }.toMutableList().also {
            context.sequence.registerEvents(it)
        }

    /** Holds the stick and is rotated to the correct position. */
    private val stickNode: Node = Node().also { geometry.attachChild(it) }

    /** The stick that is used to hit the cymbal. */
    private val stick =
        Striker(
            context = context,
            strikeEvents = pseudoHits,
            stickModel = StickType.DRUM_SET_STICK,
        ).apply {
            setParent(stickNode)
            node.setLocalTranslation(0f, 0f, 15f)
        }

    override fun tick(
        time: Duration,
        delta: Duration,
    ) {
        super.tick(time, delta)

        // Animate the stick
        val results = stick.tick(time, delta)

        // Find the time of the next note end. If there is none, assume it will happen infinitely in the future.
        val nextHitTime =
            stick.peek()?.let {
                context.sequence.getTimeOf(it)
            } ?: Double.MAX_VALUE.seconds

        // Move the stick around the cymbal according to the note
        if (results.strikingFor != null && results.strike == null) {
            pseudoHits.firstOrNull()?.let {
                stickNode.localRotation = Quaternion().fromAngles(0f, ((it.note % 12) * 30).toFloat(), 0f)
            }
        }

        // Animate the cymbal
        cymbal.localRotation = Quaternion().fromAngles(ReverseCymbalAnimator.rotationAmount(nextHitTime - time), 0f, 0f)
    }

    override fun adjustForMultipleInstances(delta: Duration) {
        root.setLocalTranslation(0f, 20 * updateInstrumentIndex(delta), 0f)
    }

    init {
        geometry.setLocalTranslation(75f, 40f, -35f)
    }
}

/** The amplitude of the cymbal's rotation. */
private const val amplitude: Float = 2.5f

/** The frequency of the cymbal's rotation. */
private const val wobbleSpeed: Float = 4.5f

/** How quickly the cymbal's rotation slows down. */
private const val dampening: Float = 1.5f

/** Animates the reverse cymbal. */
@Suppress("DuplicatedCode")
object ReverseCymbalAnimator {
    /**
     * Calculates the amount of rotation to apply to the cymbal given the [amount of time until the next end of a
     * note][timeUntilPseudoStrike].
     */
    fun rotationAmount(timeUntilPseudoStrike: Duration): Float {
        return if (timeUntilPseudoStrike >= 0.seconds) {
            if (timeUntilPseudoStrike < 4.5.seconds) {
                val timeUntilStrikeInSeconds = timeUntilPseudoStrike.toDouble(SECONDS)
                (
                    amplitude * (
                        cos(timeUntilStrikeInSeconds * wobbleSpeed * FastMath.PI) / (
                            3 + timeUntilStrikeInSeconds.pow(
                                3.0,
                            ) * wobbleSpeed * dampening * FastMath.PI
                        )
                    )
                ).toFloat()
            } else {
                0F
            }
        } else {
            0F
        }
    }
}
