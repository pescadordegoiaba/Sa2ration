package com.xda.sa2ration.profile;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class ProfileJsonCodec {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public String encode(ProfileDocument document) {
        return gson.toJson(document);
    }

    public ProfileDocument decode(String json) {
        ProfileDocument document = gson.fromJson(json, ProfileDocument.class);
        if (document == null || document.schemaVersion < 1 || document.profiles == null) {
            throw new IllegalArgumentException("Documento de perfis inválido");
        }
        for (DisplayProfile profile : document.profiles) {
            if (profile == null || profile.id == null || profile.name == null || profile.configuration == null) {
                throw new IllegalArgumentException("Perfil incompleto");
            }
            profile.configuration.sanitize();
            profile.formatVersion = DisplayProfile.FORMAT_VERSION;
        }
        return document;
    }
}
