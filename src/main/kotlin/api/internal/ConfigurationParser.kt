package org.gurikin.api.internal

/**
 * API for work with histogram's configuration
 * Parse source configuration (.yaml file for example) and create [HistogramConfiguration]
 *  with some internal class of data ([Int], [Long], [Double], [Float], etc.)
 */
internal interface ConfigurationParser {
    fun <T, S : Number> parseConfiguration(sourceConfig: T): HistogramConfiguration<S>

    data class HistogramConfiguration<S : Number>(
        val histogramBorders: HistogramBorder<S>
    )

    data class HistogramBorder<S>(val from: S, val to: S)
}


