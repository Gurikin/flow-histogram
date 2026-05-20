package util

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import org.gurikin.histogram.SourceFlowGenerator
import org.gurikin.histogram.internal.Frame
import org.gurikin.histogram.internal.IntFrame

/**
 * Test flow generator with predictable weights of each bin.
 * Bin weights:
 *  1: 5%
 *  2: 7%
 *  3: 56%
 *  4: 22%
 *  5: 8%
 *  6: 2%
 *
 *  Generate 1000 messages with predictable separation
 */
class TestPredictableFlowGenerator(private val scope: CoroutineScope) : SourceFlowGenerator<Int> {
    override fun flowData(): Flow<Frame<Int>?> = channelFlow {
        val messagesCount: Int = 100
        val binPercentage = listOf(0.05, 0.07, 0.56, 0.22, 0.08, 0.02)
        var from = 0
        for (percentIndex in 0..<binPercentage.size) {
            val to = (binPercentage[percentIndex] * messagesCount).toInt() + from
            val frameList = getListToEmit(from = from, to = to, percentIndex)
            frameList.forEach { this.send(it) }
            delay(10.milliseconds)
            from = to
        }
    }

    private fun getListToEmit(from: Int, to: Int, binIndex: Int): List<Frame<Int>> {
        val result = mutableListOf<Frame<Int>>()
        repeat(to - from) {
            when (binIndex) {
                0 -> result.add(IntFrame(0))
                1 -> result.add(IntFrame(8))
                2 -> result.add(IntFrame(30))
                3 -> result.add(IntFrame(52))
                4 -> result.add(IntFrame(70))
                5 -> result.add(IntFrame(98))
            }

        }
        return result
    }
}