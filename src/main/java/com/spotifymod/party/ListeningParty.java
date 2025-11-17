package com.spotifymod.party;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ListeningParty {
    private final String partyId;
    private final String hostProfileId;
    private String partyName;
    private String currentTrackUri;
    private int currentPositionMs;
    private boolean isPlaying;
    private long lastUpdateTime;
    private final List<PartyMember> members;
    private int maxMembers;

    public ListeningParty(String hostProfileId, String partyName) {
        this.partyId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.hostProfileId = hostProfileId;
        this.partyName = partyName;
        this.members = new ArrayList<>();
        this.maxMembers = 10;
        this.lastUpdateTime = System.currentTimeMillis();
        
        // Host auto joins
        addMember(new PartyMember(hostProfileId, partyName, true));
    }

    public String getPartyId() {
        return partyId;
    }

    public String getHostProfileId() {
        return hostProfileId;
    }

    public String getPartyName() {
        return partyName;
    }

    public void setPartyName(String partyName) {
        this.partyName = partyName;
    }

    public String getCurrentTrackUri() {
        return currentTrackUri;
    }

    public void setCurrentTrackUri(String currentTrackUri) {
        this.currentTrackUri = currentTrackUri;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public int getCurrentPositionMs() {
        return currentPositionMs;
    }

    public void setCurrentPositionMs(int currentPositionMs) {
        this.currentPositionMs = currentPositionMs;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    public List<PartyMember> getMembers() {
        return new ArrayList<>(members);
    }

    public int getMemberCount() {
        return members.size();
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    public boolean addMember(PartyMember member) {
        if (members.size() >= maxMembers) {
            return false;
        }
        
        for (PartyMember m : members) {
            if (m.getProfileId().equals(member.getProfileId())) {
                return false;
            }
        }
        
        members.add(member);
        return true;
    }

    public void removeMember(String profileId) {
        members.removeIf(m -> m.getProfileId().equals(profileId));
    }

    public boolean isMember(String profileId) {
        for (PartyMember m : members) {
            if (m.getProfileId().equals(profileId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isHost(String profileId) {
        return hostProfileId.equals(profileId);
    }

    public boolean isFull() {
        return members.size() >= maxMembers;
    }

    public static class PartyMember {
        private final String profileId;
        private final String displayName;
        private final boolean isHost;
        private long lastSeenTime;

        public PartyMember(String profileId, String displayName, boolean isHost) {
            this.profileId = profileId;
            this.displayName = displayName;
            this.isHost = isHost;
            this.lastSeenTime = System.currentTimeMillis();
        }

        public String getProfileId() {
            return profileId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isHost() {
            return isHost;
        }

        public long getLastSeenTime() {
            return lastSeenTime;
        }

        public void updateLastSeen() {
            this.lastSeenTime = System.currentTimeMillis();
        }
    }
}
