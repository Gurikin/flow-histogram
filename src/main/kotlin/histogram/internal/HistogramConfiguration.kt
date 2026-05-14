package org.gurikin.histogram.internal

import java.util.*
import kotlin.math.log2
import kotlinx.serialization.Serializable
import org.gurikin.histogram.internal.HistogramSourceTypesEnum.DOUBLE
import org.gurikin.histogram.internal.HistogramSourceTypesEnum.FLOAT
import org.gurikin.histogram.internal.HistogramSourceTypesEnum.INT
import org.gurikin.histogram.internal.HistogramSourceTypesEnum.LONG

@Serializable
class HistogramConfiguration<S : Comparable<S>>(
    val sourceType: HistogramSourceTypesEnum,
    val histogramBorder: Border<S>? = null,
    val minStep: S?,
    val valueList: List<S>? = null,
)

/**
 * Base formula for calculating histogram bins count $n = 1 + log_2(N)$
 * N - number of all possible elements in histogram's border:
 *  `N = (histogramConfiguration.histogramBorder.to - histogramConfiguration.histogramBorder.from) / histogramConfiguration.minStep`
 *
 * @return
 */
fun <S : Comparable<S>> HistogramConfiguration<S>.calcBinsCount(): Int {
    val stepCount: Double = this.getBorderLength() / getMinStep()
    return (1 + log2(stepCount)).toInt()
}

@Suppress("UNCHECKED_CAST")
private fun <S : Comparable<S>> HistogramConfiguration<S>.getBorderLength(): Double {
    val borderLength = when (this.sourceType) {
        INT -> {
            (this.histogramBorder as Border<Int>).borderLength()
        }

        LONG -> {
            (this.histogramBorder as Border<Long>).borderLength()
        }

        FLOAT -> {
            (this.histogramBorder as Border<Float>).borderLength()
        }

        DOUBLE -> {
            (this.histogramBorder as Border<Double>).borderLength()
        }

        else -> throw UnsupportedOperationException("Unknown type of histogram source")
    }
    return borderLength.toDouble()
}

private fun <S : Comparable<S>> HistogramConfiguration<S>.getMinStep(): Double {
    val step = when (this.sourceType) {
        INT -> {
            this.minStep as Int
        }

        LONG -> {
            this.minStep as Long
        }

        FLOAT -> {
            this.minStep as Float
        }

        DOUBLE -> {
            this.minStep as Double
        }

        else -> throw UnsupportedOperationException("Unknown type of histogram source")
    }
    return step.toDouble()
}