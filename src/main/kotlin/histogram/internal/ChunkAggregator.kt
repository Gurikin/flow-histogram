package org.gurikin.histogram.internal

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * API для агрегации элементов, поступающих из SourceFlowGenerator в чанки (гистограммы с константными границами)
 * Точки взаимодействия см. [sequence](docs/sequence.puml):
 *  - `Store`
 *  - `Queue`
 *  - `SourceFlow`
 *  - `HistogramBuilder`
 * В реализации по умолчанию по мере поступления данных выполняется следующий алгоритм:
 *  - Определяем chunk для записи "точки" (минимальной единицы данных)
 *  - Добавляем "точку" к чанку (минимальной гистограмме статического размера)
 *  - Подгружаем параметры чанка (гистограммы)
 *  - Добавляем "точку" к нужному бину (определяется математически)
 */
internal interface ChunkAggregator<S : Comparable<S>> {
    suspend fun collectData()
    suspend fun storeChunk(chunk: Chunk<S>): ChunkId
    suspend fun sendChunkId(chunkId: ChunkId)
}

data class Chunk<S : Comparable<S>>(
    val histogram: Histogram<S>,
    val chunkId: ChunkId = ChunkId()
) : Comparable<Chunk<S>> {

    override fun compareTo(other: Chunk<S>): Int {
        val result = mutableListOf<Int>()
        if (this.histogram.bins[0].zBorder != null) {
            result.add(this.histogram.bins[0].zBorder!!.from.compareTo(other.histogram.bins[0].zBorder!!.from))
        }
        if (this.histogram.bins[0].yBorder != null) {
            result.add(this.histogram.bins[0].yBorder!!.from.compareTo(other.histogram.bins[0].yBorder!!.from))
        }
        result.add(this.histogram.bins[0].xBorder.from.compareTo(other.histogram.bins[0].xBorder.from))
        return when {
            result.contains(-1) -> -1
            result.all { it == 0 } -> 0
            else -> 1
        }
    }

    fun copy(): Chunk<S> = Chunk(
        histogram = this.histogram.copy(),
        chunkId = this.chunkId.copy(),
    )

    fun frameInChunk(frame: Frame<S>): Boolean {
        val firstXCompare = this.histogram.bins.first().xBorder.from.compareTo(frame.value())
        val firstYCompare = this.histogram.bins.first().yBorder?.from?.compareTo(frame.value())
        val firstZCompare = this.histogram.bins.first().zBorder?.from?.compareTo(frame.value())

        val firstCompare = (firstZCompare == null || firstZCompare == -1 || firstZCompare == 0) &&
                (firstYCompare == null || firstYCompare == -1 || firstYCompare == 0) &&
                (firstXCompare == -1 || firstXCompare == 0)

        val lastXCompare = this.histogram.bins.last().xBorder.from.compareTo(frame.value())
        val lastYCompare = this.histogram.bins.last().yBorder?.from?.compareTo(frame.value())
        val lastZCompare = this.histogram.bins.last().zBorder?.from?.compareTo(frame.value())

        val lastCompare = (lastZCompare == null || lastZCompare == 1 || lastZCompare == 0) &&
                (lastYCompare == null || lastYCompare == 1 || lastYCompare == 0) &&
                (lastXCompare == 1 || lastXCompare == 0)

        return firstCompare && lastCompare
    }
}

    data class ChunkId(val id: UUID = UUID.randomUUID())


    /**
     * Default implementatio of [ChunkAggregator].
     * Try to use it whith all types you need.
     */
    class DefaultChunkAggregator<S : Comparable<S>>(
        private val framesFlow: Flow<Frame<S>?>,
        private val chunks: SortedSet<Chunk<S>>,
        private val chunkStorage: ChunkStorage<S>,
        private val chunkQueue: ChunkQueue,
        private val scope: CoroutineScope,
        private val framesBufSize: Int = 200,
        private val queueSendTimeout: Duration = 1000.milliseconds,
    ) : ChunkAggregator<S> {

        @OptIn(ExperimentalCoroutinesApi::class)
        override suspend fun collectData() {
            framesFlow
                .filter { frame -> frame != null }
                .onEach { frame ->
                    for (chunk in chunks) {
                        if (chunk.frameInChunk(frame!!)) {
                            chunk.histogram.add(frame)
                            println("[ChunkAggregator] Frame: ${frame.value()}")
                            break
                        }
                    }
                }.launchIn(scope)
            scope.launch {
                while (isActive) {
                    delay(queueSendTimeout)
                    sendChunks()
                }
            }
        }

        private suspend fun sendChunks() {
            val filteredChunks = chunks.filter { it.histogram.getFrameSum() > 0 }
            val chunkTotalFrames = chunks.sumOf { it.histogram.getFrameSum() }
            println("[ChunkAggregator] ChunksTotalFrames: $chunkTotalFrames")

            filteredChunks.forEach { chunk ->
                val chunkCopy = chunk.copy()
                sendChunkId(storeChunk(chunkCopy))
                chunk.histogram.clear()
            }
        }

        override suspend fun storeChunk(chunk: Chunk<S>): ChunkId = chunkStorage.storeChunk(chunk)

        override suspend fun sendChunkId(chunkId: ChunkId) = chunkQueue.add(chunkId)
    }