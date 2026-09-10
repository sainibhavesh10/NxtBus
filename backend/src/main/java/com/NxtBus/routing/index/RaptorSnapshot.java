package com.nxtbus.routing.index;

/** Bundled so a reader can never see an index from one build alongside an
 *  overlay positioned for a different build. */
public record RaptorSnapshot(RaptorIndex index, boolean[] excludedTrips) {}