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

package org.wysko.midis2jam2.instrument.family.ensemble

import com.jme3.math.Vector3f
import org.wysko.kmidi.midi.TimedArc
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.DivisiveSustainedInstrument
import org.wysko.midis2jam2.instrument.Instrument
import org.wysko.midis2jam2.instrument.PitchClassAnimator
import org.wysko.midis2jam2.util.*
import org.wysko.midis2jam2.world.modelD
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.DurationUnit

private val BASE_POSITION = Vector3f(0f, 29.5f, -152.65f)

/**
 * A modified version of the stage choir that animates applause.
 */
class ApplauseChoir(context: Midis2jam2, private val eventList: List<MidiEvent>) :
    DivisiveSustainedInstrument(context, eventList) {

    override val animators: List<PitchClassAnimator> = List(12) { ApplauseChoirPeep() }

    init {
        repeat(12) {
            with(geometry) {
                +node {
                    +animators[it].root
                    rot = v3(0, 11.2 + it * -5.636, 0)
                }
            }
        }
    }

    override fun findSimilar(): List<Instrument> =
        // Order matters here to preserve index
        context.instruments.filterIsInstance<StageChoir>() + context.instruments.filterIsInstance<ApplauseChoir>()

    override fun adjustForMultipleInstances(delta: Duration) {
        val indexForMoving = updateInstrumentIndex(delta)
        animators.forEach {
            if (indexForMoving >= 0) {
                it.root.localTranslation =
                    BASE_POSITION.clone().add(Vector3f(0f, 10f, -15f).mult(indexForMoving))
            } else {
                it.root.localTranslation =
                    BASE_POSITION.clone().add(Vector3f(0f, indexForMoving * 10f, indexForMoving * 10f))
            }
        }
    }

    private inner class ApplauseChoirPeep : PitchClassAnimator(
        context,
        TimedArc.fromNoteEvents(context.sequence, eventList.filterIsInstance<NoteEvent>())
    ) {
        private val bounceSpeed = 2 * Math.random() + 2f
        private var animInfluence = 0.0

        init {
            with(geometry) {
                +context.modelD("StageChoirNoBook.obj", "ChoirPeepOoh.png")
            }
            root.loc = BASE_POSITION
        }

        override fun tick(time: Duration, delta: Duration) {
            collector.advance(time)

            // Progress animation
            val duration = when {
                collector.currentTimedArcs.isNotEmpty() -> delta
                else -> -delta
            }
            animInfluence += duration.toDouble(DurationUnit.SECONDS).toFloat()
            animInfluence = animInfluence.coerceIn(0.0..1.0)

            // Set bounce
            animation.loc = v3(0, animation(time) * animInfluence, 0)
        }

        private fun animation(time: Duration) =
            ((-(((time.toDouble(DurationUnit.SECONDS) * bounceSpeed) % 1) - 0.5).pow(2) + 0.25) * 38).toFloat()
    }
}
