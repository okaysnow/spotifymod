package com.spotifymod.api;

public class SpotifyCredentials {
    private String clientId;
    private String clientSecret;

    public SpotifyCredentials() {
        this.clientId = "";
        this.clientSecret = "";
    }

    public SpotifyCredentials(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public boolean isValid() {
        return clientId != null && !clientId.isEmpty() && 
               clientSecret != null && !clientSecret.isEmpty();
    }
}
