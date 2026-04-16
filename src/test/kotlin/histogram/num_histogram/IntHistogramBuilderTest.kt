package histogram.num_histogram

import kotlin.test.Test
import kotlin.test.assertEquals
import org.gurikin.histogram.internal.Border
import org.gurikin.histogram.internal.Frame
import org.gurikin.histogram.internal.add
import org.gurikin.histogram.num_histogram.IntHistogramBuilder

class IntHistogramBuilderTest {
    @Test
    fun initHistogram() {
        val border = Border(
            from = 0,
            to = 152
        )
        val binsCount = 14
        val histogram = IntHistogramBuilder().initHistogram(border, binsCount)
        assertEquals(binsCount, histogram.bins.size)
        for (i in (0..<(border.to % binsCount))) {
            assertEquals(11, histogram.bins[i].border.to - histogram.bins[i].border.from)
        }
        for (i in ((border.to % binsCount) + 1..<binsCount)) {
            assertEquals(10, histogram.bins[i].border.to - histogram.bins[i].border.from)
        }
        assertEquals(152, histogram.bins.sumOf { it.border.to - it.border.from })
    }

    @Test
    fun simpleAdd() {
        val border = Border(
            from = 0,
            to = 100
        )
        val binsCount = 10
        val histogramBuilder = IntHistogramBuilder()
        val histogram = histogramBuilder.initHistogram(border, binsCount)
        for (i in 0..<100) {
            when (i) {
                in (0..49) -> histogram.add(Frame(49))
                in (50..89) -> histogram.add(Frame(89))
                else -> histogram.add(Frame(9))
            }
        }
        assertEquals(100, histogram.totalFrameSum)
        assertEquals(50.0 / 100.toDouble(), histogram.bins[4].weight)
        assertEquals(40.0 / 100.toDouble(), histogram.bins[8].weight)
        assertEquals(10.0 / 100.toDouble(), histogram.bins[0].weight)
    }

    @Test
    fun addWithReminderOfDivision() {
        val border = Border(
            from = 0,
            to = 102
        )
        val binsCount = 10
        val histogramBuilder = IntHistogramBuilder()
        val histogram = histogramBuilder.initHistogram(border, binsCount)
        for (i in 0..<100) {
            when (i) {
                in (0..49) -> histogram.add(Frame(10))
                in (50..89) -> histogram.add(Frame(21))
                else -> histogram.add(Frame(22))
            }
        }
        assertEquals(100, histogram.totalFrameSum)
        assertEquals(50.0 / 100.toDouble(), histogram.bins[0].weight)
        assertEquals(40.0 / 100.toDouble(), histogram.bins[1].weight)
        assertEquals(10.0 / 100.toDouble(), histogram.bins[2].weight)
    }
}