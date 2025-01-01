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

package org.wysko.midis2jam2.gui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import org.wysko.midis2jam2.gui.viewmodel.I18n

/**
 * A composable that allows the user to select a MIDI file.
 *
 * @param modifier The modifier to apply to this layout.
 * @param selectedMidiFile The currently selected MIDI file.
 * @param focusCount The number of times the focus has been requested.
 * @param openMidiFileBrowse The function to call when the user wants to browse for a MIDI file.
 */
@Composable
fun MidiFileSelector(
    modifier: Modifier = Modifier,
    selectedMidiFile: String,
    focusCount: Int,
    openMidiFileBrowse: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        TextField(
            modifier = Modifier.focusable(false).fillMaxWidth().focusRequester(focusRequester),
            value = selectedMidiFile,
            onValueChange = { },
            label = { Text(I18n["midi_file"].value) },
            singleLine = true,
            readOnly = true,
            trailingIcon = {
                ToolTip(I18n["browse"].value) {
                    IconButton({
                        openMidiFileBrowse()
                    }, Modifier.pointerHoverIcon(PointerIcon.Hand)) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = I18n["browse"].value)
                    }
                }
            },
            colors = TextFieldDefaults.colors()
        )
    }

    DisposableEffect(focusCount) {
        if (focusCount != 0) {
            focusRequester.requestFocus()
        }
        onDispose { }
    }
}