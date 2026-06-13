package org.gurikin.histogram.internal

import kotlinx.serialization.Serializable

@Serializable
enum class HistogramSourceTypesEnum() {
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    STRING,
}