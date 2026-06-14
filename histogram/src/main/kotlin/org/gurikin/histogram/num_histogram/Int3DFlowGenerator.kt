package org.gurikin.histogram.num_histogram

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import org.gurikin.histogram.SourceFlowGenerator
import org.gurikin.histogram.internal.Point
import org.gurikin.histogram.internal.PointFrame
import java.util.*

class Int3DFlowGenerator(
    private val range: IntRange,
    private val messagesCount: Int = 1000,
    private val isGaussian: Boolean = true
) : SourceFlowGenerator<Int> {

    override fun flowData() = flow {
        while (currentCoroutineContext().isActive) {
            val r = Random()
            repeat(messagesCount) {
                if (isGaussian) {
                    val x = r.nextGaussian() * r.nextInt(range.first, range.last)
                    val y = r.nextGaussian() * r.nextInt(range.first, range.last)
                    val z = r.nextGaussian() * r.nextInt(range.first, range.last)
                    val frame = PointFrame(
                        Point(
                            x = x.toInt(),
                            y = y.toInt(),
                            z = z.toInt()
                        )
                    )
//                    println("[Int3DFlowGenerator] Frame = [${frame.value.x},${frame.value.y},${frame.value.z}]")
                    emit(frame)
                } else {
                    val x = r.nextInt(range.first, range.last)
                    val y = r.nextInt(range.first, range.last)
                    val z = r.nextInt(range.first, range.last)
                    val frame = PointFrame(Point(x = x, y = y, z = z))
//                    println("[Int3DFlowGenerator] Frame = [${frame.value.x},${frame.value.y},${frame.value.z}]")
                    emit(frame)
                }

            }
            delay(1000)
        }
    }
}