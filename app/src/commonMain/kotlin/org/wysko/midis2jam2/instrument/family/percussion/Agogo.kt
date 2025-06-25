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
package org.wysko.midis2jam2.instrument.family.percussion

import com.jme3.math.Vector3f
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.StickType
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.util.max
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration

private val BASE_POSITION = Vector3f(-5f, 50f, -85f)

/** The Agogo. */
class Agogo(
    context: Midis2jam2,
    highHits: MutableList<NoteEvent.NoteOn>,
    lowHits: MutableList<NoteEvent.NoteOn>,
) : AuxiliaryPercussion(context, (highHits + lowHits).sortedBy { it.tick }.toMutableList()) {
    private val leftStick =
        Striker(
            context = context,
            strikeEvents = highHits,
            stickModel = StickType.DRUM_SET_STICK,
        ).apply {
            setParent(recoilNode)
            node.move(3f, 0f, 13f)
        }

    private val rightStick =
        Striker(
            context = context,
            strikeEvents = lowHits,
            stickModel = StickType.DRUM_SET_STICK,
        ).apply {
            setParent(recoilNode)
            node.move(10f, 0f, 11f)
        }

    init {
        recoilNode.attachChild(context.modelD("Agogo.obj", "HornSkinGrey.bmp"))
        geometry.localTranslation = BASE_POSITION
    }

    override fun tick(
        time: Duration,
        delta: Duration,
    ) {
        super.tick(time, delta)

        val leftResults = leftStick.tick(time, delta)
        val rightResults = rightStick.tick(time, delta)

        recoilDrum(
            drum = recoilNode,
            velocity = max(leftResults.strike?.velocity ?: 0, rightResults.strike?.velocity ?: 0),
            delta = delta,
        )
    }
}
