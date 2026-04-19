package org.gurikin.histogram.internal

import java.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch

/**
 * API для работы с очередью сообщений о необходимости аккумуляции чанка в общей гистограмме
 * Предпологаются различные реализации в виде персистентных хранилищ, очередей в памяти, key-value хранилищ и пр.
 */
interface ChunkQueue {
    suspend fun add(chunkId: ChunkId)
    suspend fun poll(): ChunkId
}

/**
 * Default implementation of [ChunkQueue].
 * Used under the hood a coroutine's [Channel] and LinkedList.
 *
 * @property scope: [CoroutineScope] for launt task for a read channel
 * @property maxQueueSize: [Int] capacity for channel. Default value = 100
 */
class DefaultChunkQueue(val scope: CoroutineScope, maxQueueSize: Int = 100) : ChunkQueue {

    private val chunkIdChannel = Channel<ChunkId>(maxQueueSize)
    private val chunkQueue = LinkedList<ChunkId>()

    init {
        scope.launch {
            chunkIdChannel.consumeEach { chunkId ->
                chunkQueue.add(chunkId)
            }
        }.invokeOnCompletion {
            chunkIdChannel.close()
        }
    }

    override suspend fun add(chunkId: ChunkId) = chunkIdChannel.trySend(chunkId).getOrThrow()

    override suspend fun poll(): ChunkId = chunkQueue.poll()
}
