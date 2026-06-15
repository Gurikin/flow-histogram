package org.gurikin.histogram

import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.gurikin.histogram.internal.Border
import org.gurikin.histogram.internal.Chunk
import org.gurikin.histogram.internal.ChunkAggregator
import org.gurikin.histogram.internal.ChunkId
import org.gurikin.histogram.internal.DefaultChunkAggregator
import org.gurikin.histogram.internal.DefaultChunkQueue
import org.gurikin.histogram.internal.DefaultChunkStorage
import org.gurikin.histogram.internal.IntFrame
import org.gurikin.histogram.internal.Point
import org.gurikin.histogram.internal.frameInBorder
import org.gurikin.histogram.num_histogram.Int3DFlowGenerator
import org.gurikin.histogram.num_histogram.Int3DHistogramBuilder
import org.gurikin.histogram.num_histogram.IntFlowGenerator
import org.gurikin.histogram.num_histogram.IntHistogramBuilder
import org.gurikin.util.TestPredictableFlowGenerator
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import kotlin.test.Test
import kotlin.test.assertTrue
import org.gurikin.histogram.internal.splitIntoParts

class ApplicationTest {
    @Test
    fun `test ChunkAggregator collectData 200 messages`() {
        runBlocking {
            val histogramBuilder = IntHistogramBuilder()
            val chunks = TreeSet<Chunk<Int>>()
            val step = 100
            val binsCount = 10
            var border: Border<Int> =
                Border(IntFrame(0), IntFrame(step - 1))
            (1..10).forEach { histogramNum ->
                chunks.add(Chunk(histogramBuilder.initHistogram(border, binsCount), ChunkId()))
                border = Border(
                    IntFrame(histogramNum * step),
                    IntFrame(histogramNum * step + step - 1)
                )
            }
            println("frame in border: " + chunks.first().histogram.bins.first().frameInBorder(Point(8)))
            val chunkStorage = DefaultChunkStorage<Int>(this)
            val chunkQueue = DefaultChunkQueue(this)
            val expectedMessageCnt = 200
            val sourceFlowGenerator = IntFlowGenerator(0..<expectedMessageCnt, 10, false)
            val sourceFlow = sourceFlowGenerator.flowData()
            val chunkAggregator = DefaultChunkAggregator(
                framesFlow = sourceFlow,
                chunks = chunks,
                chunkStorage = chunkStorage,
                chunkQueue = chunkQueue,
                scope = this,
                queueSendTimeout = 20.milliseconds,
            )

            this.launch {
                while (this.isActive) {
                    chunkAggregator.collectData()
                    delay(20.milliseconds)
                }
            }

            this.launch {
                var messageCnt = 1
                while (messageCnt < expectedMessageCnt) {
                    val chunkId = chunkQueue.poll()
                    val chunk = chunkStorage.getChunk(chunkId)
                    assertTrue {
                        chunk.histogram.getFrameSum() == 0 ||
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
    fun `test histogrammator accumulate 5000 messages`() {
        runBlocking {
            val histogramBuilder = IntHistogramBuilder()
            val chunks = TreeSet<Chunk<Int>>()
            val step = 100
            val binsCount = 10
            var border: Border<Int> =
                Border(IntFrame(0), IntFrame(step - 1))
            (0..9).forEach { histogramNum ->
                val chunk = Chunk(histogramBuilder.initHistogram(border, binsCount), ChunkId())
                chunks.add(chunk)
                border = Border(
                    IntFrame(chunk.histogram.bins.last().xBorder.to.value() + 1),
                    IntFrame(chunk.histogram.bins.last().xBorder.to.value() + step)
                )
            }
            val chunkStorage = DefaultChunkStorage<Int>(this)
            val chunkQueue = DefaultChunkQueue(this)
            val expectedMessageCnt = 1000
            val sourceFlowGenerator = IntFlowGenerator(0..<expectedMessageCnt, expectedMessageCnt)
            val sourceFlow = sourceFlowGenerator.flowData()
            val chunkAggregator = DefaultChunkAggregator(
                framesFlow = sourceFlow,
                chunks = chunks,
                chunkStorage = chunkStorage,
                chunkQueue = chunkQueue,
                scope = this,
                queueSendTimeout = 100.milliseconds,
            )

            chunkAggregator.collectData()


            this.launch {
                val globalBorder: Border<Int> = Border(
                    IntFrame(0),
                    IntFrame(chunks.last().histogram.bins.last().xBorder.to.value())
                )
                val histogram = histogramBuilder.initHistogram(globalBorder, 10)
                val histogrammator = DefaultHistogrammator(
                    histogram = histogram,
                    chunkQueue = chunkQueue,
                    chunkStorage = chunkStorage,
                    scope = this
                )
                val accumulateJob = launch { histogrammator.accumulate() }
                var totalWeight = 0.0
                while (histogrammator.histogram.getFrameSum() < 5000) {
                    println("Accumulate general histogram...")
                    println("Total message count = ${histogrammator.histogram.getFrameSum()}")
                    delay(1000.milliseconds)
                    totalWeight = histogrammator.getTotalWeight()
                }
                accumulateJob.cancel()
                println("Test complete successfully")
                val binsString =
                    histogrammator.histogram.bins.joinToString(",") { "From: ${it.xBorder.from} To: ${it.xBorder.to} Weight: ${it.weight} FrameSum: ${it.getFrameSum()}" }
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

    @Test
    fun `test histogrammator accumulate predictable histogram`() {
        runBlocking {
            val histogramBuilder = IntHistogramBuilder()
            val chunks = TreeSet<Chunk<Int>>()
            val step = 10
            val binsCount = 10
            var border: Border<Int> =
                Border(IntFrame(0), IntFrame(step - 1))
            (0..9).forEach { histogramNum ->
                val chunk = Chunk(histogramBuilder.initHistogram(border, binsCount), ChunkId())
                chunks.add(chunk)
                border = Border(
                    IntFrame(chunk.histogram.bins.last().xBorder.to.value() + 1),
                    IntFrame(chunk.histogram.bins.last().xBorder.to.value() + step)
                )
            }
            val chunkStorage = DefaultChunkStorage<Int>(this)
            val chunkQueue = DefaultChunkQueue(this)
            val sourceFlowGenerator = TestPredictableFlowGenerator(this)
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
                val globalBorder: Border<Int> = Border(
                    IntFrame(0),
                    IntFrame(chunks.last().histogram.bins.last().xBorder.to.value())
                )
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
                    totalWeight = histogrammator.getTotalWeight()
                }
                accumulateJob.cancel()
                println("Test complete successfully")
                println(Json.encodeToString(histogrammator.histogram))
                val binsFrameSum = histogrammator.histogram.bins.sumOf { it.getFrameSum() }
                println("BinsFrameSum=$binsFrameSum")
                assertEquals(
                    BigDecimal.ONE.setScale(
                        2,
                        RoundingMode.HALF_EVEN
                    ),
                    totalWeight.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)
                )
                assertEquals(
                    BigDecimal.valueOf(0.12).setScale(2, RoundingMode.HALF_EVEN),
                    histogram.bins[0].weight.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)
                )
                assertEquals(
                    BigDecimal.valueOf(0.56).setScale(2, RoundingMode.HALF_EVEN),
                    histogram.bins[1].weight.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)
                )
                assertEquals(
                    BigDecimal.valueOf(0.00).setScale(2, RoundingMode.HALF_EVEN),
                    histogram.bins[2].weight.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)
                )
                assertEquals(
                    BigDecimal.valueOf(0.22).setScale(2, RoundingMode.HALF_EVEN),
                    histogram.bins[3].weight.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)
                )
                assertEquals(
                    BigDecimal.valueOf(0.08).setScale(2, RoundingMode.HALF_EVEN),
                    histogram.bins[4].weight.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)
                )
                assertEquals(
                    BigDecimal.valueOf(0.02).setScale(2, RoundingMode.HALF_EVEN),
                    histogram.bins[5].weight.toBigDecimal().setScale(2, RoundingMode.HALF_EVEN)
                )
                //Histogram(totalFrameSum=100, bins=From: 0 To: 16 Weight: 0.12 FrameSum: 12,From: 17 To: 33 Weight: 0.56 FrameSum: 56,From: 34 To: 50 Weight: 0.0 FrameSum: 0,From: 51 To: 67 Weight: 0.22 FrameSum: 22,From: 68 To: 83 Weight: 0.08 FrameSum: 8,From: 84 To: 99 Weight: 0.02 FrameSum: 2)
                this@runBlocking.coroutineContext.cancelChildren()
            }
        }
    }

