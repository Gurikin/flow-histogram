package org.gurikin.histogram.internal

import gurikin.histogram.internal.Borderimport gurikin.histogram.internal.Chunk
import gurikin.histogram.internal.ChunkIdimport gurikin.histogram.internal.DefaultChunkStorage
import gurikin.histogram.internal.IntFrameimport kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.gurikin.histogram.internal.Border
import org.gurikin.histogram.internal.Chunk
import org.gurikin.histogram.internal.ChunkId
import org.gurikin.histogram.internal.DefaultChunkStorage
import org.gurikin.histogram.internal.Frame
import org.gurikin.histogram.internal.IntFrame
import org.gurikin.histogram.num_histogram.IntHistogramBuilder
import org.junit.Before

class DefaultChunkStorageTest {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val storage = DefaultChunkStorage<Int>(scope)
    private lateinit var chunk: Chunk<Int>

    @Before
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