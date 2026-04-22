package org.gurikin.histogram.internal

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * API для управления хранилищем параметров и состояния чанков
 * API для сохранения/получения данных о чанках
 */
interface ChunkStorage<S : Comparable<S>> {
    suspend fun storeChunk(chunk: Chunk<S>): ChunkId
    suspend fun getChunk(chunkId: ChunkId): Chunk<S>
}

/**
 * Default implementation of [ChunkStorage].
 * Used under the hood a coroutine's [Channel] and MutableMap.
 *
 * @property scope: [CoroutineScope] for launt task for a read channel
 */
class DefaultChunkStorage<S : Comparable<S>>(val scope: CoroutineScope) : ChunkStorage<S> {

    private val chunkChannel = Channel<Chunk<S>>()
    private val chunkMap = mutableMapOf<ChunkId, Chunk<S>>()

    init {
        scope.launch {
            chunkChannel.consumeEach { chunk ->
                chunkMap[chunk.chunkId] = chunk
            }
        }.invokeOnCompletion {
            chunkChannel.close()
        }
    }

    override suspend fun storeChunk(chunk: Chunk<S>): ChunkId {
        chunkChannel.send(chunk)
        return chunk.chunkId
    }

    override suspend fun getChunk(chunkId: ChunkId): Chunk<S> = withTimeout(100.milliseconds) {
        var result: Chunk<S>? = null
        while (result == null) {
            result = chunkMap[chunkId]
            if (result == null) delay(2.milliseconds)
        }
        result
    }
}