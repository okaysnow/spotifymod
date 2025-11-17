package com.spotifymod;

import com.spotifymod.api.SpotifyAPI;
import com.spotifymod.debug.LogBuffer;
import com.spotifymod.commands.CommandParty;
import com.spotifymod.commands.CommandSpotifyHUD;
import com.spotifymod.config.SpotifyConfig;
import com.spotifymod.gui.SpotifyGuiHandler;
import com.spotifymod.handlers.KeybindHandler;
import com.spotifymod.handlers.NametagTrackRenderer;
import com.spotifymod.network.ModNetworkHandler;
import com.spotifymod.party.PartyManager;
import com.spotifymod.user.ProfileManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod(modid = SpotifyMod.MODID, version = SpotifyMod.VERSION, name = SpotifyMod.NAME)
public class SpotifyMod {
    public static final String MODID = "spotifymod";
    public static final String VERSION = "1.0";
    public static final String NAME = "Spotify Mod";

    @Mod.Instance(MODID)
    public static SpotifyMod instance;

    private SpotifyAPI spotifyAPI;
    private SpotifyConfig config;
    private KeybindHandler keybindHandler;
    private ProfileManager profileManager;
    private PartyManager partyManager;
    private ModNetworkHandler networkHandler;
    private int tickCounter = 0;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        config = new SpotifyConfig(event.getSuggestedConfigurationFile());
        profileManager = new ProfileManager(event.getModConfigurationDirectory());
        spotifyAPI = new SpotifyAPI(config, profileManager);
        partyManager = new PartyManager();
        LogBuffer.get().info("PreInit complete: config, profileManager, spotifyAPI, partyManager initialized");
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        keybindHandler = new KeybindHandler(spotifyAPI);
        networkHandler = new ModNetworkHandler();
        MinecraftForge.EVENT_BUS.register(keybindHandler);
        MinecraftForge.EVENT_BUS.register(new SpotifyGuiHandler(spotifyAPI));
        MinecraftForge.EVENT_BUS.register(new NametagTrackRenderer());
        MinecraftForge.EVENT_BUS.register(networkHandler);
        MinecraftForge.EVENT_BUS.register(this);
        LogBuffer.get().info("Init: handlers registered (including network and nametag renderer)");
    }

    @EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandSpotifyHUD());
        event.registerServerCommand(new CommandParty()); // Handles /spotify command
        LogBuffer.get().info("ServerStarting: /spotify and /spotifyhud commands registered");
    }
    
    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        LogBuffer.get().info("Server stopping - cleaning up resources");
        
        // Cleanup network handler
        if (networkHandler != null) {
            networkHandler.cleanup();
            LogBuffer.get().info("Network handler cleaned up");
        }
        
        // Cleanup party manager
        if (partyManager != null) {
            partyManager.cleanup();
            LogBuffer.get().info("Party manager cleaned up");
        }
        
        // Cleanup Spotify API (close HTTP connections)
        if (spotifyAPI != null) {
            spotifyAPI.cleanup();
            LogBuffer.get().info("Spotify API cleaned up");
        }
        
        LogBuffer.get().info("All resources cleaned up successfully");
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            // Sync party state every second (20 ticks)
            if (tickCounter % 20 == 0) {
                partyManager.tick();
                if (networkHandler != null) {
                    networkHandler.tick();
                }
                LogBuffer.get().trace("PartyManager and NetworkHandler tick executed");
            }
        }
    }

    public SpotifyAPI getSpotifyAPI() {
        return spotifyAPI;
    }

    public SpotifyConfig getConfig() {
        return config;
    }

    public ProfileManager getProfileManager() {
        return profileManager;
    }

    public PartyManager getPartyManager() {
        return partyManager;
    }

    public ModNetworkHandler getNetworkHandler() {
        return networkHandler;
    }
}
