package com.spotifymod.network;

import com.spotifymod.SpotifyMod;
import com.spotifymod.api.SpotifyAPI;
import com.spotifymod.debug.LogBuffer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles network communication between mod users to share currently playing tracks.
 * Uses custom payload packets that work on any server without server-side mod.
 */
public class ModNetworkHandler {
    private static final String CHANNEL = "spotifymod";
    private static final byte PACKET_HANDSHAKE = 1;
    private static final byte PACKET_TRACK_UPDATE = 2;
    
    // Store track info for each player UUID
    private final Map<UUID, PlayerTrackInfo> playerTracks = new ConcurrentHashMap<>();
    private long lastBroadcast = 0;
    private static final long BROADCAST_INTERVAL = 5000; // 5 seconds
    
    public static class PlayerTrackInfo {
        public final String artist;
        public final String track;
        public final boolean isPlaying;
        public final long timestamp;
        
        public PlayerTrackInfo(String artist, String track, boolean isPlaying) {
            this.artist = artist;
            this.track = track;
            this.isPlaying = isPlaying;
            this.timestamp = System.currentTimeMillis();
        }
        
        public boolean isStale() {
            return System.currentTimeMillis() - timestamp > 15000; // 15 seconds
        }
        
        public String getCompactDisplay() {
            if (!isPlaying) return "[Paused]";
            
            // More compact format - shorten both if needed
            String displayArtist = artist.length() > 12 ? artist.substring(0, 10) + ".." : artist;
            String displayTrack = track.length() > 18 ? track.substring(0, 16) + ".." : track;
            
            // Use a compact separator
            return displayArtist + " - " + displayTrack;
        }
    }
    
    public ModNetworkHandler() {
        // For 1.8.9, we register via event bus only
        // Network channel registration will be handled differently
    }
    
    /**
     * Called when client connects to a server
     */
    @SubscribeEvent
    public void onClientConnectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        LogBuffer.get().info("Connected to server - sending mod handshake");
        sendHandshake();
    }
    
    /**
     * Called when client disconnects
     */
    @SubscribeEvent
    public void onClientDisconnection(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        LogBuffer.get().info("Disconnected - clearing player track cache");
        playerTracks.clear();
    }
    
    /**
     * Receive packets from other mod users
     */
    @SubscribeEvent
    public void onPacketReceived(FMLNetworkEvent.ClientCustomPacketEvent event) {
        FMLProxyPacket packet = event.packet;
        if (!packet.channel().equals(CHANNEL)) return;
        
        ByteBuf buf = packet.payload();
        try {
            byte packetType = buf.readByte();
            
            if (packetType == PACKET_HANDSHAKE) {
                handleHandshake(buf);
            } else if (packetType == PACKET_TRACK_UPDATE) {
                handleTrackUpdate(buf);
            }
        } catch (Exception e) {
            LogBuffer.get().error("Error handling packet: " + e.getMessage());
        }
    }
    
    private void handleHandshake(ByteBuf buf) {
        // Another mod user connected - send our current track
        LogBuffer.get().trace("Received handshake from mod user");
        broadcastCurrentTrack();
    }
    
    private void handleTrackUpdate(ByteBuf buf) {
        try {
            // Read UUID
            long mostSig = buf.readLong();
            long leastSig = buf.readLong();
            UUID playerUUID = new UUID(mostSig, leastSig);
            
            // Read track info
            boolean isPlaying = buf.readBoolean();
            String artist = readString(buf);
            String track = readString(buf);
            
            playerTracks.put(playerUUID, new PlayerTrackInfo(artist, track, isPlaying));
            LogBuffer.get().trace("Updated track for player: " + artist + " - " + track);
        } catch (Exception e) {
            LogBuffer.get().error("Error reading track update: " + e.getMessage());
        }
    }
    
    private void sendHandshake() {
        ByteBuf buf = Unpooled.buffer();
        buf.writeByte(PACKET_HANDSHAKE);
        sendPacket(buf);
    }
    
    public void broadcastCurrentTrack() {
        SpotifyAPI api = SpotifyMod.instance.getSpotifyAPI();
        if (!api.isAuthenticated()) return;
        
        api.getCurrentTrack().thenAccept(trackInfo -> {
            if (trackInfo == null) return;
            
            try {
                ByteBuf buf = Unpooled.buffer();
                buf.writeByte(PACKET_TRACK_UPDATE);
                
                // Write our UUID
                UUID uuid = Minecraft.getMinecraft().thePlayer.getUniqueID();
                buf.writeLong(uuid.getMostSignificantBits());
                buf.writeLong(uuid.getLeastSignificantBits());
                
                // Write track info
                buf.writeBoolean(trackInfo.isPlaying);
                writeString(buf, trackInfo.artist);
                writeString(buf, trackInfo.name);
                
                sendPacket(buf);
                LogBuffer.get().trace("Broadcasted track: " + trackInfo.artist + " - " + trackInfo.name);
            } catch (Exception e) {
                LogBuffer.get().error("Error broadcasting track: " + e.getMessage());
            }
        });
    }
    
    private void sendPacket(ByteBuf buf) {
        try {
            C17PacketCustomPayload packet = new C17PacketCustomPayload(CHANNEL, new PacketBuffer(buf));
            Minecraft.getMinecraft().getNetHandler().addToSendQueue(packet);
        } catch (Exception e) {
            LogBuffer.get().error("Error sending packet: " + e.getMessage());
        }
    }
    
    public void tick() {
        long now = System.currentTimeMillis();
        
        // Broadcast our track every 5 seconds
        if (now - lastBroadcast > BROADCAST_INTERVAL) {
            broadcastCurrentTrack();
            lastBroadcast = now;
        }
        
        // Clean up stale entries
        playerTracks.entrySet().removeIf(entry -> entry.getValue().isStale());
    }
    
    public void cleanup() {
        // Clear all player track data
        playerTracks.clear();
        LogBuffer.get().info("Network handler cleaned up - track cache cleared");
    }
    
    public PlayerTrackInfo getPlayerTrack(UUID playerUUID) {
        return playerTracks.get(playerUUID);
    }
    
    public Map<UUID, PlayerTrackInfo> getPlayerTracks() {
        return playerTracks;
    }
    
    private void writeString(ByteBuf buf, String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        buf.writeShort(bytes.length);
        buf.writeBytes(bytes);
    }
    
    private String readString(ByteBuf buf) {
        short length = buf.readShort();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
