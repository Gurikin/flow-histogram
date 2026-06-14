package org.gurikin.histogram.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.gurikin.histogram.num_histogram.IntHistogramBuilder
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultChunkStorageTest {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val storage = DefaultChunkStorage<Int>(scope)
    private lateinit var chunk: Chunk<Int>

    @BeforeEach
    fun setUp() {
        val builder = IntHistogramBuilder()
        val border: Border<Int> = Border(IntFrame(0), IntFrame(100))
        val histogram = builder.initHistogram(border, 10)
        chunk = Chunk(histogram, ChunkId())
    }

    @Test
    fun storeChunk() = runBlocking {
        val actualChunkId = storage.storeChunk(chunk)
        assertEquals(chunk.chunkId, actualChunkId)
    }

    @Test
    fun getChunk() {
        runBlocking {
            val actualChunkId = scope.async {
                storage.storeChunk(chunk)
            }.await()
            assertEquals(chunk.chunkId, actualChunkId)
            val actualChunk = scope.async {
                storage.getChunk(chunk.chunkId)
            }.await()
            assertEquals(chunk, actualChunk)
        }
    }
}