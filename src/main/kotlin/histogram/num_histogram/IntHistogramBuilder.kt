package org.gurikin.histogram.num_histogram

import org.gurikin.histogram.internal.*
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.math.abs
import kotlin.math.log2

/**
 * Implementaion of [HistogramBuilder] for [Int] type
 * As is type is possible very different its need
 * to create Histogram instance for each type particullary.
 *
 * Only 1D histogram are possible now
 */
@OptIn(ExperimentalAtomicApi::class)
class IntHistogramBuilder : HistogramBuilder<Int> {
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
        var remOfDiv = reminderOfDivision(border, binsCount)
        val interval = abs(border.to - remOfDiv - border.from + 1)
        val step = interval / binsCount
        val bins: MutableList<Bin<Int>> = mutableListOf()
        var currBin = Bin(Border(border.from, border.from + step - 1))
        for (i in (0..<binsCount)) {
            val binFrom = currBin.border.from
            val correctionToBin = remOfDiv.addCorrectionToBin()
            val binTo = currBin.border.to + correctionToBin
            remOfDiv -= correctionToBin
            val border = Border(binFrom, binTo)
            val bin = Bin(border)
            bins.add(bin)
            currBin = Bin(Border(bin.border.to + 1, bin.border.to + step))
        }
        return Histogram(bins = bins, totalFrameSum = 0)
    }

    private fun reminderOfDivision(border: Border<Int>, binsCount: Int): Int =
        border.let { (it.to - it.from + 1) % binsCount }

    private fun Int.addCorrectionToBin(): Int = if (this == 0) 0 else 1

    override fun initHistogram(histogramConfiguration: HistogramConfiguration<Int>): Histogram<Int> {
        val border = histogramConfiguration.histogramBorder
        val binsCount = calcBinsCount(histogramConfiguration)
        return createHistogram(border, binsCount)
    }

    /**
     * Base formula for calculating histogram bins count $n = 1 + log_2(N)$
     * N - number of all possible elements in histogram's border:
     *  `N = (histogramConfiguration.histogramBorder.to - histogramConfiguration.histogramBorder.from) / histogramConfiguration.minStep`
     *
     * @param histogramConfiguration
     * @return
     */
    private fun calcBinsCount(histogramConfiguration: HistogramConfiguration<Int>): Int {
        val stepCount = (histogramConfiguration.histogramBorder.to - histogramConfiguration.histogramBorder.from) / histogramConfiguration.minStep
        return (1 + log2(stepCount.toDouble())).toInt()
    }

}