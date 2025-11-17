package com.spotifymod.user;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ProfileManager {
    private final File profilesFile;
    private final Gson gson;
    private List<UserProfile> profiles;
    private UserProfile activeProfile;

    public ProfileManager(File configDir) {
        this.profilesFile = new File(configDir, "spotify_profiles.json");
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.profiles = new ArrayList<>();
        load();
    }

    public void load() {
        if (!profilesFile.exists()) {
            profiles = new ArrayList<>();
            return;
        }

        try (Reader reader = new FileReader(profilesFile)) {
            Type listType = new TypeToken<ArrayList<UserProfile>>(){}.getType();
            profiles = gson.fromJson(reader, listType);
            if (profiles == null) {
                profiles = new ArrayList<>();
            }

            // Find active user profile
            for (UserProfile profile : profiles) {
                if (profile.isActive()) {
                    activeProfile = profile;
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            profiles = new ArrayList<>();
        }
    }

    public void save() {
        try {
            if (!profilesFile.getParentFile().exists()) {
                profilesFile.getParentFile().mkdirs();
            }

            try (Writer writer = new FileWriter(profilesFile)) {
                gson.toJson(profiles, writer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public UserProfile createProfile(String name) {
        UserProfile profile = new UserProfile(name);
        profiles.add(profile);
        save();
        return profile;
    }

    public void deleteProfile(String profileId) {
        profiles.removeIf(p -> p.getProfileId().equals(profileId));
        if (activeProfile != null && activeProfile.getProfileId().equals(profileId)) {
            activeProfile = null;
        }
        save();
    }

    public void setActiveProfile(UserProfile profile) {
        // Deactivate all profiles
        for (UserProfile p : profiles) {
            p.setActive(false);
        }

        // Activate selected profile
        if (profile != null) {
            profile.setActive(true);
            activeProfile = profile;
        } else {
            activeProfile = null;
        }
        save();
    }

    public UserProfile getActiveProfile() {
        return activeProfile;
    }

    public List<UserProfile> getAllProfiles() {
        return new ArrayList<>(profiles);
    }

    public UserProfile getProfileById(String profileId) {
        for (UserProfile profile : profiles) {
            if (profile.getProfileId().equals(profileId)) {
                return profile;
            }
        }
        return null;
    }
}
