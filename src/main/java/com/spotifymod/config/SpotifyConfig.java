package com.spotifymod.config;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public class SpotifyConfig {
    private Configuration config;
    private String accessToken;
    private String refreshToken;
    private long tokenExpiresAt;
    private int hudX;
    private int hudY;
    private boolean hudEnabled;
    private String hudColorScheme;
    private boolean hudBackground;

    public SpotifyConfig(File configFile) {
        config = new Configuration(configFile);
        load();
    }

    public void load() {
        config.load();
        
        accessToken = config.getString("accessToken", "spotify", "", "Spotify Access Token");
        refreshToken = config.getString("refreshToken", "spotify", "", "Spotify Refresh Token");
        String expiresStr = config.getString("tokenExpiresAt", "spotify", "0", "Token Expiration Time");
        try {
            tokenExpiresAt = Long.parseLong(expiresStr);
        } catch (NumberFormatException e) {
            tokenExpiresAt = 0L;
        }
        hudX = config.getInt("hudX", "display", 5, 0, 10000, "HUD X Position");
        hudY = config.getInt("hudY", "display", 5, 0, 10000, "HUD Y Position");
        hudEnabled = config.getBoolean("hudEnabled", "display", true, "Show HUD Overlay");
        hudColorScheme = config.getString("hudColorScheme", "display", "DEFAULT", "HUD Color Scheme");
        hudBackground = config.getBoolean("hudBackground", "display", true, "Show HUD Background");

        if (config.hasChanged()) {
            config.save();
        }
    }

    public void save() {
        config.get("spotify", "accessToken", "").set(accessToken);
        config.get("spotify", "refreshToken", "").set(refreshToken);
        config.get("spotify", "tokenExpiresAt", "0").set(String.valueOf(tokenExpiresAt));
        config.get("display", "hudX", 5).set(hudX);
        config.get("display", "hudY", 5).set(hudY);
        config.get("display", "hudEnabled", true).set(hudEnabled);
        config.get("display", "hudColorScheme", "DEFAULT").set(hudColorScheme);
        config.get("display", "hudBackground", true).set(hudBackground);
        
        if (config.hasChanged()) {
            config.save();
        }
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

    public int getHudX() {
        return hudX;
    }

    public void setHudX(int hudX) {
        this.hudX = hudX;
    }

    public int getHudY() {
        return hudY;
    }

    public void setHudY(int hudY) {
        this.hudY = hudY;
    }

    public boolean isHudEnabled() {
        return hudEnabled;
    }

    public void setHudEnabled(boolean hudEnabled) {
        this.hudEnabled = hudEnabled;
    }

    public String getHudColorScheme() {
        return hudColorScheme;
    }

    public void setHudColorScheme(String hudColorScheme) {
        this.hudColorScheme = hudColorScheme;
    }

    public boolean isHudBackground() {
        return hudBackground;
    }

    public void setHudBackground(boolean hudBackground) {
        this.hudBackground = hudBackground;
    }
}
