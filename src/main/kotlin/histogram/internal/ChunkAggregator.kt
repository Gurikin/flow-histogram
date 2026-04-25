package org.gurikin.histogram.internal

import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.chunked
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
    suspend fun collectData(framesFlow: Flow<Frame<S>>)
    suspend fun storeChunk(chunk: Chunk<S>): ChunkId
    suspend fun sendChunkId(chunkId: ChunkId)
}

data class Chunk<S : Comparable<S>>(
    val histogram: Histogram<S>,
    val chunkId: ChunkId = ChunkId()
) : Comparable<Chunk<S>> {
    override fun compareTo(other: Chunk<S>): Int =
        this.histogram.bins[0].border.from.compareTo(other.histogram.bins[0].border.from)
}

data class ChunkId(val id: UUID = UUID.randomUUID())


/**
 * Default implementatio of [ChunkAggregator].
 * Try to use it whith all types you need.
 */
class DefaultChunkAggregator<S : Comparable<S>>(
    val chunks: SortedSet<Chunk<S>>,
    val chunkStorage: ChunkStorage<S>,
    val chunkQueue: ChunkQueue,
    val scope: CoroutineScope,
    val framesBufSize: Int = 200,
    val queueSendTimeout: Duration = 1000.milliseconds,
) : ChunkAggregator<S> {

    private val framesBuf = ArrayList<Frame<S>>(1000)

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun collectData(framesFlow: Flow<Frame<S>>) {
        scope.launch {
            framesFlow.chunked(framesBufSize).collect {
                for (chunk in chunks) {
                    for (frame in it) {
                        if (chunk.histogram.bins.first().border.from <= frame.value && chunk.histogram.bins.last().border.to > frame.value) {
                            chunk!!.histogram.add(frame)
                            break
                        }
                    }

                }
            }
        }
        scope.launch {
            while (this.isActive) {
                delay(queueSendTimeout)
                for (chunk in chunks) {
                    this@DefaultChunkAggregator.sendChunkId(chunkStorage.storeChunk(chunk))
                }
            }

        }
    }

    override suspend fun storeChunk(chunk: Chunk<S>): ChunkId = chunkStorage.storeChunk(chunk)

    override suspend fun sendChunkId(chunkId: ChunkId) = chunkQueue.add(chunkId)
}