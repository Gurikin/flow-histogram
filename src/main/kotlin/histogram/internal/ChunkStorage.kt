package org.gurikin.histogram.internal

/**
 * API для управления хранилищем параметров и состояния чанков
 * API для сохранения/получения данных о чанках
 */
interface ChunkStorage {
    fun storeChunk(chunk: Chunk<*>): ChunkId
    fun getChunk(chunkId: ChunkId): Chunk<*>
}