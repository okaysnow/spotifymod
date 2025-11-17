package com.spotifymod.party;

import com.spotifymod.SpotifyMod;
import com.spotifymod.api.SpotifyAPI;
import com.spotifymod.user.UserProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PartyManager {
    private static final Map<String, ListeningParty> parties = new ConcurrentHashMap<>();
    private ListeningParty currentParty;
    private boolean isHost;
    private long lastSyncTime = 0;
    private static final long SYNC_INTERVAL = 1000; // 1 second

    public ListeningParty createParty(String partyName) {
        UserProfile profile = SpotifyMod.instance.getProfileManager().getActiveProfile();
        if (profile == null) {
            return null;
        }

        ListeningParty party = new ListeningParty(profile.getProfileId(), partyName);
        parties.put(party.getPartyId(), party);
        currentParty = party;
        isHost = true;
        
        return party;
    }

    public boolean joinParty(String partyId) {
        ListeningParty party = parties.get(partyId);
        if (party == null) {
            return false;
        }

        UserProfile profile = SpotifyMod.instance.getProfileManager().getActiveProfile();
        if (profile == null) {
            return false;
        }

        if (party.isFull()) {
            return false;
        }

        ListeningParty.PartyMember member = new ListeningParty.PartyMember(
            profile.getProfileId(),
            profile.getProfileName(),
            false
        );

        if (party.addMember(member)) {
            leaveCurrentParty(); // Leave any existing party
            currentParty = party;
            isHost = false;
            syncWithHost();
            return true;
        }

        return false;
    }

    public void leaveCurrentParty() {
        if (currentParty == null) {
            return;
        }

        UserProfile profile = SpotifyMod.instance.getProfileManager().getActiveProfile();
        if (profile != null) {
            currentParty.removeMember(profile.getProfileId());
        }

        if (isHost) {
            // If host leaves, disband the party
            parties.remove(currentParty.getPartyId());
        }

        currentParty = null;
        isHost = false;
    }

    public void updatePartyState(String trackUri, int positionMs, boolean playing) {
        if (currentParty == null || !isHost) {
            return;
        }

        currentParty.setCurrentTrackUri(trackUri);
        currentParty.setCurrentPositionMs(positionMs);
        currentParty.setPlaying(playing);
    }

    public void syncWithHost() {
        if (currentParty == null || isHost) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSyncTime < SYNC_INTERVAL) {
            return; // Don't sync too frequently
        }

        lastSyncTime = currentTime;

        String trackUri = currentParty.getCurrentTrackUri();
        int positionMs = currentParty.getCurrentPositionMs();
        boolean isPlaying = currentParty.isPlaying();

        // Calculate adjusted position based on time elapsed
        long timeSinceUpdate = currentTime - currentParty.getLastUpdateTime();
        if (isPlaying && timeSinceUpdate > 0) {
            positionMs += (int) timeSinceUpdate;
        }

        // Apply to local player
        SpotifyAPI api = SpotifyMod.instance.getSpotifyAPI();
        if (trackUri != null) {
            // Note: This is simplified - you'd need to implement track URI playback
            final int finalPositionMs = positionMs;
            final boolean finalIsPlaying = isPlaying;
            api.getCurrentTrack().thenAccept(currentTrack -> {
                if (currentTrack != null) {
                    // Check if we need to sync
                    int drift = Math.abs(currentTrack.progressMs - finalPositionMs);
                    if (drift > 3000) { // More than 3 seconds drift
                        // Would need seek functionality in Spotify API
                        System.out.println("Drift detected: " + drift + "ms");
                    }

                    if (currentTrack.isPlaying != finalIsPlaying) {
                        if (finalIsPlaying) {
                            api.play();
                        } else {
                            api.pause();
                        }
                    }
                }
            });
        }
    }

    public ListeningParty getCurrentParty() {
        return currentParty;
    }

    public boolean isInParty() {
        return currentParty != null;
    }

    public boolean isPartyHost() {
        return isHost;
    }

    public static List<ListeningParty> getAllParties() {
        return new ArrayList<>(parties.values());
    }

    public static ListeningParty getPartyById(String partyId) {
        return parties.get(partyId);
    }

    // Call this regularly to sync with party
    public void tick() {
        if (currentParty != null && !isHost) {
            syncWithHost();
        } else if (currentParty != null && isHost) {
            // Update party state from current playback
            SpotifyAPI api = SpotifyMod.instance.getSpotifyAPI();
            api.getCurrentTrack().thenAccept(track -> {
                if (track != null) {
                    updatePartyState(null, track.progressMs, track.isPlaying);
                }
            });
        }
    }
    
    public void cleanup() {
        // Leave current party
        if (currentParty != null) {
            leaveCurrentParty();
        }
        
        // Clear all party data
        parties.clear();
    }
}
