package org.gurikin.histogram.num_histogram

import org.gurikin.histogram.internal.Bin
import org.gurikin.histogram.internal.Histogram

class IntHistogram(bins: List<Bin<Int>>, totalFrameSum: Int) : Histogram<Int>(bins, totalFrameSum)