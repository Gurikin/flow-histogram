package org.gurikin.histogram.num_histogram

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import org.gurikin.histogram.SourceFlowGenerator
import org.gurikin.histogram.internal.Point
import org.gurikin.histogram.internal.PointFrame
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random
import kotlin.random.nextInt

class Int3DFlowGenerator(
    private val range: IntRange,
    private val messagesCount: Int = 1000,
    private val isGaussian: Boolean = true
) : SourceFlowGenerator<Int> {

    override fun flowData() = flow {
        while (currentCoroutineContext().isActive) {
            repeat(messagesCount) {
                if (isGaussian) {
                    val x = Random.nextInt(range)
                    val y = Random.nextInt(range)
                    val z = Random.nextInt(range)
                    val gausX: Double = (1 / (1 * (sqrt(2 * PI)))) * E.pow((x.toDouble() / messagesCount).pow(2))
                    val gausY: Double = (1 / (1 * (sqrt(2 * PI)))) * E.pow((y.toDouble() / messagesCount).pow(2))
                    val gausZ: Double = (1 / (1 * (sqrt(2 * PI)))) * E.pow((z.toDouble() / messagesCount).pow(2))
                    val frame = PointFrame(
                        Point(
                            x = (gausX * messagesCount).toInt(),
                            y = (gausY * messagesCount).toInt(),
                            z = (gausZ * messagesCount).toInt()
                        )
                    )
                    println("[FlowGenerator] Frame = $frame")
                    emit(frame)
                } else {
                    val cord = Random.nextInt(range)
                    val frame = PointFrame(Point(x = cord, y = cord, z = cord))
                    println("[FlowGenerator] Frame = $frame")
                    emit(frame)
                }

            }
            delay(1000)
        }
    }
}