package org.gurikin.histogram.num_histogram

import kotlin.math.E
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.random.nextInt
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import org.gurikin.histogram.SourceFlowGenerator
import org.gurikin.histogram.internal.Frame

class IntFlowGenerator(
    private val range: IntRange,
    private val messagesCount: Int = 1000,
    private val isGaussian: Boolean = true
) : SourceFlowGenerator<Int> {

    override fun flowData() = flow {
        while (currentCoroutineContext().isActive) {
            repeat(messagesCount) {
                if (isGaussian) {
                    val x = Random.nextInt(range)
                    val gausX: Double = (1 / (1 * (sqrt(2 * PI)))) * E.pow((x.toDouble() / messagesCount).pow(2))
                    emit(Frame((gausX * messagesCount).toInt()))
                } else {
                    emit(Frame(Random.nextInt(range)))
                }

            }
            delay(1000)
        }
    }
}