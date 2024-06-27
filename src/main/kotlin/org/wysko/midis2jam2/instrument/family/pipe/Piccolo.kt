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
package org.wysko.midis2jam2.instrument.family.pipe

import com.jme3.math.Quaternion
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.algorithmic.HandPositionFingeringManager
import org.wysko.midis2jam2.instrument.clone.ClonePitchBendConfiguration
import org.wysko.midis2jam2.particle.SteamPuffer
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.modelR

private val FINGERING_MANAGER: HandPositionFingeringManager = HandPositionFingeringManager.from(Piccolo::class)

/** The Piccolo. */
class Piccolo(context: Midis2jam2, events: List<MidiEvent>) :
    InstrumentWithHands(context, events, PiccoloClone::class, FINGERING_MANAGER) {

    override val pitchBendConfiguration: ClonePitchBendConfiguration = ClonePitchBendConfiguration(reversed = true)

    /**
     * A single Piccolo.
     */
    inner class PiccoloClone : FluteAndPiccoloClone(this@Piccolo, SteamPuffer.SteamPuffTexture.NORMAL, 1f) {
        init {
            val horn = context.modelR(
                "Piccolo.obj",
                "CymbalSkinSphereMap.bmp"
            )
            loadHands()
            puffer.root.localRotation = Quaternion().fromAngles(floatArrayOf(0f, 0f, rad(-90.0)))
            puffer.root.setLocalTranslation(0f, -8.6f, 0f)
            geometry.attachChild(horn)
        }
    }

    init {
        // Piccolo positioning
        geometry.setLocalTranslation(5f, 58f, -20f)
        geometry.localRotation = Quaternion().fromAngles(rad(-80.0), rad(-53.0), rad(0.0))
    }
}
