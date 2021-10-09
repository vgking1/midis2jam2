/*
 * Copyright (C) 2021 Jacob Wysko
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
package org.wysko.midis2jam2.instrument

import com.jme3.scene.Node
import org.jetbrains.annotations.Contract
import org.wysko.midis2jam2.Midis2jam2
import org.wysko.midis2jam2.util.InstrumentTransition
import org.wysko.midis2jam2.util.Utils
import kotlin.math.max

/**
 * Any visual representation of a MIDI instrument. midis2jam2 displays separate instruments for
 * each channel, and also creates new instruments when the program of a channel changes (i.e., the MIDI instrument of
 * the channel changes).
 *
 * Classes that implement Instrument are responsible for handling [tick], which updates the current animation and
 * note handling for every call.
 */
abstract class Instrument protected constructor(
    /** Context to the main class. */
    val context: Midis2jam2,
) {
    /** Used for moving the instrument when there are two or more consecutively visible instruments of the same type. */
    val offsetNode: Node = Node()

    /** Used for general positioning and rotation of the instrument. */
    val highestLevel: Node = Node()

    /** Contains instrument geometry, ideally through further sub-nodes. */
    val instrumentNode: Node = Node()

    /**
     * When true, this instrument should be displayed on the screen. Otherwise, it should not. The positions of
     * instruments rely on this variable.
     */
    var isVisible: Boolean = false

    /**
     * The index of this instrument in the stack of similar instruments. Can be a decimal when instrument transition
     * easing is enabled.
     */
    private var index = 0.0

    /**
     * Updates the animation and other necessary frame-dependent calculations.
     *
     * @param time  the current time since the beginning of the MIDI file, expressed in seconds
     * @param delta the amount of time since the last call this method, expressed in seconds
     */
    abstract fun tick(time: Double, delta: Float)

    /**
     * Calculates if this instrument is visible at a given time.
     *
     * TODO criteria
     */
    abstract fun calcVisibility(time: Double): Boolean

    /**
     * Returns the index of this instrument in the list of other instruments of this type that are visible.
     *
     * @param delta the amount of time that has passed since the last frame
     */
    @Contract(pure = true)
    protected fun indexForMoving(delta: Float): Float {
        val target: Int = if (isVisible) {
            max(0, context.instruments
                .filter { e: Instrument -> this.javaClass.isInstance(e) && e.isVisible }
                .toList().indexOf(this))
        } else {
            context.instruments.filter { e: Instrument -> this.javaClass.isInstance(e) && e.isVisible }.size - 1
        }
        val transitionSpeed = context.settings.transitionSpeed
        return if (transitionSpeed != InstrumentTransition.NONE) {
            val animationCoefficient = transitionSpeed.speed
            index += delta * TRANSITION_SPEED * (target - index) / animationCoefficient
            index = index.coerceAtMost(context.instruments.filter { this.javaClass.isInstance(it) }.size.toDouble())
            index.toFloat()
        } else {
            target.toFloat()
        }
    }

    /** Calculates and moves this instrument for when multiple instances of this instrument are visible. */
    protected abstract fun moveForMultiChannel(delta: Float)

    protected fun setVisibility(time: Double) {
        isVisible = calcVisibility(time)
        instrumentNode.cullHint = Utils.cullHint(isVisible)
    }

    companion object {
        /** How fast instruments move when transitioning. */
        private const val TRANSITION_SPEED = 2500
    }

    init {
        /* Connect node tree */
        highestLevel.attachChild(instrumentNode)
        offsetNode.attachChild(highestLevel)
        context.rootNode.attachChild(offsetNode)
    }
}