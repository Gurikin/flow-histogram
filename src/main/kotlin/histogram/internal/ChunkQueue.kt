package org.gurikin.histogram.internal

/**
 * API для работы с очередью сообщений о необходимости аккумуляции чанка в общей гистограмме
 * Предпологаются различные реализации в виде персистентных хранилищ, очередей в памяти, key-value хранилищ и пр.
 */
interface ChunkQueue {
    fun add(chunk: ChunkId)
    fun poll(): ChunkId
}