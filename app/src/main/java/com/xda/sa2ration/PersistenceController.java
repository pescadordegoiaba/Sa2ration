package com.xda.sa2ration;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import static android.content.Context.MODE_PRIVATE;

public class PersistenceController {

    private Map<String, String> values;
    private Context context;
    private static PersistenceController instance;

    private PersistenceController(Context context) {
        this.context = context.getApplicationContext();
        values = new HashMap<>();
        Properties properties = new Properties();
        try (FileInputStream fis  = context.openFileInput("info.properties")) {
            properties.load(fis);
            for (String name : properties.stringPropertyNames()) {
                values.put(name, properties.getProperty(name));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static PersistenceController getInstance(Context context) {
        if (instance == null) {
            instance = new PersistenceController(context);
        }
        return instance;
    }

    /**
     * Restores property from saved properties.
     * @param propName name of the property.
     * @return property value, if present.
     */
    public Optional<String> restoreFromProperties(String propName) {
        return Optional.ofNullable(values.get(propName));
    }

    /**
     * Stores or updates property to properties.
     * @param propName name of the property.
     * @param value new value for property.
     */
    public void storeToProperties(String propName, String value) {
        values.put(propName, value);
    }

    /**
     * Persists all properties in memory to system storage.
     */
    public void persist() {
        Properties properties = new Properties();
        try (FileOutputStream fos  = context.openFileOutput("info.properties", MODE_PRIVATE)) {
            properties.putAll(values);
            properties.store(fos, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
