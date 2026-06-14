package org.gurikin.histogram.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.gurikin.histogram.num_histogram.IntHistogramBuilder
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DefaultChunkQueueTest {
    private val scope = CoroutineScope(Dispatchers.Default)
    private val queue = DefaultChunkQueue(scope)
    private lateinit var chunk: Chunk<Int>

    @BeforeEach
    fun setUp() {
        val builder = IntHistogramBuilder()
        val border: Border<Int> = Border(IntFrame(0), IntFrame(100))
        val histogram = builder.initHistogram(border, 10)
        chunk = Chunk(histogram, ChunkId())
    }

    @Test
    fun addAndPoll() = runBlocking {
        var expEx: RuntimeException? = null
        try {
            queue.add(chunk.chunkId)
            val actualChunkId = queue.poll()
            assertEquals(chunk.chunkId, actualChunkId)
        } catch (ex: RuntimeException) {
            expEx = ex
        }
        assertNull(expEx)
    }
}
