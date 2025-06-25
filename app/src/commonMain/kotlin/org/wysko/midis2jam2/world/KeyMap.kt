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

package org.wysko.midis2jam2.world

import com.jme3.app.Application
import com.jme3.input.KeyInput
import com.jme3.input.controls.ActionListener
import com.jme3.input.controls.KeyTrigger
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.wysko.midis2jam2.util.resourceToString

/**
 * Stores a map between an action name and a key.
 *
 * @property name The action name.
 * @property key The key name, as defined in [KeyInput].
 */
@Serializable
data class KeyMap(val name: String, val key: String) {
    companion object {
        /**
         * Registers key mappings for an application using the given listener.
         *
         * @param app The application instance to register the key mappings.
         * @param listener The action listener to be associated with the key mappings.
         */
        fun registerMappings(app: Application, listener: ActionListener) {
            val keyMaps: Array<KeyMap> = Json.decodeFromString(resourceToString("/keymap.json"))
            val javaFields = KeyInput::class.java.declaredFields

            keyMaps.forEach { (name, key) ->
                javaFields.firstOrNull { it.name == key }?.let { field ->
                    with(app.inputManager) {
                        addMapping(name, KeyTrigger(field.getInt(KeyInput::class.java)))
                        addListener(listener, name)
                    }
                }
            }
        }
    }
}
