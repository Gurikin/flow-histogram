package org.gurikin.histogram.internal

/**
 * API for work with histogram's configuration
 * Parse source configuration (.json file for example) and create [HistogramConfiguration]
 *  with some internal class of data ([Int], [Long], [Double], [Float], etc.)
 */
interface ConfigurationParser<in T, S : Comparable<S>> {
    fun parseConfiguration(sourceConfig: T): HistogramConfiguration<S>
}
