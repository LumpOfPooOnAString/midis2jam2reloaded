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
package org.wysko.midis2jam2.instrument.clone

import org.wysko.midis2jam2.instrument.family.pipe.InstrumentWithHands
import org.wysko.midis2jam2.particle.SteamPuffer
import org.wysko.midis2jam2.particle.SteamPuffer.Texture
import org.wysko.midis2jam2.util.unaryPlus
import kotlin.time.Duration

/** Any clone that visualizes playing by using a [SteamPuffer]. */
abstract class CloneWithPuffer protected constructor(
    parent: InstrumentWithHands,
    puffType: Texture,
    pufferScale: Float
) : CloneWithHands(parent, 0f) {

    /** The steam puffer. */
    val puffer: SteamPuffer =
        SteamPuffer(parent.context, puffType, pufferScale.toDouble(), SteamPuffer.Behavior.Outwards)

    override fun tick(time: Duration, delta: Duration) {
        super.tick(time, delta)
        puffer.tick(delta, isPlaying)
    }

    init {
        with(geometry) {
            +puffer.root
        }
    }
}
