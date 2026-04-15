package org.gurikin.histogram.num_histogram

import org.gurikin.histogram.internal.Bin
import org.gurikin.histogram.internal.Histogram

class IntHistogram(val bins: List<Bin<Int>>) : Histogram<Int>(bins) {
}