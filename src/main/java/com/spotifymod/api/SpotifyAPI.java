package com.spotifymod.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.spotifymod.config.SpotifyConfig;
import com.spotifymod.debug.LogBuffer;
import com.spotifymod.user.ProfileManager;
import com.spotifymod.user.UserProfile;
import okhttp3.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SpotifyAPI {
    private static final String API_BASE = "https://api.spotify.com/v1";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    
    // Credentials loaded from spotify_credentials.json
    // Create your own Spotify app at https://developer.spotify.com/dashboard
    private final String CLIENT_ID;
    private final String CLIENT_SECRET;
    private static final String REDIRECT_URI = "http://127.0.0.1:8888/callback";
    
    private final OkHttpClient httpClient;
    private final SpotifyConfig config;
    private final ProfileManager profileManager;
    private final JsonParser jsonParser;

    public SpotifyAPI(SpotifyConfig config, ProfileManager profileManager) {
        this.httpClient = new OkHttpClient();
        this.config = config;
        this.profileManager = profileManager;
        this.jsonParser = new JsonParser();
        
        // Load credentials from config
        SpotifyCredentials credentials = config.loadCredentials();
        this.CLIENT_ID = credentials.getClientId();
        this.CLIENT_SECRET = credentials.getClientSecret();
    }

    public boolean isAuthenticated() {
        UserProfile profile = profileManager.getActiveProfile();
        return profile != null && profile.hasValidTokens();
    }

    public boolean isTokenExpired() {
        UserProfile profile = profileManager.getActiveProfile();
        if (profile == null) return true;
        return System.currentTimeMillis() >= profile.getTokenExpiresAt();
    }

    private String getAccessToken() {
        UserProfile profile = profileManager.getActiveProfile();
        return profile != null ? profile.getAccessToken() : null;
    }

    private String getRefreshToken() {
        UserProfile profile = profileManager.getActiveProfile();
        return profile != null ? profile.getRefreshToken() : null;
    }

    public String getAuthUrl() {
        return AUTH_URL + "?client_id=" + CLIENT_ID +
                "&response_type=code" +
                "&redirect_uri=" + REDIRECT_URI +
                "&scope=user-read-playback-state%20user-modify-playback-state%20user-read-currently-playing%20playlist-read-private%20playlist-read-collaborative";
    }

    public CompletableFuture<Boolean> authenticate(String authCode) {
        LogBuffer.get().info("Starting authentication exchange");
        return CompletableFuture.supplyAsync(() -> {
            try {
                RequestBody body = new FormBody.Builder()
                        .add("grant_type", "authorization_code")
                        .add("code", authCode)
                        .add("redirect_uri", REDIRECT_URI)
                        .add("client_id", CLIENT_ID)
                        .add("client_secret", CLIENT_SECRET)
                        .build();

                Request request = new Request.Builder()
                        .url(TOKEN_URL)
                        .post(body)
                        .build();

                Response response = httpClient.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject json = jsonParser.parse(response.body().string()).getAsJsonObject();
                    String accessToken = json.get("access_token").getAsString();
                    String refreshToken = json.get("refresh_token").getAsString();
                    int expiresIn = json.get("expires_in").getAsInt();
                    long tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000L);

                    // Save to active profile
                    UserProfile profile = profileManager.getActiveProfile();
                    if (profile != null) {
                        profile.setAccessToken(accessToken);
                        profile.setRefreshToken(refreshToken);
                        profile.setTokenExpiresAt(tokenExpiresAt);
                        profileManager.save();
                    }

                    LogBuffer.get().info("Authentication success; token expires in " + expiresIn + "s");
                    return true;
                }
            } catch (Exception e) {
                LogBuffer.get().error("Auth error: " + e.getMessage());
            }
            return false;
        });
    }

    public CompletableFuture<Boolean> refreshAccessToken() {
        String refreshToken = getRefreshToken();
        if (refreshToken == null || refreshToken.isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }

        LogBuffer.get().info("Refreshing access token");
        return CompletableFuture.supplyAsync(() -> {
            try {
                RequestBody body = new FormBody.Builder()
                        .add("grant_type", "refresh_token")
                        .add("refresh_token", refreshToken)
                        .add("client_id", CLIENT_ID)
                        .add("client_secret", CLIENT_SECRET)
                        .build();

                Request request = new Request.Builder()
                        .url(TOKEN_URL)
                        .post(body)
                        .build();

                Response response = httpClient.newCall(request).execute();
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject json = jsonParser.parse(response.body().string()).getAsJsonObject();
                    String accessToken = json.get("access_token").getAsString();
                    int expiresIn = json.get("expires_in").getAsInt();
                    long tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000L);

                    // Save to active profile
                    UserProfile profile = profileManager.getActiveProfile();
                    if (profile != null) {
                        profile.setAccessToken(accessToken);
                        profile.setTokenExpiresAt(tokenExpiresAt);
                        profileManager.save();
                    }

                    LogBuffer.get().info("Token refreshed; new expiry " + expiresIn + "s");
                    return true;
                }
            } catch (Exception e) {
                LogBuffer.get().error("Refresh error: " + e.getMessage());
            }
            return false;
        });
    }

    private CompletableFuture<Boolean> ensureValidToken() {
        if (isTokenExpired()) {
            return refreshAccessToken();
        }
        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<TrackInfo> getCurrentTrack() {
        return ensureValidToken().thenCompose(valid -> {
            if (!valid) return CompletableFuture.completedFuture(null);

            return CompletableFuture.supplyAsync(() -> {
                try {
                    Request request = new Request.Builder()
                            .url(API_BASE + "/me/player/currently-playing")
                            .addHeader("Authorization", "Bearer " + getAccessToken())
                            .build();

                    Response response = httpClient.newCall(request).execute();
                    if (response.code() == 204) {
                        // 204 = No Content - no active playback
                        LogBuffer.get().trace("getCurrentTrack: No active playback");
                        return null;
                    }
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        if (responseBody.isEmpty()) {
                            LogBuffer.get().trace("getCurrentTrack: Empty response");
                            return null;
                        }

                        JsonObject json = jsonParser.parse(responseBody).getAsJsonObject();
                        return parseTrackInfo(json);
                    } else {
                        LogBuffer.get().warn("getCurrentTrack failed: " + response.code());
                    }
                } catch (Exception e) {
                    LogBuffer.get().error("getCurrentTrack error: " + e.getMessage());
                }
                return null;
            });
        });
    }

    private TrackInfo parseTrackInfo(JsonObject json) {
        try {
            JsonObject item = json.getAsJsonObject("item");
            String trackName = item.get("name").getAsString();
            String artist = item.getAsJsonArray("artists").get(0).getAsJsonObject().get("name").getAsString();
            String album = item.getAsJsonObject("album").get("name").getAsString();
            int durationMs = item.get("duration_ms").getAsInt();
            int progressMs = json.get("progress_ms").getAsInt();
            boolean isPlaying = json.get("is_playing").getAsBoolean();

            return new TrackInfo(trackName, artist, album, durationMs, progressMs, isPlaying);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public CompletableFuture<Boolean> play() {
        return playerControl("play", "PUT", null);
    }

    public CompletableFuture<Boolean> pause() {
        return playerControl("pause", "PUT", null);
    }

    public CompletableFuture<Boolean> next() {
        return playerControl("next", "POST", null);
    }

    public CompletableFuture<Boolean> previous() {
        return playerControl("previous", "POST", null);
    }

    public CompletableFuture<Boolean> setVolume(int percent) {
        if (percent < 0) percent = 0;
        if (percent > 100) percent = 100;
        return playerControl("volume?volume_percent=" + percent, "PUT", null);
    }

    public CompletableFuture<List<Device>> getAvailableDevices() {
        return ensureValidToken().thenCompose(valid -> {
            if (!valid) return CompletableFuture.completedFuture(new ArrayList<>());

            return CompletableFuture.supplyAsync(() -> {
                try {
                    Request request = new Request.Builder()
                            .url(API_BASE + "/me/player/devices")
                            .addHeader("Authorization", "Bearer " + getAccessToken())
                            .build();

                    Response response = httpClient.newCall(request).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        JsonObject json = jsonParser.parse(response.body().string()).getAsJsonObject();
                        List<Device> devices = new ArrayList<>();
                        JsonArray devicesArray = json.getAsJsonArray("devices");
                        for (int i = 0; i < devicesArray.size(); i++) {
                            JsonObject deviceObj = devicesArray.get(i).getAsJsonObject();
                            String id = deviceObj.get("id").getAsString();
                            String name = deviceObj.get("name").getAsString();
                            String type = deviceObj.get("type").getAsString();
                            boolean isActive = deviceObj.get("is_active").getAsBoolean();
                            devices.add(new Device(id, name, type, isActive));
                        }
                        LogBuffer.get().info("Found " + devices.size() + " devices");
                        return devices;
                    }
                } catch (Exception e) {
                    LogBuffer.get().error("getAvailableDevices error: " + e.getMessage());
                }
                return new ArrayList<>();
            });
        });
    }

    public CompletableFuture<Boolean> transferPlayback(String deviceId) {
        return ensureValidToken().thenCompose(valid -> {
            if (!valid) return CompletableFuture.completedFuture(false);

            return CompletableFuture.supplyAsync(() -> {
                try {
                    String jsonBody = "{\"device_ids\":[\"" + deviceId + "\"],\"play\":true}";
                    RequestBody body = RequestBody.create(
                            MediaType.parse("application/json"), 
                            jsonBody
                    );

                    Request request = new Request.Builder()
                            .url(API_BASE + "/me/player")
                            .addHeader("Authorization", "Bearer " + getAccessToken())
                            .put(body)
                            .build();

                    Response response = httpClient.newCall(request).execute();
                    boolean ok = response.isSuccessful() || response.code() == 202;
                    LogBuffer.get().info("transferPlayback deviceId=" + deviceId + " -> " + ok);
                    return ok;
                } catch (Exception e) {
                    LogBuffer.get().error("transferPlayback error: " + e.getMessage());
                    return false;
                }
            });
        });
    }

    public CompletableFuture<List<Playlist>> getUserPlaylists() {
        return ensureValidToken().thenCompose(valid -> {
            if (!valid) return CompletableFuture.completedFuture(new ArrayList<>());

            return CompletableFuture.supplyAsync(() -> {
                try {
                    Request request = new Request.Builder()
                            .url(API_BASE + "/me/playlists?limit=50")
                            .addHeader("Authorization", "Bearer " + getAccessToken())
                            .build();

                    Response response = httpClient.newCall(request).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        JsonObject json = jsonParser.parse(response.body().string()).getAsJsonObject();
                        return parsePlaylistsFromJson(json);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return new ArrayList<>();
            });
        });
    }

    private List<Playlist> parsePlaylistsFromJson(JsonObject json) {
        List<Playlist> playlists = new ArrayList<>();
        try {
            JsonArray items = json.getAsJsonArray("items");
            for (int i = 0; i < items.size(); i++) {
                JsonObject item = items.get(i).getAsJsonObject();
                String id = item.get("id").getAsString();
                String name = item.get("name").getAsString();
                String uri = item.get("uri").getAsString();
                int trackCount = item.getAsJsonObject("tracks").get("total").getAsInt();
                playlists.add(new Playlist(id, name, uri, trackCount));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return playlists;
    }

    public CompletableFuture<Boolean> playPlaylist(String playlistUri) {
        return ensureValidToken().thenCompose(valid -> {
            if (!valid) return CompletableFuture.completedFuture(false);

            return CompletableFuture.supplyAsync(() -> {
                try {
                    String jsonBody = "{\"context_uri\":\"" + playlistUri + "\"}";
                    RequestBody body = RequestBody.create(
                            MediaType.parse("application/json"), 
                            jsonBody
                    );

                    Request request = new Request.Builder()
                            .url(API_BASE + "/me/player/play")
                            .addHeader("Authorization", "Bearer " + getAccessToken())
                            .put(body)
                            .build();

                    Response response = httpClient.newCall(request).execute();
                    boolean ok = response.isSuccessful();
                    if (!ok && response.body() != null) {
                        String errorBody = response.body().string();
                        LogBuffer.get().error("playPlaylist failed: " + response.code() + " - " + errorBody);
                        // Check for common errors
                        if (response.code() == 404) {
                            LogBuffer.get().error("No active device found. Open Spotify on a device first!");
                        }
                    } else {
                        LogBuffer.get().info("playPlaylist contextUri=" + playlistUri + " -> success");
                    }
                    return ok;
                } catch (Exception e) {
                    LogBuffer.get().error("playPlaylist error: " + e.getMessage());
                    return false;
                }
            });
        });
    }

    public CompletableFuture<List<PlaylistTrack>> getPlaylistTracks(String playlistId) {
        return ensureValidToken().thenCompose(valid -> {
            if (!valid) return CompletableFuture.completedFuture(new ArrayList<>());

            return CompletableFuture.supplyAsync(() -> {
                try {
                    Request request = new Request.Builder()
                            .url(API_BASE + "/playlists/" + playlistId + "/tracks?limit=100")
                            .addHeader("Authorization", "Bearer " + getAccessToken())
                            .build();

                    Response response = httpClient.newCall(request).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        JsonObject json = jsonParser.parse(response.body().string()).getAsJsonObject();
                        return parsePlaylistTracksFromJson(json);
                    }
                } catch (Exception e) {
                    LogBuffer.get().error("getPlaylistTracks error: " + e.getMessage());
                }
                return new ArrayList<>();
            });
        });
    }

    private List<PlaylistTrack> parsePlaylistTracksFromJson(JsonObject json) {
        List<PlaylistTrack> tracks = new ArrayList<>();
        try {
            JsonArray items = json.getAsJsonArray("items");
            for (int i = 0; i < items.size(); i++) {
                JsonObject item = items.get(i).getAsJsonObject();
                if (!item.has("track")) continue;
                
                JsonObject track = item.getAsJsonObject("track");
                if (track.isJsonNull()) continue;
                
                String uri = track.get("uri").getAsString();
                String name = track.get("name").getAsString();
                String artist = track.getAsJsonArray("artists").get(0).getAsJsonObject().get("name").getAsString();
                int durationMs = track.get("duration_ms").getAsInt();
                
                tracks.add(new PlaylistTrack(uri, name, artist, durationMs));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tracks;
    }

    public CompletableFuture<Boolean> playTrackFromPlaylist(String playlistUri, String trackUri) {
        return ensureValidToken().thenCompose(valid -> {
            if (!valid) return CompletableFuture.completedFuture(false);

            return CompletableFuture.supplyAsync(() -> {
                try {
                    String jsonBody = "{\"context_uri\":\"" + playlistUri + "\",\"offset\":{\"uri\":\"" + trackUri + "\"}}";
                    RequestBody body = RequestBody.create(
                            MediaType.parse("application/json"), 
                            jsonBody
                    );

                    Request request = new Request.Builder()
                            .url(API_BASE + "/me/player/play")
                            .addHeader("Authorization", "Bearer " + getAccessToken())
                            .put(body)
                            .build();

                    Response response = httpClient.newCall(request).execute();
                    boolean ok = response.isSuccessful();
                    if (!ok) {
                        if (response.code() == 404) {
                            LogBuffer.get().error("playTrackFromPlaylist: No active device");
                        } else if (response.body() != null) {
                            LogBuffer.get().error("playTrackFromPlaylist failed: " + response.code() + " - " + response.body().string());
                        }
                    } else {
                        LogBuffer.get().info("playTrackFromPlaylist: success");
                    }
                    return ok;
                } catch (Exception e) {
                    LogBuffer.get().error("playTrackFromPlaylist error: " + e.getMessage());
                    return false;
                }
            });
        });
    }
    
    public CompletableFuture<Boolean> playTrack(String trackUri) {
        return ensureValidToken().thenCompose(valid -> {
            if (!valid) return CompletableFuture.completedFuture(false);

            return CompletableFuture.supplyAsync(() -> {
                try {
                    String jsonBody = "{\"uris\":[\"" + trackUri + "\"]}";
                    RequestBody body = RequestBody.create(
                            MediaType.parse("application/json"), 
                            jsonBody
                    );

                    Request request = new Request.Builder()
                            .url(API_BASE + "/me/player/play")
                            .addHeader("Authorization", "Bearer " + getAccessToken())
                            .put(body)
                            .build();

                    Response response = httpClient.newCall(request).execute();
                    boolean ok = response.isSuccessful();
                    if (!ok && response.body() != null) {
                        LogBuffer.get().error("playTrack failed: " + response.body().string());
                    }
                    return ok;
                } catch (Exception e) {
                    LogBuffer.get().error("playTrack error: " + e.getMessage());
                    return false;
                }
            });
        });
    }
    
    public CompletableFuture<Boolean> playArtist(String artistUri) {
        return ensureValidToken().thenCompose(valid -> {
            if (!valid) return CompletableFuture.completedFuture(false);

            return CompletableFuture.supplyAsync(() -> {
                try {
                    String jsonBody = "{\"context_uri\":\"" + artistUri + "\"}";
                    RequestBody body = RequestBody.create(
                            MediaType.parse("application/json"), 
                            jsonBody
                    );

                    Request request = new Request.Builder()
                            .url(API_BASE + "/me/player/play")
                            .addHeader("Authorization", "Bearer " + getAccessToken())
                            .put(body)
                            .build();

                    Response response = httpClient.newCall(request).execute();
                    boolean ok = response.isSuccessful();
                    if (!ok && response.body() != null) {
                        LogBuffer.get().error("playArtist failed: " + response.body().string());
                    }
                    return ok;
                } catch (Exception e) {
                    LogBuffer.get().error("playArtist error: " + e.getMessage());
                    return false;
                }
            });
        });
    }

    private CompletableFuture<Boolean> playerControl(String endpoint, String method, RequestBody body) {
        return ensureValidToken().thenCompose(valid -> {
            if (!valid) return CompletableFuture.completedFuture(false);

            return CompletableFuture.supplyAsync(() -> {
                try {
                    Request.Builder builder = new Request.Builder()
                            .url(API_BASE + "/me/player/" + endpoint)
                            .addHeader("Authorization", "Bearer " + getAccessToken());

                    if (method.equals("PUT")) {
                        builder.put(body != null ? body : RequestBody.create(null, new byte[0]));
                    } else if (method.equals("POST")) {
                        builder.post(body != null ? body : RequestBody.create(null, new byte[0]));
                    }

                    Response response = httpClient.newCall(builder.build()).execute();
                    boolean ok = response.isSuccessful();
                    if (!ok) {
                        if (response.code() == 404) {
                            LogBuffer.get().error("playerControl failed: No active device. Open Spotify on a device!");
                        } else if (response.body() != null) {
                            LogBuffer.get().error("playerControl " + endpoint + " failed: " + response.code() + " - " + response.body().string());
                        } else {
                            LogBuffer.get().error("playerControl " + endpoint + " failed: " + response.code());
                        }
                    } else {
                        LogBuffer.get().trace("playerControl " + endpoint + " method=" + method + " -> success");
                    }
                    return ok;
                } catch (Exception e) {
                    LogBuffer.get().error("playerControl error: " + e.getMessage());
                    return false;
                }
            });
        });
    }

    public static class TrackInfo {
        public final String name;
        public final String artist;
        public final String album;
        public final int durationMs;
        public final int progressMs;
        public final boolean isPlaying;

        public TrackInfo(String name, String artist, String album, int durationMs, int progressMs, boolean isPlaying) {
            this.name = name;
            this.artist = artist;
            this.album = album;
            this.durationMs = durationMs;
            this.progressMs = progressMs;
            this.isPlaying = isPlaying;
        }

        public String getProgressString() {
            return formatTime(progressMs) + " / " + formatTime(durationMs);
        }

        private String formatTime(int ms) {
            int seconds = ms / 1000;
            int minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    public static class Playlist {
        public final String id;
        public final String name;
        public final String uri;
        public final int trackCount;

        public Playlist(String id, String name, String uri, int trackCount) {
            this.id = id;
            this.name = name;
            this.uri = uri;
            this.trackCount = trackCount;
        }
    }

    public static class PlaylistTrack {
        public final String uri;
        public final String name;
        public final String artist;
        public final int durationMs;

        public PlaylistTrack(String uri, String name, String artist, int durationMs) {
            this.uri = uri;
            this.name = name;
            this.artist = artist;
            this.durationMs = durationMs;
        }

        public String getDurationString() {
            int seconds = durationMs / 1000;
            int minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%d:%02d", minutes, seconds);
        }
    }

    public static class Device {
        public final String id;
        public final String name;
        public final String type;
        public final boolean isActive;

        public Device(String id, String name, String type, boolean isActive) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.isActive = isActive;
        }
    }
    
    public CompletableFuture<List<QueueTrack>> getQueue() {
        return ensureValidToken().thenCompose(valid -> {
            if (!valid) return CompletableFuture.completedFuture(new ArrayList<>());

            return CompletableFuture.supplyAsync(() -> {
                List<QueueTrack> queue = new ArrayList<>();
                try {
                    Request request = new Request.Builder()
                            .url(API_BASE + "/me/player/queue")
                            .addHeader("Authorization", "Bearer " + getAccessToken())
                            .get()
                            .build();

                    Response response = httpClient.newCall(request).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        JsonObject json = jsonParser.parse(response.body().string()).getAsJsonObject();
                        
                        // Get queue items
                        if (json.has("queue")) {
                            JsonArray queueArray = json.getAsJsonArray("queue");
                            // Only get first 5 tracks
                            int limit = Math.min(5, queueArray.size());
                            for (int i = 0; i < limit; i++) {
                                JsonObject track = queueArray.get(i).getAsJsonObject();
                                String name = track.get("name").getAsString();
                                String artist = track.getAsJsonArray("artists").get(0).getAsJsonObject().get("name").getAsString();
                                String album = track.getAsJsonObject("album").get("name").getAsString();
                                int durationMs = track.get("duration_ms").getAsInt();
                                
                                queue.add(new QueueTrack(name, artist, album, durationMs));
                            }
                        }
                        
                        LogBuffer.get().info("Queue has " + queue.size() + " tracks");
                    }
                } catch (Exception e) {
                    LogBuffer.get().error("getQueue error: " + e.getMessage());
                    e.printStackTrace();
                }
                return queue;
            });
        });
    }
    
    public static class QueueTrack {
        public final String name;
        public final String artist;
        public final String album;
        public final int durationMs;
        
        public QueueTrack(String name, String artist, String album, int durationMs) {
            this.name = name;
            this.artist = artist;
            this.album = album;
            this.durationMs = durationMs;
        }
        
        public String getDurationString() {
            int seconds = durationMs / 1000;
            int minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%d:%02d", minutes, seconds);
        }
    }
    
    public CompletableFuture<List<SearchResult>> search(String query, String type) {
        return ensureValidToken().thenCompose(valid -> {
            if (!valid) return CompletableFuture.completedFuture(new ArrayList<>());

            return CompletableFuture.supplyAsync(() -> {
                List<SearchResult> results = new ArrayList<>();
                try {
                    String url = API_BASE + "/search?q=" + java.net.URLEncoder.encode(query, "UTF-8") + 
                                "&type=" + type + "&limit=20";
                    
                    Request request = new Request.Builder()
                            .url(url)
                            .addHeader("Authorization", "Bearer " + getAccessToken())
                            .get()
                            .build();

                    Response response = httpClient.newCall(request).execute();
                    if (response.isSuccessful() && response.body() != null) {
                        JsonObject json = jsonParser.parse(response.body().string()).getAsJsonObject();
                        
                        // Parse tracks
                        if (json.has("tracks")) {
                            JsonArray tracks = json.getAsJsonObject("tracks").getAsJsonArray("items");
                            for (int i = 0; i < tracks.size(); i++) {
                                JsonObject track = tracks.get(i).getAsJsonObject();
                                String name = track.get("name").getAsString();
                                String artist = track.getAsJsonArray("artists").get(0).getAsJsonObject().get("name").getAsString();
                                String uri = track.get("uri").getAsString();
                                String album = track.getAsJsonObject("album").get("name").getAsString();
                                int durationMs = track.get("duration_ms").getAsInt();
                                
                                results.add(new SearchResult(name, artist, album, uri, durationMs, "track"));
                            }
                        }
                        
                        // Parse artists
                        if (json.has("artists")) {
                            JsonArray artists = json.getAsJsonObject("artists").getAsJsonArray("items");
                            for (int i = 0; i < artists.size(); i++) {
                                JsonObject artist = artists.get(i).getAsJsonObject();
                                String name = artist.get("name").getAsString();
                                String uri = artist.get("uri").getAsString();
                                
                                results.add(new SearchResult(name, "", "", uri, 0, "artist"));
                            }
                        }
                        
                        LogBuffer.get().info("Search found " + results.size() + " results");
                    }
                } catch (Exception e) {
                    LogBuffer.get().error("Search error: " + e.getMessage());
                    e.printStackTrace();
                }
                return results;
            });
        });
    }
    
    public CompletableFuture<Boolean> seek(int positionMs) {
        return ensureValidToken().thenCompose(valid -> {
            if (!valid) return CompletableFuture.completedFuture(false);

            return CompletableFuture.supplyAsync(() -> {
                try {
                    Request request = new Request.Builder()
                            .url(API_BASE + "/me/player/seek?position_ms=" + positionMs)
                            .addHeader("Authorization", "Bearer " + getAccessToken())
                            .put(RequestBody.create(null, new byte[0]))
                            .build();

                    Response response = httpClient.newCall(request).execute();
                    boolean ok = response.isSuccessful();
                    if (ok) {
                        LogBuffer.get().info("Seeked to position: " + positionMs + "ms");
                    } else {
                        LogBuffer.get().error("Seek failed: " + response.code());
                    }
                    return ok;
                } catch (Exception e) {
                    LogBuffer.get().error("Seek error: " + e.getMessage());
                    return false;
                }
            });
        });
    }
    
    public CompletableFuture<Boolean> setShuffle(boolean state) {
        return playerControl("shuffle?state=" + state, "PUT", null);
    }
    
    public CompletableFuture<Boolean> setRepeat(String state) {
        // state: "track", "context", "off"
        return playerControl("repeat?state=" + state, "PUT", null);
    }
    
    public static class SearchResult {
        public final String name;
        public final String artist;
        public final String album;
        public final String uri;
        public final int durationMs;
        public final String type;
        
        public SearchResult(String name, String artist, String album, String uri, int durationMs, String type) {
            this.name = name;
            this.artist = artist;
            this.album = album;
            this.uri = uri;
            this.durationMs = durationMs;
            this.type = type;
        }
        
        public String getDurationString() {
            int seconds = durationMs / 1000;
            int minutes = seconds / 60;
            seconds = seconds % 60;
            return String.format("%d:%02d", minutes, seconds);
        }
    }
    
    public void cleanup() {
        // Shutdown HTTP client connection pool
        if (httpClient != null) {
            try {
                httpClient.dispatcher().executorService().shutdown();
                httpClient.connectionPool().evictAll();
                LogBuffer.get().info("HTTP client cleaned up - connections closed");
            } catch (Exception e) {
                LogBuffer.get().error("Error cleaning up HTTP client: " + e.getMessage());
            }
        }
    }
}
