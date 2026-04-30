package org.gurikin.histogram.internal

/**
 * API for work with histogram's configuration
 * Parse source configuration (.yaml file for example) and create [HistogramConfiguration]
 *  with some internal class of data ([Int], [Long], [Double], [Float], etc.)
 */
public interface ConfigurationParser {
    fun <T, S: Comparable<S>> parseConfiguration(sourceConfig: T): HistogramConfiguration<S>
}


