package com.myapp.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.myapp.model.HistoryEntry;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Gere a persistência do histórico de rotas em ficheiro JSON.
 */
public class HistoryManager {
    private static final String FILE_NAME = "history.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private List<HistoryEntry> history;

    public HistoryManager() {
        this.history = loadHistory();
    }

    /**
     * Adiciona uma nova entrada e guarda no ficheiro.
     */
    public void addEntry(HistoryEntry entry) {
        history.add(0, entry); // Adiciona no início (mais recente primeiro)
        saveHistory();
    }

    public List<HistoryEntry> getHistory() {
        return history;
    }

    public void clearHistory() {
        history.clear();
        saveHistory();
    }

    private void saveHistory() {
        try (Writer writer = new FileWriter(FILE_NAME)) {
            GSON.toJson(history, writer);
        } catch (IOException e) {
            System.err.println("Erro ao guardar histórico: " + e.getMessage());
        }
    }

    private List<HistoryEntry> loadHistory() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (Reader reader = new FileReader(file)) {
            Type listType = new TypeToken<ArrayList<HistoryEntry>>(){}.getType();
            List<HistoryEntry> loaded = GSON.fromJson(reader, listType);
            return loaded != null ? loaded : new ArrayList<>();
        } catch (IOException e) {
            System.err.println("Erro ao ler histórico: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
