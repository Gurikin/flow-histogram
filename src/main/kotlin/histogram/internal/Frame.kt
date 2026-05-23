package org.gurikin.histogram.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Frame<S> {
    abstract fun value(): S
    abstract operator fun plus(other: Frame<S>): Frame<S>
    abstract operator fun minus(other: Frame<S>): Frame<S>
    abstract operator fun times(other: Frame<S>): Frame<S>
    abstract operator fun div(other: Frame<S>): Frame<S>
    abstract operator fun rem(other: Frame<S>): Frame<S>
    abstract operator fun compareTo(other: Frame<S>): Int
    abstract operator fun plus(other: S): Frame<S>
    abstract operator fun minus(other: S): Frame<S>
    abstract operator fun times(other: S): Frame<S>
    abstract operator fun div(other: S): Frame<S>
    abstract operator fun rem(other: S): Frame<S>
    abstract operator fun compareTo(other: S): Int
}

@Serializable
@SerialName("IntFrame")
data class IntFrame(val value: Int) : Frame<Int>() {
    override fun value(): Int = value

    override fun plus(other: Frame<Int>): Frame<Int> = IntFrame(value + other.value())

    override fun minus(other: Frame<Int>): Frame<Int> = IntFrame(value - other.value())

    override fun times(other: Frame<Int>): Frame<Int> = IntFrame(value * other.value())

    override fun div(other: Frame<Int>): Frame<Int> = IntFrame(value / other.value())

    override fun rem(other: Frame<Int>): Frame<Int> = IntFrame(value % other.value())

    override fun compareTo(other: Frame<Int>): Int = value.compareTo(other.value())

    override fun plus(other: Int): Frame<Int> = IntFrame(value + other)

    override fun minus(other: Int): Frame<Int> = IntFrame(value - other)

    override fun times(other: Int): Frame<Int> = IntFrame(value * other)

    override fun div(other: Int): Frame<Int> = IntFrame(value / other)

    override fun rem(other: Int): Frame<Int> = IntFrame(value % other)

    override fun compareTo(other: Int): Int = value.compareTo(other)
}

@Serializable
@SerialName("LongFrame")
data class LongFrame(val value: Long) : Frame<Long>() {
    override fun value(): Long = value

    override fun plus(other: Frame<Long>): Frame<Long> = LongFrame(value + other.value())

    override fun minus(other: Frame<Long>): Frame<Long> = LongFrame(value - other.value())

    override fun times(other: Frame<Long>): Frame<Long> = LongFrame(value * other.value())

    override fun div(other: Frame<Long>): Frame<Long> = LongFrame(value / other.value())

    override fun rem(other: Frame<Long>): Frame<Long> = LongFrame(value % other.value())

    override fun compareTo(other: Frame<Long>): Int = value.compareTo(other.value())

    override fun plus(other: Long): Frame<Long> = LongFrame(value + other)

    override fun minus(other: Long): Frame<Long> = LongFrame(value - other)

    override fun times(other: Long): Frame<Long> = LongFrame(value * other)

    override fun div(other: Long): Frame<Long> = LongFrame(value / other)

    override fun rem(other: Long): Frame<Long> = LongFrame(value % other)

    override fun compareTo(other: Long): Int = value.compareTo(other)
}

@Serializable
@SerialName("FloatFrame")
data class FloatFrame(val value: Float) : Frame<Float>() {
    override fun value(): Float = value

    override fun plus(other: Frame<Float>): Frame<Float> = FloatFrame(value + other.value())

    override fun minus(other: Frame<Float>): Frame<Float> = FloatFrame(value - other.value())

    override fun times(other: Frame<Float>): Frame<Float> = FloatFrame(value * other.value())

    override fun div(other: Frame<Float>): Frame<Float> = FloatFrame(value / other.value())

    override fun rem(other: Frame<Float>): Frame<Float> = FloatFrame(value % other.value())

    override fun compareTo(other: Frame<Float>): Int = value.compareTo(other.value())

    override fun plus(other: Float): Frame<Float> = FloatFrame(value + other)

    override fun minus(other: Float): Frame<Float> = FloatFrame(value - other)

    override fun times(other: Float): Frame<Float> = FloatFrame(value * other)

    override fun div(other: Float): Frame<Float> = FloatFrame(value / other)

    override fun rem(other: Float): Frame<Float> = FloatFrame(value % other)

    override fun compareTo(other: Float): Int = value.compareTo(other)
}

@Serializable
@SerialName("DoubleFrame")
data class DoubleFrame(val value: Double) : Frame<Double>() {
    override fun value(): Double = value

    override fun plus(other: Frame<Double>): Frame<Double> = DoubleFrame(value + other.value())

    override fun minus(other: Frame<Double>): Frame<Double> = DoubleFrame(value - other.value())

    override fun times(other: Frame<Double>): Frame<Double> = DoubleFrame(value * other.value())

    override fun div(other: Frame<Double>): Frame<Double> = DoubleFrame(value / other.value())

    override fun rem(other: Frame<Double>): Frame<Double> = DoubleFrame(value % other.value())

    override fun compareTo(other: Frame<Double>): Int = value.compareTo(other.value())

    override fun plus(other: Double): Frame<Double> = DoubleFrame(value + other)

    override fun minus(other: Double): Frame<Double> = DoubleFrame(value - other)

    override fun times(other: Double): Frame<Double> = DoubleFrame(value * other)

    override fun div(other: Double): Frame<Double> = DoubleFrame(value / other)

    override fun rem(other: Double): Frame<Double> = DoubleFrame(value % other)

    override fun compareTo(other: Double): Int = value.compareTo(other)
}