package org.gurikin.histogram.internal

fun <S : Comparable<S>> Chunk<S>.calcCovariance() {
    for (bin in this.histogram.bins) {
        val center = bin.border.getBorderCenter()
        bin.covariance = center * bin.getFrameSum() / this.histogram.getFrameSum()
        this.histogram.covariance += bin.covariance
    }
}
