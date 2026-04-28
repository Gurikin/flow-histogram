package histogram

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.gurikin.histogram.DefaultHistogrammator
import org.gurikin.histogram.internal.Border
import org.gurikin.histogram.internal.Chunk
import org.gurikin.histogram.internal.ChunkAggregator
import org.gurikin.histogram.internal.ChunkId
import org.gurikin.histogram.internal.DefaultChunkAggregator
import org.gurikin.histogram.internal.DefaultChunkQueue
import org.gurikin.histogram.internal.DefaultChunkStorage
import org.gurikin.histogram.internal.Frame
import org.gurikin.histogram.num_histogram.IntHistogramBuilder
import util.TestPredictableFlowGenerator

class ApplicationTest {
//    @Test
//    fun `test ChunkAggregator collectData 200 messages`() {
//        runBlocking {
//            val histogramBuilder = IntHistogramBuilder()
//            val chunks = TreeSet<Chunk<Int>>()
//            val step = 100
//            val binsCount = 10
//            var border = Border(0, step - 1)
//            (1..10).forEach { histogramNum ->
//                chunks.add(Chunk(histogramBuilder.initHistogram(border, binsCount), ChunkId()))
//                border = Border(histogramNum * step, histogramNum * step + step - 1)
//            }
//            val chunkStorage = DefaultChunkStorage<Int>(this)
//            val chunkQueue = DefaultChunkQueue(this)
//            val expectedMessageCnt = 200
//            val sourceFlowGenerator = IntFlowGenerator(0..<expectedMessageCnt, 10)
//            val sourceFlow = this.async {
//                sourceFlowGenerator.flowData()
//            }.await()
//            val chunkAggregator = DefaultChunkAggregator(
//                framesFlow = sourceFlow,
//                chunks = chunks,
//                chunkStorage = chunkStorage,
//                chunkQueue = chunkQueue,
//                scope = this,
//                queueSendTimeout = 20.milliseconds,
//            )
//
//            this.launch {
//                while (this.isActive) {
//                    chunkAggregator.collectData()
//                    delay(20.milliseconds)
//                }
//            }
//
//            this.launch {
//                var messageCnt = 1
//                while (messageCnt < expectedMessageCnt) {
//                    val chunkId = chunkQueue.poll()
//                    val chunk = chunkStorage.getChunk(chunkId)
//                    assertTrue {
//                        chunk.histogram.totalFrameSum == 0 ||
//                                BigDecimal.ONE.setScale(2, RoundingMode.HALF_EVEN)
//                                    .compareTo(chunk.histogram.bins.sumOf {
//                                        it.weight
//                                    }.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)) == 0
//                    }
//                    val binsWeightString = chunk.histogram.bins.map { it.weight }.joinToString(",")
//                    println("$messageCnt:${binsWeightString}")
//                    chunk.let { messageCnt++ }
//                }
//                assertEquals(expectedMessageCnt, messageCnt)
//                println("Test complete successfully")
//                this@runBlocking.coroutineContext.cancelChildren()
//            }
//        }
//    }
//
//    @Test
//    fun `test histogrammator accumulate 5000 messages`() {
//        runBlocking {
//            val histogramBuilder = IntHistogramBuilder()
//            val chunks = TreeSet<Chunk<Int>>()
//            val step = 100
//            val binsCount = 10
//            var border = Border(0, step - 1)
//            (0..9).forEach { histogramNum ->
//                val chunk = Chunk(histogramBuilder.initHistogram(border, binsCount), ChunkId())
//                chunks.add(chunk)
//                border = Border(chunk.histogram.bins.last().border.to + 1, chunk.histogram.bins.last().border.to + step)
//            }
//            val chunkStorage = DefaultChunkStorage<Int>(this)
//            val chunkQueue = DefaultChunkQueue(this)
//            val expectedMessageCnt = 1000
//            val sourceFlowGenerator = IntFlowGenerator(0..<expectedMessageCnt, expectedMessageCnt)
//            val sourceFlow = this.async {
//                sourceFlowGenerator.flowData()
//            }.await()
//            val chunkAggregator = DefaultChunkAggregator(
//                framesFlow = sourceFlow,
//                chunks = chunks,
//                chunkStorage = chunkStorage,
//                chunkQueue = chunkQueue,
//                scope = this,
//                queueSendTimeout = 100.milliseconds,
//            )
//
//            chunkAggregator.collectData()
//
//
//            this.launch {
//                val globalBorder = Border(0, chunks.last().histogram.bins.last().border.to)
//                val histogram = histogramBuilder.initHistogram(globalBorder, 10)
//                val histogrammator = DefaultHistogrammator(
//                    histogram = histogram,
//                    chunkQueue = chunkQueue,
//                    chunkStorage = chunkStorage,
//                    scope = this
//                )
//                val accumulateJob = launch { histogrammator.accumulate() }
//                var totalWeight = 0.0
//                while (histogrammator.histogram.totalFrameSum < 5000) {
//                    println("Accumulate general histogram...")
//                    println("Total message count = ${histogrammator.histogram.totalFrameSum}")
//                    delay(1000.milliseconds)
//                    totalWeight = histogrammator.getTotalWeith()
//                }
//                accumulateJob.cancel()
//                println("Test complete successfully")
//                val binsString =
//                    histogrammator.histogram.bins.joinToString(",") { "From: ${it.border.from} To: ${it.border.to} Weight: ${it.weight} FrameSum: ${it.frameSum}" }
//                println("Histogram(totalFrameSum=${histogrammator.histogram.totalFrameSum}, bins=$binsString)")
//                val binsFrameSum = histogrammator.histogram.bins.sumOf { it.frameSum }
//                println("BinsFrameSum=$binsFrameSum")
//                assertEquals(
//                    BigDecimal.ONE.setScale(
//                        2,
//                        RoundingMode.HALF_EVEN
//                    ),
//                    totalWeight.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)
//                )
//                this@runBlocking.coroutineContext.cancelChildren()
//            }
//        }
//    }

