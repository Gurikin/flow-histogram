package org.gurikin.histogram.internal

import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable
enum class HistogramSourceTypesEnum(public val klass: KClass<*>) {
    INT(Int::class),
    LONG(Long::class),
    FLOAT(Float::class),
    DOUBLE(Double::class),
}

fun HistogramSourceTypesEnum.getType(): KClass<*> = this.klass