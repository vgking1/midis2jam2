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
package org.wysko.midis2jam2.particle

/** Generates particles. */
interface ParticleGenerator {
    /**
     * Updates the animation of this generator.
     *
     * @param delta  the amount of time since the last frame
     * @param active true if this particle generator should be generating
     */
    fun tick(delta: Float, active: Boolean)

    /** A particle as generated by a [ParticleGenerator]. */
    interface Particle {
        /**
         * Update animation.
         *
         * @param delta the amount of time since the last frame
         * @return true if this particle needs to remain, false otherwise
         */
        fun tick(delta: Float): Boolean
    }
}