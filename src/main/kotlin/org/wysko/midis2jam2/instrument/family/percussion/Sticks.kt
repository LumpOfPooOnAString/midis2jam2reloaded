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
import com.jme3.scene.Node
import org.wysko.kmidi.midi.event.NoteEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.StickType
import org.wysko.midis2jam2.instrument.algorithmic.Striker
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration

/** The sticks. */
class Sticks(context: Midis2jam2, hits: MutableList<NoteEvent.NoteOn>) : AuxiliaryPercussion(context, hits) {
    private val mainStick =
        Striker(
            context = context,
            strikeEvents = hits,
            stickModel = StickType.DRUM_SET_STICK,
            strikeSpeed = 2.0,
            maxIdleAngle = 30.0,
            actualStick = false,
        ).apply {
            setParent(geometry)
            offsetStick {
                it.move(2.5f, 0f, 0f)
                it.localRotation = Quaternion().fromAngles(0f, rad(20.0), 0f)
            }
        }

    /** Contains the right stick. */
    private val rightStickNode =
        Node().apply {
            attachChild(
                context.modelD("DrumSet_Stick.obj", "StickSkin.bmp").apply {
                    setLocalTranslation(-2.5f, 0f, 0f)
                    localRotation = Quaternion().fromAngles(0f, -rad(20.0), 0f)
                },
            )
        }.also {
            geometry.attachChild(it)
        }

    override fun tick(
        time: Duration,
        delta: Duration,
    ) {
        super.tick(time, delta)
        val results = mainStick.tick(time, delta)
        rightStickNode.localRotation = Quaternion().fromAngles(-results.rotationAngle, 0f, 0f)
    }

    init {
        with(geometry) {
            setLocalTranslation(-12f, 42.3f, -48.4f)
            localRotation = Quaternion().fromAngles(rad(90.0), rad(90.0), 0f)
        }
    }
}
