package org.esbtools.lightbluenotificationhook;

public enum EventStatus {
    /** New raw event */
    NEW,

    /** Being readied (transient state) */
    READYING,

    /** Normalized and prioritized */
    READY,

    /** Being processed (transient state) */
    PROCESSING,

    /** Processed */
    PROCESSED,

    /** Superseded by a duplicate event */
    SUPERSEDED,

    /** Merged into another event */
    MERGED;
}
