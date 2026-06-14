package org.gurikin.histogram.num_histogram

import org.gurikin.histogram.internal.*
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.abs

/**
 * Implementation of [HistogramBuilder] for [Int] type
 * As is type is possible very different its need
 * to create Histogram instance for each type particular.
 *
 * Only 1D histogram are possible now
 */
@OptIn(ExperimentalAtomicApi::class)
class Int3DHistogramBuilder : HistogramBuilder<Int> {

    override fun initHistogram(
        xBorder: Border<Int>,
        binsCount: Int,
        yBorder: Border<Int>?,
        zBorder: Border<Int>?,
    ): Histogram<Int> {
        return createHistogram(xBorder, binsCount, yBorder, zBorder)
    }

    private fun createHistogram(
        xBorder: Border<Int>,
        binsCount: Int,
        yBorder: Border<Int>?,
        zBorder: Border<Int>?,
    ): Histogram<Int> {
        val bins = mutableListOf<Bin<Int>>()
        // Вычисляем шаг внутри гистограммы (размер одного бина)
        val xSize = (xBorder.to.value() - xBorder.from.value() + 1) / binsCount
        val ySize = (yBorder!!.to.value() - yBorder.from.value() + 1) / binsCount
        val zSize = (zBorder!!.to.value() - zBorder.from.value() + 1) / binsCount

        for (xi in 0 until binsCount) {
            val xFrom = xBorder.from.value() + xi * xSize
            val xTo = if (xi == binsCount - 1) xBorder.to.value() else xFrom + xSize - 1
            val xBinBorder = Border(IntFrame(xFrom), IntFrame(xTo))

            for (yi in 0 until binsCount) {
                val yFrom = yBorder.from.value() + yi * ySize
                val yTo = if (yi == binsCount - 1) yBorder.to.value() else yFrom + ySize - 1
                val yBinBorder = Border(IntFrame(yFrom), IntFrame(yTo))

                for (zi in 0 until binsCount) {
                    val zFrom = zBorder.from.value() + zi * zSize
                    val zTo = if (zi == binsCount - 1) zBorder.to.value() else zFrom + zSize - 1
                    val zBinBorder = Border(IntFrame(zFrom), IntFrame(zTo))

                    bins.add(Bin(xBinBorder, yBinBorder, zBinBorder))
                }
            }
        }
        return Histogram(bins = bins, totalFrameSum = 0)
    }

    private fun reminderOfDivision(border: Border<Int>, binsCount: Int): Int =
        border.let { (it.to.value() - it.from.value() + 1) % binsCount }

    private fun Int.addCorrectionToBin(): Int = if (this == 0) 0 else 1

    override fun initHistogram(histogramConfiguration: HistogramConfiguration<Int>): Histogram<Int> {
        val border = histogramConfiguration.histogramBorder!!
        val binsCount = histogramConfiguration.calcBinsCount()
        return createHistogram(border, binsCount, null, null)
    }
}