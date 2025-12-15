package com.myapp.utils;

import com.myapp.model.HistoryEntry;
import com.myapp.model.Point;
import com.myapp.model.TransportMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HistoryManagerTest {
    private Path tempFile;

    private HistoryManager newManager() throws IOException {
        tempFile = Files.createTempFile("history-", ".json");
        tempFile.toFile().deleteOnExit();
        return new HistoryManager(tempFile.toString());
    }

    @AfterEach
    void cleanup() throws IOException {
        if (tempFile != null && Files.exists(tempFile)) {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    void addAndPersistEntries() throws IOException {
        HistoryManager manager = newManager();

        Point origin = new Point(38.7223, -9.1393, "Lisboa");
        Point dest = new Point(41.1579, -8.6291, "Porto");
        HistoryEntry e1 = new HistoryEntry(origin, dest, List.of(), TransportMode.CAR);
        manager.addEntry(e1);

        Point origin2 = new Point(40.6405, -8.6538, "Aveiro");
        Point dest2 = new Point(39.3999, -8.2245, "Portugal");
        HistoryEntry e2 = new HistoryEntry(origin2, dest2, List.of(), TransportMode.BIKE);
        manager.addEntry(e2);

        List<HistoryEntry> history = manager.getHistory();
        assertEquals(2, history.size());
        // Most recent first
        assertSame(e2, history.get(0));
        assertSame(e1, history.get(1));

        // Reload from file and verify content is persisted
        HistoryManager reloaded = new HistoryManager(tempFile.toString());
        List<HistoryEntry> reloadedHistory = reloaded.getHistory();
        assertEquals(2, reloadedHistory.size());
        assertEquals("Aveiro", reloadedHistory.get(0).getOrigin().getName());
        assertEquals("Lisboa", reloadedHistory.get(1).getOrigin().getName());
    }

    @Test
    void clearHistoryEmptiesListAndFile() throws IOException {
        HistoryManager manager = newManager();
        HistoryEntry e = new HistoryEntry(new Point(1,1,"A"), new Point(2,2,"B"), List.of(), TransportMode.FOOT);
        manager.addEntry(e);
        assertFalse(manager.getHistory().isEmpty());

        manager.clearHistory();
        assertTrue(manager.getHistory().isEmpty());

        // Reload to ensure persisted empty state
        HistoryManager reloaded = new HistoryManager(tempFile.toString());
        assertTrue(reloaded.getHistory().isEmpty());
    }
}
