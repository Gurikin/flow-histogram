package org.gurikin.histogram.num_histogram

import kotlin.random.Random
import kotlin.random.nextInt
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.isActive
import kotlinx.coroutines.isActive
import org.gurikin.histogram.SourceFlowGenerator
import org.gurikin.histogram.internal.Frame

class IntFlowGenerator(private val range: IntRange, private val messagesCount: Int = 1000) : SourceFlowGenerator<Int> {
    override fun flowData() = flow {
        while (currentCoroutineContext().isActive) {
            repeat(messagesCount) {
                emit(Frame(Random.nextInt(range)))
            }
            delay(10)
        }
    }
}