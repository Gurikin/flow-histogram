package org.gurikin.histogram.internal

import java.util.TreeMap
import java.util.TreeSet

/**
 * API для расчёта и инициализации параметров общей гистограммы.
 * API для расчёта параметров чанков.
 *
 */
interface ChunkParameterBuilder {
}


data class ChunkParameters(val chunks: TreeMap<Border<*>, Chunk<*>>)