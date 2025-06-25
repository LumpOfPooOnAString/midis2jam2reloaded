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
package org.wysko.midis2jam2.instrument.family.strings

import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import org.wysko.kmidi.midi.event.MidiEvent
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.util.Utils.rad
import org.wysko.midis2jam2.world.modelD
import kotlin.time.Duration

/** The upright bass. */
class AcousticBass(context: Midis2jam2, events: List<MidiEvent>, style: PlayingStyle) :
    StringFamilyInstrument(
        context,
        events,
        style == PlayingStyle.ARCO,
        20.0,
        Vector3f(0.75f, 0.75f, 0.75f),
        intArrayOf(28, 33, 38, 43),
        context.modelD(
            "DoubleBass.obj",
            if (style == PlayingStyle.ARCO) "DoubleBassSkin.bmp" else "DoubleBassSkinAlt.png"
        )
    ) {
    override fun adjustForMultipleInstances(delta: Duration) {
        root.setLocalTranslation(-25 * updateInstrumentIndex(delta), 0f, 0f)
    }

    /** The acoustic bass can be played two ways in MIDI, arco (Contrabass) and pizzicato (Acoustic Bass) */
    enum class PlayingStyle {
        /** Arco playing style. */
        ARCO,

        /** Pizzicato playing style. */
        PIZZICATO
    }

    init {
        placement.setLocalTranslation(-50f, 46f, -95f)
        geometry.setLocalScale(2.5f)
        geometry.localRotation = Quaternion().fromAngles(rad(-15.0), rad(45.0), 0f)
    }
}
