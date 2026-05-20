package org.gurikin.histogram.internal

import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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

    override fun compareTo(other: Chunk<S>): Int =
        this.histogram.bins[0].border.from.compareTo(other.histogram.bins[0].border.from.value)

    fun copy(): Chunk<S> = Chunk(
        histogram = this.histogram.copy(),
        chunkId = this.chunkId.copy(),
    )
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
        framesFlow.onEach { frame ->
            for (chunk in chunks) {
                if (chunk.histogram.bins.first().border.from <= frame!!.value
                    && chunk.histogram.bins.last().border.to >= frame.value
                ) {
                    chunk.histogram.add(frame)
//                    println("[ChunkAggregator] Frame: ${frame.value}")
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