    @Test
    fun `test histogrammator accumulate predictable histogram`() {
        runBlocking {
            val histogramBuilder = IntHistogramBuilder()
            val chunks = TreeSet<Chunk<Int>>()
            val step = 10
            val binsCount = 10
            var border = Border(0, step - 1)
            (0..9).forEach { histogramNum ->
                val chunk = Chunk(histogramBuilder.initHistogram(border, binsCount), ChunkId())
                chunks.add(chunk)
                border = Border(chunk.histogram.bins.last().border.to + 1, chunk.histogram.bins.last().border.to + step)
            }
            val chunkStorage = DefaultChunkStorage<Int>(this)
            val chunkQueue = DefaultChunkQueue(this)
            val sourceFlowGenerator = TestPredictableFlowGenerator(this)
//            printFlow(sourceFlowGenerator.flowData())
            val chunkAggregator = DefaultChunkAggregator(
                sourceFlowGenerator.flowData(),
                chunks = chunks,
                chunkStorage = chunkStorage,
                chunkQueue = chunkQueue,
                scope = this,
                queueSendTimeout = 1000.milliseconds,
            )
            aggregateChunk(aggregator = chunkAggregator)

            this.launch {
                val globalBorder = Border(0, chunks.last().histogram.bins.last().border.to)
                val histogram = histogramBuilder.initHistogram(globalBorder, 6)
                val histogrammator = DefaultHistogrammator(
                    histogram = histogram,
                    chunkQueue = chunkQueue,
                    chunkStorage = chunkStorage,
                    scope = this
                )
                val accumulateJob = launch { histogrammator.accumulate() }
                var totalWeight = 0.0
                val expectedMessageCnt = 100
                while (histogrammator.histogram.getFrameSum() < expectedMessageCnt) {
                    println("Accumulate general histogram...")
                    println("Total message count = ${histogrammator.histogram.getFrameSum()}")
                    delay(1000.milliseconds)
                    totalWeight = histogrammator.getTotalWeith()
                }
                accumulateJob.cancel()
                println("Test complete successfully")
                val binsString =
                    histogrammator.histogram.bins.joinToString(",") { "From: ${it.border.from} To: ${it.border.to} Weight: ${it.weight} FrameSum: ${it.getFrameSum()}" }
                println("Histogram(totalFrameSum=${histogrammator.histogram.getFrameSum()}, bins=$binsString)")
                val binsFrameSum = histogrammator.histogram.bins.sumOf { it.getFrameSum() }
                println("BinsFrameSum=$binsFrameSum")
                assertEquals(
                    BigDecimal.ONE.setScale(
                        2,
                        RoundingMode.HALF_EVEN
                    ),
                    totalWeight.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)
                )
                this@runBlocking.coroutineContext.cancelChildren()
            }
        }
    }
}

private suspend fun printFlow(flow: Flow<Frame<Int>?>) {
    var cnt = 0
    val sourceFlow = flow.collect {
        println("${cnt++}: ${it?.value}")
    }
}

private suspend fun aggregateChunk(aggregator: ChunkAggregator<Int>) {
    aggregator.collectData()
}