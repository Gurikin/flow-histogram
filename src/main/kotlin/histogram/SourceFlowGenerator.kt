package org.gurikin.histogram

import kotlinx.coroutines.flow.Flow
import org.gurikin.histogram.internal.Frame

/**
 * API for generation of a flow data
 *
 * Default realise - generating of a flow data for histogram
 *
 * Moved to the external API for have possible implement users types of source data flow
 */
interface SourceFlowGenerator<S : Comparable<S>> {
    fun flowData(): Flow<Frame<S>?>
}