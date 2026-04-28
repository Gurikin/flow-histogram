package util

import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import org.gurikin.histogram.SourceFlowGenerator
import org.gurikin.histogram.internal.Frame

/**
 * Test flow generator with predictable weights of each bin.
 * Bin weights:
 *  1: 5%
 *  2: 7%
 *  3: 48%
 *  4: 30%
 *  5: 8%
 *  6: 2%
 *
 *  Generate 1000 messages with predictable separation
 */
class TestPredictableFlowGenerator(private val scope: CoroutineScope) : SourceFlowGenerator<Int> {
    override fun flowData(): Flow<Frame<Int>?> = channelFlow {
        val messagesCount: Int = 100
        val binPercentage = listOf(0.05, 0.07, 0.48, 0.30, 0.08, 0.02)
        var from = 0
        for (percentIndex in 0..<binPercentage.size) {
            val to = (binPercentage[percentIndex] * messagesCount).toInt() + from
            val frameList = getListToEmit(from = from, to = to)
            frameList.forEach { this.send(it) }
            delay(10.milliseconds)
            from = to
        }
    }

    private fun getListToEmit(from: Int, to: Int): List<Frame<Int>> {
        val result = mutableListOf<Frame<Int>>()
        repeat(to - from) {
            result.add(Frame(Random.nextInt(from, to)))
        }
        return result
    }
}