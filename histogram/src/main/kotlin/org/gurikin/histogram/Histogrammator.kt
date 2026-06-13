package org.gurikin.histogram

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
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
import org.gurikin.histogram.internal.calcCovariance
import org.gurikin.histogram.internal.chunkInBorder
import org.gurikin.histogram.internal.chunkLeftSideInBorder
import org.gurikin.histogram.internal.chunkRightSideInBorder
import org.gurikin.histogram.internal.plusAssign
import org.gurikin.histogram.internal.setWeight

/**
 * Facade for start build histogram from flow data.
 *
 * Histogrammator periodically got set of chunks and add its data to general histogram.
 */
interface Histogrammator<S : Comparable<S>> {
    suspend fun accumulate()
    suspend fun calcAvg()
}

@Serializable
class DefaultHistogrammator<S : Comparable<S>>(
    val histogram: org.gurikin.histogram.internal.Histogram<S>,
    private val chunkQueue: org.gurikin.histogram.internal.ChunkQueue,
    private val chunkStorage: org.gurikin.histogram.internal.ChunkStorage<S>,
    private val scope: CoroutineScope,
    private val accumulateDelay: Duration = 10.milliseconds,
    private val refreshBinsDelay: Duration = 100.milliseconds,
    private val calcAvgDelay: Duration = 2000.milliseconds,
) : org.gurikin.histogram.Histogrammator<S> {

    override suspend fun accumulate() {
        scope.launch {
            while (true) {
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
                histogram.covariance += chunk.histogram.covariance
                chunkStorage.remove(chunkId)
                delay(accumulateDelay)
            }
        }
        scope.launch {
            while (scope.isActive) {
                for (bin in histogram.bins) {
                    refreshBin(bin)
                }
                delay(refreshBinsDelay)
            }
        }
    }

    override suspend fun calcAvg() {
        scope.launch {
            while (true) {
                delay(calcAvgDelay)
                histogram.refreshAvg()
            }
        }
    }

    fun getTotalWeight(): Double = histogram.bins.sumOf { it.weight }

    private fun accumulateChunkEntire(bin: Bin<S>, chunk: Chunk<S>) {
        val chunkFrameSum = chunk.histogram.getFrameSum()
        histogram.incrementFrameSum(chunkFrameSum)
        bin.incrementFrameSum(chunkFrameSum)
        bin.setWeight(bin.getFrameSum().toDouble() / histogram.getFrameSum())
        chunk.calcCovariance()
    }

    private fun accumulateChunkLeftSide(bin: Bin<S>, chunk: Chunk<S>) {
        for (chunkBin in chunk.histogram.bins.filter { it.getFrameSum() != 0 }) {
            when {
                bin.binInBorder(chunkBin) || bin.binIsCrossingBorder(chunkBin) -> {
                    bin.incrementFrameSum(chunkBin.getFrameSum())
                    histogram.incrementFrameSum(chunkBin.getFrameSum())
                    bin.setWeight(bin.getFrameSum().toDouble() / histogram.getFrameSum())
                    chunk.calcCovariance()
                }

                else -> continue
            }
        }
    }

    private fun accumulateChunkRightSide(bin: Bin<S>, chunk: Chunk<S>) {
        for (chunkBin in chunk.histogram.bins) {
            when {
                bin.binInBorder(chunkBin) -> {
                    bin.incrementFrameSum(chunkBin.getFrameSum())
                    histogram.incrementFrameSum(chunkBin.getFrameSum())
                    bin.setWeight(bin.getFrameSum().toDouble() / histogram.getFrameSum())
                    chunk.calcCovariance()
                }

                else -> continue
            }
        }
    }

    private fun refreshBin(bin: Bin<S>) {
        if (histogram.getFrameSum() == 0) {
            return
        }
        bin.setWeight(bin.getFrameSum().toDouble() / histogram.getFrameSum())
    }
}