package com.myapp.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryEntryTest {

    @Test
    void toStringContainsTimestampAndDescription() {
        Point origin = new Point(38.7, -9.1, "Lisboa");
        Point dest = new Point(41.15, -8.62, "Porto");
        HistoryEntry entry = new HistoryEntry(origin, dest, List.of(), TransportMode.CAR);

        String s = entry.toString();
        // Format should include brackets with timestamp and a description with arrow and mode
        assertTrue(s.startsWith("["), "Should start with [timestamp]");
        assertTrue(s.contains("] "), "Should contain closing bracket and space");
        assertTrue(s.contains("Lisboa"));
        assertTrue(s.contains("Porto"));
        assertTrue(s.contains("CAR"));
        assertTrue(s.contains("➔"));
    }
}
