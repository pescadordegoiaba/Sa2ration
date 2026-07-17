package com.xda.sa2ration.persistence;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.xda.sa2ration.PersistenceController;
import com.xda.sa2ration.domain.DisplayConfiguration;
import com.xda.sa2ration.domain.StoredApplicationState;

import java.util.Optional;

/** DataStore-backed state repository with one-time migration from info.properties. */
public final class ConfigurationRepository {
    private static volatile ConfigurationRepository instance;
    private final ConfigurationDataStore dataStore;
    private final Context context;
    private final Gson gson = new GsonBuilder().serializeSpecialFloatingPointValues().create();

    private ConfigurationRepository(Context context) {
        this.context = context.getApplicationContext();
        this.dataStore = ConfigurationDataStore.getInstance(this.context);
    }

    public static ConfigurationRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (ConfigurationRepository.class) {
                if (instance == null) instance = new ConfigurationRepository(context);
            }
        }
        return instance;
    }

    public synchronized StoredApplicationState load() {
        String json = dataStore.readBlocking();
        StoredApplicationState state = null;
        if (json != null && !json.trim().isEmpty()) {
            try { state = gson.fromJson(json, StoredApplicationState.class); }
            catch (RuntimeException ignored) { state = null; }
        }
        if (state == null) {
            state = migrateLegacyState();
            save(state);
        }
        state.sanitize();
        return state;
    }

    public synchronized void save(StoredApplicationState state) {
        state.sanitize();
        dataStore.writeBlocking(gson.toJson(state));
    }

    public synchronized void saveCurrent(DisplayConfiguration current) {
        StoredApplicationState state = load();
        state.current = current.copy();
        save(state);
    }

    public synchronized void confirmStable(DisplayConfiguration configuration) {
        StoredApplicationState state = load();
        state.current = configuration.copy();
        state.stable = configuration.copy();
        state.lastConfirmedAtEpochMs = System.currentTimeMillis();
        state.consecutiveBootFailures = 0;
        save(state);
    }

    public synchronized String exportJson() {
        return gson.toJson(load());
    }

    public synchronized boolean importJson(String json) {
        try {
            StoredApplicationState imported = gson.fromJson(json, StoredApplicationState.class);
            if (imported == null) return false;
            imported.sanitize();
            save(imported);
            return true;
        } catch (RuntimeException error) {
            return false;
        }
    }

    private StoredApplicationState migrateLegacyState() {
        StoredApplicationState state = new StoredApplicationState();
        DisplayConfiguration configuration = DisplayConfiguration.neutral();
        PersistenceController legacy = PersistenceController.getInstance(context);
        configuration.globalSaturation = readDouble(legacy, "SATURATION", 1.0);
        configuration.globalContrast = readDouble(legacy, "CONTRAST", 1.0);
        configuration.redSaturation = readDouble(legacy, "RED_SATURATION", 1.0);
        configuration.greenSaturation = readDouble(legacy, "GREEN_SATURATION", 1.0);
        configuration.blueSaturation = readDouble(legacy, "BLUE_SATURATION", 1.0);
        configuration.redContrast = readDouble(legacy, "RED_CONTRAST", 1.0);
        configuration.greenContrast = readDouble(legacy, "GREEN_CONTRAST", 1.0);
        configuration.blueContrast = readDouble(legacy, "BLUE_CONTRAST", 1.0);
        Optional<String> colorMode = legacy.restoreFromProperties("CM");
        configuration.colorManagementEnabled = !colorMode.isPresent() || "0".equals(colorMode.get());
        configuration.sanitize();
        state.current = configuration.copy();
        state.stable = configuration.copy();
        state.lastConfirmedAtEpochMs = System.currentTimeMillis();
        return state;
    }

    private double readDouble(PersistenceController legacy, String key, double fallback) {
        Optional<String> value = legacy.restoreFromProperties(key);
        if (!value.isPresent()) return fallback;
        try {
            double parsed = Double.parseDouble(value.get());
            return Double.isFinite(parsed) ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}
