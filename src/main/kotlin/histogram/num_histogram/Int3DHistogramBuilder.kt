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
        border: Border<Int>,
        binsCount: Int
    ): Histogram<Int> {
        return createHistogram(border, binsCount)
    }

    private fun createHistogram(
        border: Border<Int>,
        binsCount: Int
    ): Histogram<Int> {
        val remOfDiv = reminderOfDivision(border, binsCount)
        var xRemOfDiv = remOfDiv
        var yRemOfDiv = remOfDiv
        var zRemOfDiv = remOfDiv
        val interval = abs(border.to.value() - remOfDiv - border.from.value() + 1)
        val step = interval / binsCount
        val bins: MutableList<Bin<Int>> = mutableListOf()
        var currBin = Bin(
            Border(border.from, border.from + step - 1),
            Border(border.from, border.from + step - 1),
            Border(border.from, border.from + step - 1)
        )
        for (x in (0..<binsCount)) {
            val xBinFrom = currBin.xBorder.from
            val xCorrectionToBin = xRemOfDiv.addCorrectionToBin()
            val xBinTo = currBin.xBorder.to + xCorrectionToBin
            xRemOfDiv -= xCorrectionToBin
            val xBorder = Border(xBinFrom, xBinTo)
            for (y in (0..<binsCount)) {
                val yBinFrom = currBin.yBorder!!.from
                val yCorrectionToBin = yRemOfDiv.addCorrectionToBin()
                val yBinTo = currBin.yBorder.to + yCorrectionToBin
                yRemOfDiv -= yCorrectionToBin
                val yBorder = Border(yBinFrom, yBinTo)
                for (z in (0..<binsCount)) {
                    val zBinFrom = currBin.zBorder!!.from
                    val zCorrectionToBin = zRemOfDiv.addCorrectionToBin()
                    val zBinTo = currBin.zBorder.to + yCorrectionToBin
                    zRemOfDiv -= zCorrectionToBin
                    val zBorder = Border(zBinFrom, zBinTo)
                    val bin = Bin(xBorder, yBorder, zBorder)
                    bins.add(bin)
                    currBin = Bin(
                        Border(bin.xBorder.to + 1, bin.xBorder.to + step),
                        Border(bin.yBorder!!.to + 1, bin.yBorder.to + step),
                        Border(bin.zBorder!!.to + 1, bin.zBorder.to + step)
                    )
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
        return createHistogram(border, binsCount)
    }
}