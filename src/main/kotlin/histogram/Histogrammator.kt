package org.gurikin.histogram

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.gurikin.histogram.internal.Bin
import org.gurikin.histogram.internal.Chunk
import org.gurikin.histogram.internal.ChunkQueue
import org.gurikin.histogram.internal.ChunkStorage
import org.gurikin.histogram.internal.Histogram
import org.gurikin.histogram.internal.binInBorder
import org.gurikin.histogram.internal.binIsCrossingBorder
import org.gurikin.histogram.internal.chunkInBorder
import org.gurikin.histogram.internal.chunkLeftSideInBorder
import org.gurikin.histogram.internal.chunkRightSideInBorder

/**
 * Facade for start build histogram from flow data.
 *
 * Histogrammator periodically got set of chunks and add its data to general histogram.
 */
interface Histogrammator<S : Comparable<S>> {
    suspend fun accumulate()
}

@Serializable
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
                    when {
                        bin.chunkInBorder(chunk) -> accumulateChunkEntire(bin, chunk)
                        bin.chunkLeftSideInBorder(chunk) -> accumulateChunkLeftSide(bin, chunk)
                        bin.chunkRightSideInBorder(chunk) -> accumulateChunkRightSide(bin, chunk)
                        else -> refreshBin(bin)
                    }
                }
            }
        }
    }

    private fun accumulateChunkEntire(bin: Bin<S>, chunk: Chunk<S>) {
        val chunkFrameSum = chunk.histogram.totalFrameSum
        histogram.totalFrameSum -= bin.frameSum
        bin.frameSum += chunkFrameSum
        histogram.totalFrameSum += bin.frameSum
        bin.weight = bin.frameSum.toDouble() / histogram.totalFrameSum
    }

    private fun accumulateChunkLeftSide(bin: Bin<S>, chunk: Chunk<S>) {
        for (chunkBin in chunk.histogram.bins) {
            when {
                bin.binInBorder(chunkBin) || bin.binIsCrossingBorder(chunkBin) -> {
                    histogram.totalFrameSum -= bin.frameSum
                    bin.frameSum += chunkBin.frameSum
                    histogram.totalFrameSum += bin.frameSum
                    bin.weight = bin.frameSum.toDouble() / histogram.totalFrameSum
                }

                else -> continue
            }
        }
    }

    private fun accumulateChunkRightSide(bin: Bin<S>, chunk: Chunk<S>) {
        for (chunkBin in chunk.histogram.bins) {
            when {
                chunkBin.binInBorder(bin) -> {
                    histogram.totalFrameSum -= bin.frameSum
                    bin.frameSum += chunkBin.frameSum
                    histogram.totalFrameSum += bin.frameSum
                    bin.weight = bin.frameSum.toDouble() / histogram.totalFrameSum
                }

                else -> continue
            }
        }
    }

    private fun refreshBin(bin: Bin<S>) {
        bin.weight = bin.frameSum.toDouble() / histogram.totalFrameSum
    }
}