package org.gurikin.histogram

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.gurikin.histogram.internal.Bin
import org.gurikin.histogram.internal.Chunk
import org.gurikin.histogram.internal.ChunkQueue
import org.gurikin.histogram.internal.ChunkStorage
import org.gurikin.histogram.internal.Histogram
import org.gurikin.histogram.internal.chunkInBorder

/**
 * Facade for start build histogram from flow data.
 *
 * Histogrammator periodically got set of chunks and add its data to general histogram.
 */
interface Histogrammator<S : Comparable<S>> {
    suspend fun accumulate()
}

class DefaultHistogrammator<S : Comparable<S>>(
    val histogram: Histogram<S>,
    private val chunkQueue: ChunkQueue,
    private val chunkStorage: ChunkStorage<S>,
    private val scope: CoroutineScope
) : Histogrammator<S> {

    override suspend fun accumulate() {
        scope.launch {
            while (scope.isActive) {
                val chunkId = chunkQueue.poll()
                val chunk: Chunk<S> = chunkStorage.getChunk(chunkId)
                for (bin in histogram.bins) {
                    if (bin.chunkInBorder(chunk)) {
                        accumulateChunkEntire(bin, chunk)
                    } else {
                        accumulateChunkPartially(bin, chunk)
                    }
                }
            }
        }
    }

    private fun accumulateChunkEntire(bin: Bin<S>, chunk: Chunk<S>) {
        val totalFrameSum = histogram.totalFrameSum
        val chunkFrameSum = chunk.histogram.totalFrameSum
        bin.frameSum += chunkFrameSum
        bin.weight = bin.frameSum.toDouble() / totalFrameSum
    }

    private fun accumulateChunkPartially(bin: Bin<S>, chunk: Chunk<S>) {

    }
}