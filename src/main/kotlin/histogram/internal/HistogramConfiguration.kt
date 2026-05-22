package org.gurikin.histogram.internal

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.util.TreeSet
import kotlin.math.log2
import kotlinx.serialization.Serializable
import org.gurikin.histogram.internal.HistogramSourceTypesEnum.DOUBLE
import org.gurikin.histogram.internal.HistogramSourceTypesEnum.FLOAT
import org.gurikin.histogram.internal.HistogramSourceTypesEnum.INT
import org.gurikin.histogram.internal.HistogramSourceTypesEnum.LONG

@Serializable
class HistogramConfiguration<S : Comparable<S>>(
    val sourceType: HistogramSourceTypesEnum,
    val histogramBorder: Border<Frame<S>>? = null,
    val minStep: S?,
    val valueList: List<S>? = null,
)

const val CHUNK_BIN_COUNT = 10

fun <S: Comparable<S>> HistogramConfiguration<S>.frameFactory(value: Any): Frame<S> {
    if (value is Number) {
        return when (this.sourceType) {
            HistogramSourceTypesEnum.INT -> IntFrame(value.toInt())
            HistogramSourceTypesEnum.LONG -> LongFrame(value.toLong())
            HistogramSourceTypesEnum.FLOAT -> FloatFrame(value.toFloat())
            HistogramSourceTypesEnum.DOUBLE -> DoubleFrame(value.toDouble())
            else -> throw UnsupportedOperationException("Unknown type for frame ${value::class}")
        } as Frame<S>
    } else {
        throw UnsupportedOperationException("Unknown type for frame ${value::class}")
    }
}


fun <S: Comparable<S>> HistogramConfiguration<S>.generateChunks(histogramBuilder: HistogramBuilder<S>): Set<Chunk<S>> {
    val chunks = TreeSet<Chunk<S>>()
    val chunksCount = this.calcChunksCount()
    val borderLenght = this.getBorderLength()
    val chunkBorderLenght = borderLenght / chunksCount
    var border: Border<Frame<S>>
    (0..chunksCount).forEach {
        border = Border(
            this.frameFactory(it * chunkBorderLenght),
            this.frameFactory(it * chunkBorderLenght + chunkBorderLenght - 1)
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
            (this.histogramBorder as Border<Frame<Int>>).borderIntLenght()
        }

        LONG -> {
            (this.histogramBorder as Border<Frame<Long>>).borderLongLenght()
        }

        FLOAT -> {
            (this.histogramBorder as Border<Frame<Float>>).borderFloatLenght()
        }

        DOUBLE -> {
            (this.histogramBorder as Border<Frame<Double>>).borderDoubleLenght()
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