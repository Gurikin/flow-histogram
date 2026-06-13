package org.gurikin.histogram.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.gurikin.histogram.internal.Border
import org.gurikin.histogram.internal.Chunk
import org.gurikin.histogram.internal.ChunkId
import org.gurikin.histogram.internal.DefaultChunkQueue
import org.gurikin.histogram.internal.Frame
import org.gurikin.histogram.internal.IntFrame
import org.gurikin.histogram.num_histogram.IntHistogramBuilder
import org.junit.Before
import org.junit.jupiter.api.assertDoesNotThrow

class DefaultChunkQueueTest {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val queue = DefaultChunkQueue(scope)
    private lateinit var chunk: Chunk<Int>

    @Before
    fun setUp() {
        val builder = IntHistogramBuilder()
        val border: Border<Int> = Border(IntFrame(0), IntFrame(100))
        val histogram = builder.initHistogram(border, 10)
        chunk = Chunk(histogram, ChunkId())
    }

    @Test
    fun addAndPoll() = runBlocking {
        assertDoesNotThrow("Should not throw an exception") {
            queue.add(chunk.chunkId)
            val actualChunkId = queue.poll()
            assertEquals(chunk.chunkId, actualChunkId)
        }
    }
}
