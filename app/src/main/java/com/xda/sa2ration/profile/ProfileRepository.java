package com.xda.sa2ration.profile;

import android.content.Context;
import android.util.AtomicFile;

import com.xda.sa2ration.domain.DisplayConfiguration;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ProfileRepository {
    private static volatile ProfileRepository instance;
    private final AtomicFile file;
    private final ProfileJsonCodec codec = new ProfileJsonCodec();

    private ProfileRepository(Context context) {
        File target = new File(context.getApplicationContext().getFilesDir(), "profiles-v1.json");
        file = new AtomicFile(target);
    }

    public static ProfileRepository getInstance(Context context) {
        if (instance == null) synchronized (ProfileRepository.class) {
            if (instance == null) instance = new ProfileRepository(context);
        }
        return instance;
    }

    public synchronized List<DisplayProfile> load() {
        if (!file.getBaseFile().exists()) {
            List<DisplayProfile> defaults = ProfileTemplates.defaults();
            save(defaults);
            return copies(defaults);
        }
        try (FileInputStream input = file.openRead(); ByteArrayOutputStream bytes = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            for (int read; (read = input.read(buffer)) != -1; ) bytes.write(buffer, 0, read);
            ProfileDocument document = codec.decode(bytes.toString(StandardCharsets.UTF_8.name()));
            mergeMissingDefaults(document.profiles);
            return copies(document.profiles);
        } catch (Exception error) {
            return copies(ProfileTemplates.defaults());
        }
    }

    public synchronized void save(List<DisplayProfile> profiles) {
        ProfileDocument document = new ProfileDocument();
        document.profiles = copies(profiles);
        FileOutputStream output = null;
        try {
            output = file.startWrite();
            output.write(codec.encode(document).getBytes(StandardCharsets.UTF_8));
            file.finishWrite(output);
        } catch (Exception error) {
            if (output != null) file.failWrite(output);
            throw new IllegalStateException("Falha ao salvar perfis", error);
        }
    }

    public synchronized DisplayProfile create(String name, DisplayConfiguration configuration) {
        List<DisplayProfile> profiles = load();
        DisplayProfile profile = new DisplayProfile();
        profile.id = UUID.randomUUID().toString();
        profile.name = name.trim().isEmpty() ? "Personalizado" : name.trim();
        profile.configuration = configuration.copy();
        profile.configuration.activeProfileId = profile.id;
        profiles.add(profile);
        save(profiles);
        return profile.copy();
    }

    public synchronized DisplayProfile duplicate(String id) {
        List<DisplayProfile> profiles = load();
        for (DisplayProfile source : profiles) if (source.id.equals(id)) {
            DisplayProfile copy = source.copy();
            copy.id = UUID.randomUUID().toString();
            copy.name = source.name + " (cópia)";
            copy.configuration.activeProfileId = copy.id;
            copy.builtIn = false;
            copy.createdAtEpochMs = System.currentTimeMillis();
            profiles.add(copy);
            save(profiles);
            return copy;
        }
        return null;
    }

    public synchronized boolean rename(String id, String name) {
        List<DisplayProfile> profiles = load();
        for (DisplayProfile profile : profiles) if (profile.id.equals(id) && !profile.builtIn) {
            profile.name = name.trim();
            save(profiles);
            return true;
        }
        return false;
    }

    public synchronized boolean delete(String id) {
        List<DisplayProfile> profiles = load();
        boolean removed = profiles.removeIf(profile -> profile.id.equals(id) && !profile.builtIn);
        if (removed) save(profiles);
        return removed;
    }

    public synchronized String exportJson() {
        ProfileDocument document = new ProfileDocument();
        document.profiles = load();
        return codec.encode(document);
    }

    public synchronized boolean importJson(String json) {
        try {
            ProfileDocument document = codec.decode(json);
            mergeMissingDefaults(document.profiles);
            save(document.profiles);
            return true;
        } catch (Exception error) {
            return false;
        }
    }

    private void mergeMissingDefaults(List<DisplayProfile> profiles) {
        for (DisplayProfile defaultProfile : ProfileTemplates.defaults()) {
            boolean found = false;
            for (DisplayProfile profile : profiles) if (defaultProfile.id.equals(profile.id)) { found = true; break; }
            if (!found) profiles.add(defaultProfile);
        }
    }

    private List<DisplayProfile> copies(List<DisplayProfile> input) {
        List<DisplayProfile> output = new ArrayList<>();
        for (DisplayProfile profile : input) output.add(profile.copy());
        return output;
    }
}
