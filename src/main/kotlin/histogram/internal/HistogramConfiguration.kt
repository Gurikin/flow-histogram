package org.gurikin.histogram.internal

import kotlin.math.log2
import kotlinx.serialization.Serializable
import org.gurikin.histogram.internal.HistogramSourceTypesEnum.DOUBLE
import org.gurikin.histogram.internal.HistogramSourceTypesEnum.FLOAT
import org.gurikin.histogram.internal.HistogramSourceTypesEnum.INT
import org.gurikin.histogram.internal.HistogramSourceTypesEnum.LONG

@Serializable
class HistogramConfiguration<S : Comparable<S>>(
    val histogramBorder: Border<S>,
    val sourceType: HistogramSourceTypesEnum,
    val minStep: S,
)

/**
 * Base formula for calculating histogram bins count $n = 1 + log_2(N)$
 * N - number of all possible elements in histogram's border:
 *  `N = (histogramConfiguration.histogramBorder.to - histogramConfiguration.histogramBorder.from) / histogramConfiguration.minStep`
 *
 * @return
 */
@Suppress("UNCHECKED_CAST")
fun <S : Comparable<S>> HistogramConfiguration<S>.calcBinsCount(): Int {
    val stepCount: Double = when (this.sourceType) {
        INT -> {
            val border = (this.histogramBorder as Border<Int>).borderLength()
            val step = this.minStep as Int
            border.toDouble() / step.toDouble()
        }
        LONG -> {
            val border = (this.histogramBorder as Border<Long>).borderLength()
            val step = this.minStep as Long
            border.toDouble() / step.toDouble()
        }
        FLOAT -> {
            val border = (this.histogramBorder as Border<Float>).borderLength()
            val step = this.minStep as Float
            border.toDouble() / step.toDouble()
        }
        DOUBLE -> {
            val border = (this.histogramBorder as Border<Double>).borderLength()
            val step = this.minStep as Double
            border / step
        }

        else -> throw UnsupportedOperationException("Unknown type of histogram source")
    }
    return (1 + log2(stepCount)).toInt()
}