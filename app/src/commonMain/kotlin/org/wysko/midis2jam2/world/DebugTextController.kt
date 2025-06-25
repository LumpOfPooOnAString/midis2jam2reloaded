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

import com.jme3.font.BitmapText
import com.jme3.material.Material
import com.jme3.material.RenderState
import com.jme3.math.ColorRGBA
import com.jme3.math.Quaternion
import com.jme3.math.Vector3f
import com.jme3.scene.Geometry
import com.jme3.scene.Spatial
import com.jme3.scene.shape.Quad
//import org.lwjgl.opengl.GL11
import org.wysko.gervill.JwRealTimeSequencer
//import org.wysko.midis2jam2.DesktopMidis2jam2
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.instrument.family.percussion.drumset.TypicalDrumSet
import org.wysko.midis2jam2.util.ch
import org.wysko.midis2jam2.util.wrap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit.SECONDS

private val OPERATING_SYSTEM by lazy {
    "${System.getProperty("os.arch")} / ${System.getProperty("os.name")} / ${
        System.getProperty(
            "os.version",
        )
    }"
}
private val GL_RENDERER: String by lazy { /*runCatching { GL11.glGetString(GL11.GL_RENDERER) }.getOrNull() ?: "UNKNOWN GL_RENDERER"*/ "TODO" }// TODO
private val JVM_INFORMATION by lazy {
    "${
        System.getProperty(
            "java.vm.name"
        )
    }, ${System.getProperty("java.vm.vendor")}, ${System.getProperty("java.vm.version")}"
}

/**
 * Draws debug text on the screen.
 *
 * @property context Context to the main class.
 */
class DebugTextController(val context: Midis2jam2) {
    private val text =
        BitmapText(context.assetManager.loadFont("Interface/Fonts/Console.fnt")).apply {
            context.app.guiNode.attachChild(this)
            setLocalTranslation(16f, context.app.viewPort.camera.height - 16f, 0f)
            cullHint = Spatial.CullHint.Always
        }

    private val percussionText =
        BitmapText(context.assetManager.loadFont("Interface/Fonts/Console.fnt")).apply {
            context.app.guiNode.attachChild(this)
            setLocalTranslation(1024f, context.app.viewPort.camera.height - 16f, 0f)
            cullHint = Spatial.CullHint.Always
        }

    private val darkBackground =
        Geometry("DebugDarken", Quad(10000f, 10000f)).apply {
            material =
                Material(context.assetManager, "Common/MatDefs/Misc/Unshaded.j3md").apply {
                    setColor("Color", ColorRGBA(0f, 0f, 0f, 0.5f))
                    additionalRenderState.blendMode = RenderState.BlendMode.Alpha
                }
            setLocalTranslation(0f, 0f, -1f)
            context.app.guiNode.attachChild(this)
            cullHint = Spatial.CullHint.Always
        }

    private val syncIndicator =
        Geometry("SyncIndicator", Quad(100f, 16f)).apply {
            material =
                Material(context.assetManager, "Common/MatDefs/Misc/Unshaded.j3md").apply {
                    setColor("Color", ColorRGBA(0f, 0f, 0f, 1.0f))
                }
            setLocalTranslation(500f, context.app.viewPort.camera.height - 32f, 0f)
            context.app.guiNode.attachChild(this)
            cullHint = Spatial.CullHint.Always
        }

    private var enabled: Boolean = false
        set(value) {
            text.cullHint = value.ch
            percussionText.cullHint = value.ch
            darkBackground.cullHint = value.ch
            syncIndicator.cullHint = value.ch
            field = value
        }

    /**
     * Toggles the visibility of the debug text.
     */
    fun toggle(): Unit = run { enabled = !enabled }

    /**
     * Updates the debug text.
     *
     * @param tpf the time per frame
     */
    fun tick(tpf: Duration) {
        if (enabled) {
            with(text) { text = context.debugText(context.time, tpf) }
            with(percussionText) { text = "" }
//            val drift =
//                (((context as DesktopMidis2jam2).sequencer as JwRealTimeSequencer).time.seconds - context.time).absoluteValue.toDouble(SECONDS)
//            syncIndicator.setLocalScale((drift * 5f).toFloat(), 1f, 1f)
//            syncIndicator.material.setColor(
//                "Color",
//                ColorRGBA(0f, 1f, 0f, 1f).interpolateLocal(ColorRGBA.Red, (drift * 10).coerceIn(0.0, 1.0).toFloat())
//            )
            // TODO
        }
    }
}

/**
 * Generates debug text.
 */
private fun Midis2jam2.debugText(
    time: Duration,
    delta: Duration,
): String {
    return buildString {
        // midis2jam2 version and build
        append(
            "midis2jam2 v${this@debugText.version} (built at ${this@debugText.build})\n",
        )

        // computer operating system and renderer
        appendLine()
        appendLine("OS:  $OPERATING_SYSTEM")
        appendLine("GPU: $GL_RENDERER")
        appendLine("JVM: $JVM_INFORMATION")
        appendLine(
            "JRE: ${
                with(Runtime.getRuntime()) {
                    "${availableProcessors()} Cores / ${freeMemory() / 1024 / 1024}/${totalMemory() / 1024 / 1024} MB / ${maxMemory() / 1024 / 1024}MB max"
                }
            }",
        )
        appendLine("${"%.0f".format(1 / delta.toDouble(SECONDS))} FPS")

        // settings
        appendLine()
        appendLine("Settings:")
        appendLine(this@debugText.configs.joinToString().wrap(80))

        appendLine()
        appendLine("File:")
        with(this@debugText.sequence) {
            appendLine("$fileName - ${smf.tpq} TPQN")
            appendLine("${time}s / ${duration}s")
//            appendLine("$specification") TODO implement specification in kmidi
        }

        // camera position and rotation
        appendLine()
        appendLine("Camera:")
        appendLine("${this@debugText.app.camera.location.sigFigs()} / ${this@debugText.app.camera.rotation.sigFigs()}")
        appendLine(this@debugText.cameraState)
        appendLine(this@debugText.cameraSpeed)

        // sequencer
//        if (this@debugText is DesktopMidis2jam2) {
//            val seq = this@debugText.sequencer as JwRealTimeSequencer
//            appendLine()
//            appendLine("Sequencer time: ${seq.time}")
//            appendLine("Our time:       ${this@debugText.time}")
//            appendLine("Drift:          ${seq.time.seconds - time}")
//            appendLine("StartLocal:     ${seq.startTimeLocal}")
//            appendLine("StartGlobal:    ${seq.startTimeGlobal}")
//        }
        // TODO

        appendLine()
        appendLine("Drum set:")
        appendLine("isVisible: ${this@debugText.drumSetVisibilityManager.isVisible}")
        appendLine(
            "currentlyVisible: ${
                this@debugText.drumSetVisibilityManager.currentlyVisibleDrumSet?.let {
                    "${it::class.simpleName} ${if (it is TypicalDrumSet) it.shellStyle.toString() else ""}"
                } ?: "null"
            }"
        )
        appendLine()
        appendLine("Lyrics")
        appendLine(this@debugText.lyricController?.debugInfo(this@debugText.time))

        // instruments strings
        appendLine()
        appendLine("Instruments:")
        append("${this@debugText.instruments.joinToString("")}\n")
    }
}

private fun Quaternion.sigFigs(): String = "%5.2f / %5.2f / %5.2f / %5.2f".format(x, y, z, w)
private fun Vector3f.sigFigs(): String = "%7.2f / %7.2f / %7.2f".format(x, y, z)
