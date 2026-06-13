package org.gurikin.histogram.internal

import kotlinx.serialization.json.Json
import java.io.File

/**
 * Get as source String with JSON content and deserialize it to HistogramConfiguration
 *
 * @param S implements [Comparable] need for
 */
class JsonConfigurationParser<S : Comparable<S>> : ConfigurationParser<String, S> {
    override fun parseConfiguration(sourceConfig: String): HistogramConfiguration<S> {
        val sourceJson = File(sourceConfig).readText()
        val configuration = Json.decodeFromString<HistogramConfiguration<S>>(sourceJson)
        return configuration
    }
}