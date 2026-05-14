package histogram.num_histogram

import kotlinx.coroutines.test.runTest
import org.gurikin.histogram.internal.*
import org.gurikin.histogram.num_histogram.IntHistogramBuilder
import org.junit.jupiter.api.DisplayName
import kotlin.test.Test
import kotlin.test.assertEquals

class IntHistogramBuilderTest {
    @Test
    @DisplayName("Test init histogram with manually defined bins count")
    fun initManuallyHistogram() {
        val border = Border(
            from = 0,
            to = 152
        )
        val binsCount = 14
        val histogram = IntHistogramBuilder().initHistogram(border, binsCount)
        assertEquals(binsCount, histogram.bins.size)
        for (i in (0..(border.to % binsCount))) {
            assertEquals(11, histogram.bins[i].border.to - histogram.bins[i].border.from + 1)
        }
        for (i in ((border.to % binsCount) + 1..<binsCount)) {
            assertEquals(10, histogram.bins[i].border.to - histogram.bins[i].border.from + 1)
        }
        assertEquals(153, histogram.bins.sumOf { it.border.borderLength() })
    }

    @Test
    fun simpleAdd() {
        runTest {
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
            assertEquals(100, histogram.getFrameSum())
            assertEquals(50.0 / 100.toDouble(), histogram.bins[4].weight)
            assertEquals(40.0 / 100.toDouble(), histogram.bins[8].weight)
            assertEquals(10.0 / 100.toDouble(), histogram.bins[0].weight)
        }
    }

    @Test
    fun addWithReminderOfDivision() {
        runTest {
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
            assertEquals(100, histogram.getFrameSum())
            assertEquals(50.0 / 100.toDouble(), histogram.bins[0].weight)
            assertEquals(40.0 / 100.toDouble(), histogram.bins[1].weight)
            assertEquals(10.0 / 100.toDouble(), histogram.bins[2].weight)
        }
    }

    @Test
    @DisplayName("Test init histogram by configuration with calculation of bins count")
    fun initHistogramByConfiguration() {
        val border = Border(
            from = 0,
            to = 152
        )
        val minStep = 1
        val type = HistogramSourceTypesEnum.INT
        val configuration = HistogramConfiguration(histogramBorder = border, sourceType = type, minStep = minStep)
        val histogram = IntHistogramBuilder().initHistogram(configuration)
        val binsCount = 8 //calculated manually by formula from [org.gurikin.histogram.num_histogram.IntHistogramBuilder.calcBinsCount]
        assertEquals(binsCount, histogram.bins.size)
        for (i in (0..(border.to % binsCount))) {
            assertEquals(20, histogram.bins[i].border.to - histogram.bins[i].border.from + 1)
        }
        for (i in ((border.to % binsCount) + 1..<binsCount)) {
            assertEquals(19, histogram.bins[i].border.to - histogram.bins[i].border.from + 1)
        }
        assertEquals(153, histogram.bins.sumOf { it.border.borderLength() })
    }
}