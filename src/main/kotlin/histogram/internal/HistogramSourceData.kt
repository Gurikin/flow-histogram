package org.gurikin.histogram.internal

abstract class HistogramSourceData<S>(open val value: S) : Comparable<S> {
    abstract operator fun plus(some: S): HistogramSourceData<S>
    abstract operator fun minus(some: S): HistogramSourceData<S>
    abstract operator fun times(some: S): HistogramSourceData<S>
    abstract operator fun div(some: S): HistogramSourceData<S>
}

data class IntHistogramSourceData(override val value: Int) : HistogramSourceData<Int>(value) {
    override fun plus(some: Int): HistogramSourceData<Int> = IntHistogramSourceData(value + some)

    override fun minus(some: Int): HistogramSourceData<Int> = IntHistogramSourceData(value - some)

    override fun times(some: Int): HistogramSourceData<Int> = IntHistogramSourceData(value * some)

    override fun div(some: Int): HistogramSourceData<Int> = IntHistogramSourceData(value / some)

    override fun compareTo(other: Int): Int = value.compareTo(other)
}

data class LongHistogramSourceData(override val value: Long) : HistogramSourceData<Long>(value) {
    override fun plus(some: Long): HistogramSourceData<Long> = LongHistogramSourceData(value + some)

    override fun minus(some: Long): HistogramSourceData<Long> = LongHistogramSourceData(value - some)

    override fun times(some: Long): HistogramSourceData<Long> = LongHistogramSourceData(value * some)

    override fun div(some: Long): HistogramSourceData<Long> = LongHistogramSourceData(value / some)

    override fun compareTo(other: Long): Int = value.compareTo(other)
}

data class FloatHistogramSourceData(override val value: Float) : HistogramSourceData<Float>(value) {
    override fun plus(some: Float): HistogramSourceData<Float> = FloatHistogramSourceData(value + some)

    override fun minus(some: Float): HistogramSourceData<Float> = FloatHistogramSourceData(value - some)

    override fun times(some: Float): HistogramSourceData<Float> = FloatHistogramSourceData(value * some)

    override fun div(some: Float): HistogramSourceData<Float> = FloatHistogramSourceData(value / some)

    override fun compareTo(other: Float): Int = value.compareTo(other)
}

data class DoubleHistogramSourceData(override val value: Double) : HistogramSourceData<Double>(value) {
    override fun plus(some: Double): HistogramSourceData<Double> = DoubleHistogramSourceData(value + some)

    override fun minus(some: Double): HistogramSourceData<Double> = DoubleHistogramSourceData(value - some)

    override fun times(some: Double): HistogramSourceData<Double> = DoubleHistogramSourceData(value * some)

    override fun div(some: Double): HistogramSourceData<Double> = DoubleHistogramSourceData(value / some)

    override fun compareTo(other: Double): Int = value.compareTo(other)
}