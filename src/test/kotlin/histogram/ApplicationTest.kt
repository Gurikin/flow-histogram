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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.gurikin.histogram.DefaultHistogrammator
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
    fun `test ChunkAggregator collectData 200 messages`() {
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
                queueSendTimeout = 200.milliseconds,
            )
            val expectedMessageCnt = 200
            val sourceFlowGenerator = IntFlowGenerator(0..<expectedMessageCnt, expectedMessageCnt)
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

    @Test
    fun `test histogrammator accumulate 200 messages`() {
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
                queueSendTimeout = 20.milliseconds,
            )
            val expectedMessageCnt = 1000
            val sourceFlowGenerator = IntFlowGenerator(0..<expectedMessageCnt, expectedMessageCnt)
            val sourceFlow = this.async {
                sourceFlowGenerator.flowData()
            }.await()

            chunkAggregator.collectData(sourceFlow)

            this.launch {
                val globalBorder = Border(0, border.to)
                val histogram = histogramBuilder.initHistogram(globalBorder, 10)
                val histogrammator = DefaultHistogrammator(
                    histogram = histogram,
                    chunkQueue = chunkQueue,
                    chunkStorage = chunkStorage,
                    scope = this
                )
                histogrammator.accumulate()
                while (histogrammator.histogram.totalFrameSum < 2000) {
                    println("Accumulate general histogram...")
                    println("Total message count = ${histogrammator.histogram.totalFrameSum}")
                    delay(500.milliseconds)
                }
                println("Test complete successfully")
                val binsString =
                    histogrammator.histogram.bins.joinToString(",") { "From: ${it.border.from} To: ${it.border.to} Weight: ${it.weight} FrameSum: ${it.frameSum}" }
                println("Histogram(totalFrameSum=${histogrammator.histogram.totalFrameSum}, bins=$binsString)")
                assertEquals(
                    BigDecimal.ONE.setScale(
                        2,
                        RoundingMode.HALF_EVEN
                    ),
                    histogrammator.histogram.bins.sumOf { it.weight }.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)
                )
                this@runBlocking.coroutineContext.cancelChildren()
            }
        }
    }
}