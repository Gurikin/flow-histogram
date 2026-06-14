package org.gurikin.histogram.internal

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

fun <S : Comparable<S>> Chunk<S>.calcCovariance() {

    val logger = LoggerFactory.getLogger("org.gurikin.histogram.internal.Chunk<S>.calcCovariance")

    val totalWeight = this.histogram.getFrameSum()
    if (totalWeight == 0) return

    if (this.histogram.bins.mapNotNull { it.yBorder }.isEmpty()) {
        logger.warn("Could not calculate covariance for 1D histogram")
        this.histogram.covariance = CovarianceMatrix3D()
        return
    }

    val meanX = this.histogram.bins.sumOf { it.xBorder.getBorderCenter() * it.weight } / totalWeight
    val meanY = this.histogram.bins
        .mapNotNull { it.yBorder?.getBorderCenter()?.times(it.weight)?.div(totalWeight) }
        .takeIf { it.isNotEmpty() }
        ?.sum()
    val meanZ = this.histogram.bins
        .mapNotNull { it.zBorder?.getBorderCenter()?.times(it.weight)?.div(totalWeight) }
        .takeIf { it.isNotEmpty() }
        ?.sum()

    var varX = 0.0
    var varY = 0.0
    var varZ = 0.0
    var covXY = 0.0
    var covXZ = 0.0
    var covYZ = 0.0

    this.histogram.bins.forEach { bin ->
        val dx = bin.xBorder.getBorderCenter() - meanX
        val dy = bin.yBorder?.getBorderCenter()?.minus(meanY!!)
        val dz = bin.zBorder?.getBorderCenter()?.minus(meanZ!!)
        val w = bin.weight

        varX += w * dx * dx
        varY += dy?.times(dy)?.times(w) ?: 0.0
        varZ += dz?.times(dz)?.times(w) ?: 0.0
        covXY += dy?.times(dx)?.times(w) ?: 0.0
        covXZ += dz?.times(dx)?.times(w) ?: 0.0
        covYZ += dy?.times(dz!!)?.times(w) ?: 0.0
    }

    this.histogram.covariance = CovarianceMatrix3D(
        varX = varX,
        varY = varY,
        varZ = varZ,
        covXY = covXY,
        covXZ = covXZ,
        covYZ = covYZ,
    )
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class CovarianceMatrix3D(
    @EncodeDefault
    var varX: Double = 0.0,
    @EncodeDefault
    var varY: Double = 0.0,
    @EncodeDefault
    var varZ: Double = 0.0,
    @EncodeDefault
    var covXY: Double = 0.0,
    @EncodeDefault
    var covXZ: Double = 0.0,
    @EncodeDefault
    var covYZ: Double = 0.0,
)


operator fun CovarianceMatrix3D.plusAssign(other: CovarianceMatrix3D) {
    this.varX += other.varX
    this.varY += other.varY
    this.varZ += other.varZ
    this.covXY += other.covXY
    this.covXZ += other.covXZ
    this.covYZ += other.covYZ
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class AvgPoint(
    @EncodeDefault
    var varX: Double = 0.0,
    @EncodeDefault
    var varY: Double? = 0.0,
    @EncodeDefault
    var varZ: Double? = 0.0,
)

operator fun AvgPoint.plusAssign(other: AvgPoint) {
    this.varX += other.varX
    if (this.varY != null) {
        this.varY = this.varY!!.plus(other.varY ?: 0.0)
    }
    if (this.varZ != null) {
        this.varZ = this.varZ!!.plus(other.varZ ?: 0.0)
    }
}

//fun <S : Comparable<S>> Chunk<S>.calcDispersion() {
//    val mean = this.histogram.bins.sumOf {bin ->
//        bin.xBorder.getBorderCenter() * bin.getFrameSum()
//    } / this.histogram.getFrameSum()
//
//    for (bin in this.histogram.bins) {
//        val center = bin.xBorder.getBorderCenter()
//        val deviation = center - mean
//        bin.dispersion = bin.weight * deviation * deviation
//        this.histogram.dispersion += bin.dispersion
//    }
//    //    val totalFrames = histogram.bins.sumOf { it.frameSum }.toDouble()
//    //    if (totalFrames == 0.0) return
//    //
//    //    // 1. Вычисляем среднее (математическое ожидание)
//    //    val mean = histogram.bins.sumOf { bin ->
//    //        bin.border.getBorderCenter() * bin.frameSum
//    //    } / totalFrames
//    //
//    //    // 2. Для каждого бина вычисляем вклад в дисперсию
//    //    histogram.bins.forEach { bin ->
//    //        val center = bin.border.getBorderCenter()
//    //        val weight = bin.frameSum / totalFrames
//    //        val deviation = center - mean
//    //        bin.contributionToVariance = weight * deviation * deviation
//    //    }
//}

