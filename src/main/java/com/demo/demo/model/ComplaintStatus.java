package com.demo.demo.model;

/**
 * Represents the basic life-cycle of a complaint.
 */
public enum ComplaintStatus {
    SUBMITTED,   // user created it
    REJECTED,    // admin rejected it
    IN_PROGRESS, // assigned and being worked on
    AWAITING_VERIFICATION, // vendor marked complete — admin must verify
    COMPLETED    // vendor or admin marked it done
}