package com.spotifymod.handlers;

import com.spotifymod.api.SpotifyAPI;
import com.spotifymod.gui.GuiSpotifyControl;
import com.spotifymod.gui.SpotifyGuiHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import org.lwjgl.input.Keyboard;

public class KeybindHandler {
    private final SpotifyAPI api;
    
    private static final String CATEGORY = "Spotify Controls";
    
    private final KeyBinding openGui;
    private final KeyBinding playPause;
    private final KeyBinding nextTrack;
    private final KeyBinding prevTrack;

    public KeybindHandler(SpotifyAPI api) {
        this.api = api;
        
        // Register keybindings
        openGui = new KeyBinding("Open Spotify GUI", Keyboard.KEY_P, CATEGORY);
        playPause = new KeyBinding("Play/Pause", Keyboard.KEY_NONE, CATEGORY);
        nextTrack = new KeyBinding("Next Track", Keyboard.KEY_NONE, CATEGORY);
        prevTrack = new KeyBinding("Previous Track", Keyboard.KEY_NONE, CATEGORY);
        
        ClientRegistry.registerKeyBinding(openGui);
        ClientRegistry.registerKeyBinding(playPause);
        ClientRegistry.registerKeyBinding(nextTrack);
        ClientRegistry.registerKeyBinding(prevTrack);
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        if (openGui.isPressed()) {
            Minecraft.getMinecraft().displayGuiScreen(new GuiSpotifyControl());
        }
        
        if (!api.isAuthenticated()) {
            return;
        }
        
        if (playPause.isPressed()) {
            api.getCurrentTrack().thenAccept(track -> {
                if (track != null && track.isPlaying) {
                    api.pause();
                } else {
                    api.play();
                }
                // Force HUD update after play/pause
                SpotifyGuiHandler handler = SpotifyGuiHandler.getInstance();
                if (handler != null) {
                    handler.forceUpdate();
                }
            });
        }
        
        if (nextTrack.isPressed()) {
            api.next().thenAccept(success -> {
                if (success) {
                    // Schedule update on main thread after a delay
                    new Thread(() -> {
                        try {
                            Thread.sleep(500);
                            Minecraft.getMinecraft().addScheduledTask(() -> {
                                SpotifyGuiHandler handler = SpotifyGuiHandler.getInstance();
                                if (handler != null) {
                                    handler.forceUpdate();
                                }
                            });
                        } catch (InterruptedException e) {
                            // Ignore
                        }
                    }).start();
                }
            });
        }
        
        if (prevTrack.isPressed()) {
            api.previous().thenAccept(success -> {
                if (success) {
                    // Schedule update on main thread after a delay
                    new Thread(() -> {
                        try {
                            Thread.sleep(500);
                            Minecraft.getMinecraft().addScheduledTask(() -> {
                                SpotifyGuiHandler handler = SpotifyGuiHandler.getInstance();
                                if (handler != null) {
                                    handler.forceUpdate();
                                }
                            });
                        } catch (InterruptedException e) {
                            // Ignore
                        }
                    }).start();
                }
            });
        }
    }
}
