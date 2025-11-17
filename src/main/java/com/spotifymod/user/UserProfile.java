package com.spotifymod.user;

import java.util.UUID;

public class UserProfile {
    private final String profileId;
    private String profileName;
    private String accessToken;
    private String refreshToken;
    private long tokenExpiresAt;
    private boolean isActive;

    public UserProfile(String profileName) {
        this.profileId = UUID.randomUUID().toString();
        this.profileName = profileName;
        this.isActive = false;
    }

    public UserProfile(String profileId, String profileName, String accessToken, String refreshToken, long tokenExpiresAt) {
        this.profileId = profileId;
        this.profileName = profileName;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenExpiresAt = tokenExpiresAt;
        this.isActive = false;
    }

    public String getProfileId() {
        return profileId;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public long getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(long tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean hasValidTokens() {
        return accessToken != null && !accessToken.isEmpty() && refreshToken != null && !refreshToken.isEmpty();
    }
}
