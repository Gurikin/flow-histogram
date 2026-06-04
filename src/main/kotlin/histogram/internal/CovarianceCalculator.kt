package org.gurikin.histogram.internal

//fun <S : Comparable<S>> Chunk<S>.calcCovariance() {
//    for (bin in this.histogram.bins) {
//        val center = bin.border.getBorderCenter()
//        bin.covariance = center * bin.getFrameSum() / this.histogram.getFrameSum()
//        this.histogram.covariance += bin.covariance
//    }
//}

fun <S : Comparable<S>> Chunk<S>.calcDispersion() {
    val mean = this.histogram.bins.sumOf {bin ->
        bin.xBorder.getBorderCenter() * bin.getFrameSum()
    } / this.histogram.getFrameSum()

    for (bin in this.histogram.bins) {
        val center = bin.xBorder.getBorderCenter()
        val deviation = center - mean
        bin.dispersion = bin.weight * deviation * deviation
        this.histogram.dispersion += bin.dispersion
    }
    //    val totalFrames = histogram.bins.sumOf { it.frameSum }.toDouble()
    //    if (totalFrames == 0.0) return
    //
    //    // 1. Вычисляем среднее (математическое ожидание)
    //    val mean = histogram.bins.sumOf { bin ->
    //        bin.border.getBorderCenter() * bin.frameSum
    //    } / totalFrames
    //
    //    // 2. Для каждого бина вычисляем вклад в дисперсию
    //    histogram.bins.forEach { bin ->
    //        val center = bin.border.getBorderCenter()
    //        val weight = bin.frameSum / totalFrames
    //        val deviation = center - mean
    //        bin.contributionToVariance = weight * deviation * deviation
    //    }
}

