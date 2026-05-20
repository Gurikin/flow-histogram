package org.gurikin.histogram.internal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class HistogramSourceData<S : Comparable<S>> : Comparable<S> {
    abstract val value: S

    abstract operator fun plus(other: S): HistogramSourceData<S>
    abstract operator fun minus(other: S): HistogramSourceData<S>
    abstract operator fun times(other: S): HistogramSourceData<S>
    abstract operator fun div(other: S): HistogramSourceData<S>
    abstract operator fun rem(other: S): HistogramSourceData<S>
    override fun compareTo(other: S): Int = value.compareTo(other)
}

@Serializable
@SerialName("Int")
data class IntHistogramSourceData(override val value: Int) : HistogramSourceData<Int>() {
    override fun plus(other: Int): HistogramSourceData<Int> = IntHistogramSourceData(value + other)

    override fun minus(other: Int): HistogramSourceData<Int> = IntHistogramSourceData(value - other)

    override fun times(other: Int): HistogramSourceData<Int> = IntHistogramSourceData(value * other)

    override fun div(other: Int): HistogramSourceData<Int> = IntHistogramSourceData(value / other)

    override fun rem(other: Int): HistogramSourceData<Int> = IntHistogramSourceData(value % other)
}

@Serializable
@SerialName("Long")
data class LongHistogramSourceData(override val value: Long) : HistogramSourceData<Long>() {
    override fun plus(other: Long): HistogramSourceData<Long> = LongHistogramSourceData(value + other)

    override fun minus(other: Long): HistogramSourceData<Long> = LongHistogramSourceData(value - other)

    override fun times(other: Long): HistogramSourceData<Long> = LongHistogramSourceData(value * other)

    override fun div(other: Long): HistogramSourceData<Long> = LongHistogramSourceData(value / other)

    override fun rem(other: Long): HistogramSourceData<Long> = LongHistogramSourceData(value % other)
}

@Serializable
@SerialName("Float")
data class FloatHistogramSourceData(override val value: Float) : HistogramSourceData<Float>() {
    override fun plus(other: Float): HistogramSourceData<Float> = FloatHistogramSourceData(value + other)

    override fun minus(other: Float): HistogramSourceData<Float> = FloatHistogramSourceData(value - other)

    override fun times(other: Float): HistogramSourceData<Float> = FloatHistogramSourceData(value * other)

    override fun div(other: Float): HistogramSourceData<Float> = FloatHistogramSourceData(value / other)

    override fun rem(other: Float): HistogramSourceData<Float> = FloatHistogramSourceData(value % other)
}

@Serializable
@SerialName("Double")
data class DoubleHistogramSourceData(override val value: Double) : HistogramSourceData<Double>() {
    override fun plus(other: Double): HistogramSourceData<Double> = DoubleHistogramSourceData(value + other)

    override fun minus(other: Double): HistogramSourceData<Double> = DoubleHistogramSourceData(value - other)

    override fun times(other: Double): HistogramSourceData<Double> = DoubleHistogramSourceData(value * other)

    override fun div(other: Double): HistogramSourceData<Double> = DoubleHistogramSourceData(value / other)

    override fun rem(other: Double): HistogramSourceData<Double> = DoubleHistogramSourceData(value % other)
}