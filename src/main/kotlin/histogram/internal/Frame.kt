package org.gurikin.histogram.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Frame<S : Comparable<S>> : Comparable<Frame<S>> {
    abstract val value: S

    abstract operator fun plus(other: S): Frame<S>
    abstract operator fun minus(other: S): Frame<S>
    abstract operator fun times(other: S): Frame<S>
    abstract operator fun div(other: S): Frame<S>
    abstract operator fun rem(other: S): Frame<S>
    abstract override fun compareTo(other: Frame<S>): Int
}

@Serializable
@SerialName("IntFrame")
data class IntFrame(override val value: Int) : Frame<Int>() {
    override fun plus(other: Int): Frame<Int> = IntFrame(value + other)

    override fun minus(other: Int): Frame<Int> = IntFrame(value - other)

    override fun times(other: Int): Frame<Int> = IntFrame(value * other)

    override fun div(other: Int): Frame<Int> = IntFrame(value / other)

    override fun rem(other: Int): Frame<Int> = IntFrame(value % other)

    override fun compareTo(other: Frame<Int>): Int = value.compareTo(other.value)
}

@Serializable
@SerialName("LongFrame")
data class LongFrame(override val value: Long) : Frame<Long>() {
    override fun plus(other: Long): Frame<Long> = LongFrame(value + other)

    override fun minus(other: Long): Frame<Long> = LongFrame(value - other)

    override fun times(other: Long): Frame<Long> = LongFrame(value * other)

    override fun div(other: Long): Frame<Long> = LongFrame(value / other)

    override fun rem(other: Long): Frame<Long> = LongFrame(value % other)

    override fun compareTo(other: Frame<Long>): Int = value.compareTo(other.value)
}

@Serializable
@SerialName("FloatFrame")
data class FloatFrame(override val value: Float) : Frame<Float>() {
    override fun plus(other: Float): Frame<Float> = FloatFrame(value + other)

    override fun minus(other: Float): Frame<Float> = FloatFrame(value - other)

    override fun times(other: Float): Frame<Float> = FloatFrame(value * other)

    override fun div(other: Float): Frame<Float> = FloatFrame(value / other)

    override fun rem(other: Float): Frame<Float> = FloatFrame(value % other)

    override fun compareTo(other: Frame<Float>): Int = value.compareTo(other.value)

}

@Serializable
@SerialName("DoubleFrame")
data class DoubleFrame(override val value: Double) : Frame<Double>() {
    override fun plus(other: Double): Frame<Double> = DoubleFrame(value + other)

    override fun minus(other: Double): Frame<Double> = DoubleFrame(value - other)

    override fun times(other: Double): Frame<Double> = DoubleFrame(value * other)

    override fun div(other: Double): Frame<Double> = DoubleFrame(value / other)

    override fun rem(other: Double): Frame<Double> = DoubleFrame(value % other)

    override fun compareTo(other: Frame<Double>): Int = value.compareTo(other.value)
}