    @Test
    fun `test 3d histogrammator accumulate 500 messages`() {
        runBlocking {
            val histogramBuilder = Int3DHistogramBuilder()
            val from = -1000
            val to = 999
            val chunksPerAxis = 5               // можно изменить: 2, 4, 5 ...
            val binsPerChunk = 10               // количество бинов внутри каждого чанка

            val xIntervals = splitIntoParts(from, to, chunksPerAxis)
            val yIntervals = splitIntoParts(from, to, chunksPerAxis)
            val zIntervals = splitIntoParts(from, to, chunksPerAxis)

            val chunks = TreeSet<Chunk<Int>>()

            for (xRange in xIntervals) {
                val xBorder = Border(IntFrame(xRange.first), IntFrame(xRange.second))
                for (yRange in yIntervals) {
                    val yBorder = Border(IntFrame(yRange.first), IntFrame(yRange.second))
                    for (zRange in zIntervals) {
                        val zBorder = Border(IntFrame(zRange.first), IntFrame(zRange.second))
                        val histogram = histogramBuilder.initHistogram(xBorder, binsPerChunk, yBorder, zBorder)
                        val chunk = Chunk(histogram, ChunkId())
                        chunks.add(chunk)
                    }
                }
            }
            val chunkStorage = DefaultChunkStorage<Int>(this)
            val chunkQueue = DefaultChunkQueue(this)
            val expectedMessageCnt = 10000
            val sourceFlowGenerator = Int3DFlowGenerator(-1000..<999, expectedMessageCnt)
            val sourceFlow = sourceFlowGenerator.flowData()
            val chunkAggregator = DefaultChunkAggregator(
                framesFlow = sourceFlow,
                chunks = chunks,
                chunkStorage = chunkStorage,
                chunkQueue = chunkQueue,
                scope = this,
                queueSendTimeout = 100.milliseconds,
            )

            chunkAggregator.collectData()


            this.launch {
                val globalBorder: Border<Int> = Border(
                    IntFrame(0),
                    IntFrame(chunks.last().histogram.bins.last().xBorder.to.value())
                )
                val histogram = histogramBuilder.initHistogram(globalBorder, 10, globalBorder, globalBorder)
                val histogrammator = DefaultHistogrammator(
                    histogram = histogram,
                    chunkQueue = chunkQueue,
                    chunkStorage = chunkStorage,
                    scope = this
                )
                val accumulateJob = launch { histogrammator.accumulate() }
                var totalWeight = 0.0
                while (histogrammator.histogram.getFrameSum() < 500) {
                    println("Accumulate general histogram...")
                    println("Total message count = ${histogrammator.histogram.getFrameSum()}")
                    delay(1000.milliseconds)
                    totalWeight = histogrammator.getTotalWeight()
                }
                accumulateJob.cancel()
                println("Test complete successfully")
                val binsString =
                    histogrammator.histogram.bins.joinToString(",") { "From: ${it.xBorder.from} To: ${it.xBorder.to} Weight: ${it.weight} FrameSum: ${it.getFrameSum()}" }
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

    private suspend fun aggregateChunk(aggregator: ChunkAggregator<Int>) {
        aggregator.collectData()
    }
}