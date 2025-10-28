package com.demo.demo.model;

/**
 * Predefined complaint categories. Stored as string in DB (easier to read & evolve).
 */
public enum ComplaintCategory {
    GARBAGE,
    POTHOLE,
    STREETLIGHT,
    WATER_LEAK,
    NOISE,
    OTHER
}