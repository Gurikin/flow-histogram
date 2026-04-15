package org.gurikin.histogram.num_histogram

import kotlin.math.abs
import org.gurikin.histogram.internal.Bin
import org.gurikin.histogram.internal.Border
import org.gurikin.histogram.internal.Frame
import org.gurikin.histogram.internal.Histogram
import org.gurikin.histogram.internal.HistogramBuilder
import org.gurikin.histogram.internal.addFrame
import org.gurikin.histogram.internal.frameInBorder
import org.gurikin.histogram.internal.setWeight

class IntHistogramBuilder : HistogramBuilder<Int> {
    override fun initHistogram(
        border: Border<Int>,
        binsCount: Int
    ): Histogram<Int> {
        var remOfDiv = reminderOfDivision(border, binsCount)
        val interval = abs(border.to - remOfDiv - border.from)
        val step = interval / binsCount
        val bins: MutableList<Bin<Int>> = mutableListOf()
        var currBin = Bin(Border(border.from, border.from + step))
        for (i in (0..<binsCount)) {
            val binFrom = currBin.border.from
            val correctionToBin = remOfDiv.addCorrectionToBin()
            val binTo = currBin.border.to + correctionToBin
            remOfDiv -= correctionToBin
            val border = Border(binFrom, binTo)
            val bin = Bin(border)
            bins.add(bin)
            currBin = Bin(Border(bin.border.to, bin.border.to + step))
        }
        return Histogram(bins = bins, totalFrameSum = 0)
    }

    private fun reminderOfDivision(border: Border<Int>, binsCount: Int): Int =
        border.let { (it.to - it.from) % binsCount }

    private fun Int.addCorrectionToBin(): Int =
        if (this == 0) {
            0
        } else {
            1
        }

    override fun add(value: Frame<Int>, histogram: Histogram<Int>) {
        for (bin in histogram.bins) {
            if (bin.frameInBorder(value)) {
                histogram.totalFrameSum += 1
                bin.addFrame(value)
            }
        }
        histogram.bins.forEach { bin ->
            bin.setWeight(bin.frameSum.toDouble() / histogram.totalFrameSum)
        }
    }
}