package org.gurikin.histogram.num_histogram

import kotlin.random.Random
import kotlin.random.nextInt
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import org.gurikin.histogram.SourceFlowGenerator
import org.gurikin.histogram.internal.Frame

class IntFlowGenerator : SourceFlowGenerator<Int> {
    override fun flowData(): Flow<Frame<Int>> = flow {
        while (currentCoroutineContext().isActive) {
            emit(Frame(Random.nextInt(0..1000)))
            delay(1000)
        }
    }
}