package org.gurikin.histogram.internal

import java.math.BigDecimal
import java.math.RoundingMode
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

const val CHUNK_BIN_COUNT = 10

@Suppress("UNCHECKED_CAST")
fun <S : Comparable<S>> HistogramConfiguration<S>.frameFactory(value: Any): Frame<S> {
    if (value is Number) {
        return when (this.sourceType) {
            INT -> IntFrame(value.toInt())
            LONG -> LongFrame(value.toLong())
            FLOAT -> FloatFrame(value.toFloat())
            DOUBLE -> DoubleFrame(value.toDouble())
            else -> throw UnsupportedOperationException("Unknown type for frame ${value::class}")
        } as Frame<S>
    } else {
        throw UnsupportedOperationException("Unknown type for frame ${value::class}")
    }
}


fun <S : Comparable<S>> HistogramConfiguration<S>.generateChunks(histogramBuilder: HistogramBuilder<S>): Set<Chunk<S>> {
    val chunks = TreeSet<Chunk<S>>()
    val chunksCount = this.calcChunksCount()
    val borderLength = this.getBorderLength()
    val chunkBorderLength = borderLength / chunksCount
    var border: Border<S>
    (0..chunksCount).forEach {
        border = Border(
            this.frameFactory(it * chunkBorderLength),
            this.frameFactory(it * chunkBorderLength + chunkBorderLength - 1)
        )
        val chunk = Chunk(histogram = histogramBuilder.initHistogram(border, CHUNK_BIN_COUNT), chunkId = ChunkId())
        chunks.add(chunk)
    }
    return chunks
}

fun <S : Comparable<S>> HistogramConfiguration<S>.calcChunksCount(): Int {
    val stepCount = this.getBorderLength() / this.getMinStep()
    return (this.calcBinsCount() / (this.getMinStep())).toInt()
}

/**
 * Base formula for calculating histogram bins count $n = 1 + log_2(N)$
 * N - number of all possible elements in histogram's border:
 *  `N = (histogramConfiguration.histogramBorder.to - histogramConfiguration.histogramBorder.from) / histogramConfiguration.minStep`
 *
 * @return
 */
fun <S : Comparable<S>> HistogramConfiguration<S>.calcBinsCount(): Int {
    val stepCount: Double = this.getBorderLength() / this.getMinStep()
    return BigDecimal(1 + log2(stepCount)).setScale(0, RoundingMode.HALF_EVEN).toInt()
}

@Suppress("UNCHECKED_CAST")
private fun <S : Comparable<S>> HistogramConfiguration<S>.getBorderLength(): Double {
    return when (this.sourceType) {
        INT -> {
            (this.histogramBorder as Border<Int>).borderIntLength()
        }

        LONG -> {
            (this.histogramBorder as Border<Long>).borderLongLength()
        }

        FLOAT -> {
            (this.histogramBorder as Border<Float>).borderFloatLength()
        }

        DOUBLE -> {
            (this.histogramBorder as Border<Double>).borderDoubleLength()
        }

        else -> throw UnsupportedOperationException("Unknown type of histogram source")
    }.toDouble()
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