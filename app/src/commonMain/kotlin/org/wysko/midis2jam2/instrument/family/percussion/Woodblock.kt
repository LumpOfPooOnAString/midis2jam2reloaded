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

import com.jme3.math.Quaternion
import com.jme3.scene.Node
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.StickType
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration

/** The woodblock. High and low. */
class Woodblock(
    context: Midis2jam2,
    highHits: MutableList<NoteEvent.NoteOn>,
    lowHits: MutableList<NoteEvent.NoteOn>,
) : AuxiliaryPercussion(context, (highHits + lowHits).sortedBy { it.tick }.toMutableList()) {
    /** The Left woodblock anim node. */
    private val leftWoodblockNode =
        Node().apply {
            attachChild(context.modelD("WoodBlockHigh.obj", "SimpleWood.bmp"))
        }

    /** The Right woodblock anim node. */
    private val rightWoodblockNode =
        Node().apply {
            attachChild(context.modelD("WoodBlockLow.obj", "SimpleWood.bmp"))
        }

    private val leftStick =
        Striker(
            context = context,
            strikeEvents = highHits,
            stickModel = StickType.DRUM_SET_STICK,
        ).apply {
            offsetStick { it.move(0f, 0f, -1f) }
            node.move(0f, 0f, 13.5f)
            setParent(leftWoodblockNode)
        }

    private val rightStick =
        Striker(
            context = context,
            strikeEvents = lowHits,
            stickModel = StickType.DRUM_SET_STICK,
        ).apply {
            offsetStick { it.move(0f, 0f, -1f) }
            node.move(0f, 0f, 13.5f)
            setParent(rightWoodblockNode)
        }

    init {
        Node().apply {
            geometry.attachChild(this)
            attachChild(leftWoodblockNode)
            localRotation = Quaternion().fromAngles(0f, rad(5.0), 0f)
            setLocalTranslation(-5f, -0.3f, 0f)
        }
        Node().apply {
            geometry.attachChild(this)
            attachChild(rightWoodblockNode)
            localRotation = Quaternion().fromAngles(0f, rad(3.0), 0f)
        }
        placement.setLocalTranslation(0f, 40f, -90f)
        placement.localRotation = Quaternion().fromAngles(rad(10.0), 0f, 0f)
    }

    override fun tick(
        time: Duration,
        delta: Duration,
    ) {
        super.tick(time, delta)

        val rightResults = rightStick.tick(time, delta)
        recoilDrum(
            drum = rightWoodblockNode,
            velocity = rightResults.strike?.velocity ?: 0,
            delta = delta,
        )

        val leftRights = leftStick.tick(time, delta)
        recoilDrum(
            drum = leftWoodblockNode,
            velocity = leftRights.strike?.velocity ?: 0,
            delta = delta,
        )
    }
}
