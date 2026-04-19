package org.gurikin.histogram.internal

import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * API для управления хранилищем параметров и состояния чанков
 * API для сохранения/получения данных о чанках
 */
interface ChunkStorage {
    suspend fun storeChunk(chunk: Chunk<*>): ChunkId
    suspend fun getChunk(chunkId: ChunkId): Chunk<*>
}

/**
 * Default implementation of [ChunkStorage].
 * Used under the hood a coroutine's [Channel] and MutableMap.
 *
 * @property scope: [CoroutineScope] for launt task for a read channel
 */
class DefaultChunkStorage(val scope: CoroutineScope) : ChunkStorage {

    private val chunkChannel = Channel<Chunk<*>>()
    private val chunkMap = mutableMapOf<ChunkId, Chunk<*>>()

    init {
        scope.launch {
            chunkChannel.consumeEach { chunk ->
                chunkMap[chunk.chunkId] = chunk
            }
        }.invokeOnCompletion {
            chunkChannel.close()
        }
    }

    override suspend fun storeChunk(chunk: Chunk<*>): ChunkId {
        chunkChannel.trySend(chunk).getOrThrow()
        return chunk.chunkId
    }

    override suspend fun getChunk(chunkId: ChunkId): Chunk<*> = scope.async {
        withTimeout(100.milliseconds) {
            var result: Chunk<*>? = null
            while (result == null) {
                result = chunkMap[chunkId]
                delay(2.milliseconds)
            }
            result
        }
    }.await()
}