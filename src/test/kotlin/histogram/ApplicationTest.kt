package histogram

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.gurikin.histogram.internal.Border
import org.gurikin.histogram.internal.Chunk
import org.gurikin.histogram.internal.ChunkId
import org.gurikin.histogram.internal.DefaultChunkAggregator
import org.gurikin.histogram.internal.DefaultChunkQueue
import org.gurikin.histogram.internal.DefaultChunkStorage
import org.gurikin.histogram.num_histogram.IntFlowGenerator
import org.gurikin.histogram.num_histogram.IntHistogramBuilder

class ApplicationTest {
    @Test
    fun `test aggregate flow to chunks`() {
        runBlocking {
            val histogramBuilder = IntHistogramBuilder()
            val chunks = TreeSet<Chunk<Int>>()
            val step = 100
            val binsCount = 10
            var border = Border(0, step - 1)
            (1..10).forEach { histogramNum ->
                chunks.add(Chunk(histogramBuilder.initHistogram(border, binsCount), ChunkId()))
                border = Border(histogramNum * step, histogramNum * step + step - 1)
            }
            val chunkStorage = DefaultChunkStorage<Int>(this)
            val chunkQueue = DefaultChunkQueue(this)
            val chunkAggregator = DefaultChunkAggregator(
                chunks = chunks,
                chunkStorage = chunkStorage,
                chunkQueue = chunkQueue,
                scope = this,
                queueSendTimeout = 2000.milliseconds,
            )
            val expectedMessageCnt = 200
            val sourceFlowGenerator = IntFlowGenerator(0..<expectedMessageCnt)
            val sourceFlow = this.async {
                sourceFlowGenerator.flowData()
            }.await()

            chunkAggregator.collectData(sourceFlow)

            this.launch {
                var messageCnt = 1
                while (messageCnt < expectedMessageCnt) {
                    val chunkId = chunkQueue.poll()
                    val chunk = chunkStorage.getChunk(chunkId)
                    assertTrue {
                        chunk.histogram.totalFrameSum == 0 ||
                                BigDecimal.ONE.setScale(2, RoundingMode.HALF_EVEN)
                                    .compareTo(chunk.histogram.bins.sumOf {
                                        it.weight
                                    }.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)) == 0
                    }
                    val binsWeightString = chunk.histogram.bins.map { it.weight }.joinToString(",")
                    println("$messageCnt:${binsWeightString}")
                    chunk.let { messageCnt++ }
                }
                assertEquals(expectedMessageCnt, messageCnt)
                println("Test complete successfully")
                this@runBlocking.coroutineContext.cancelChildren()
            }
        }
    }
}