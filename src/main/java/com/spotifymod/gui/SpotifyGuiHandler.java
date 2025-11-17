package com.spotifymod.gui;

import com.spotifymod.SpotifyMod;
import com.spotifymod.api.SpotifyAPI;
import com.spotifymod.config.SpotifyConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

public class SpotifyGuiHandler {
    private final SpotifyAPI api;
    private volatile SpotifyAPI.TrackInfo cachedTrack;
    private volatile SpotifyAPI.TrackInfo previousTrack;
    private int updateTimer = 0;
    private volatile boolean isUpdating = false;
    
    // Fade animation
    private float fadeAlpha = 1.0f;
    private boolean isFading = false;
    private long fadeStartTime = 0;
    private static final long FADE_DURATION = 300; // milliseconds
    
    private static boolean dragMode = false;
    private boolean isDragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private int hudWidth = 0;
    private int hudHeight = 30;
    
    private static SpotifyGuiHandler instance;

    public SpotifyGuiHandler(SpotifyAPI api) {
        this.api = api;
        instance = this;
    }
    
    public static SpotifyGuiHandler getInstance() {
        return instance;
    }
    
    /**
     * Force an immediate update of the HUD track info.
     * Call this when track changes via hotkeys to prevent flickering.
     */
    public void forceUpdate() {
        updateTimer = 0; // Reset timer to trigger update on next frame
        isUpdating = false; // Allow new update
    }

    public static void setDragMode(boolean enabled) {
        dragMode = enabled;
    }

    public static boolean isDragMode() {
        return dragMode;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        SpotifyConfig config = SpotifyMod.instance.getConfig();
        
        if (!config.isHudEnabled() || !api.isAuthenticated()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        
        // Update track info every 2 seconds
        updateTimer++;
        if ((updateTimer % 40 == 0 || cachedTrack == null) && !isUpdating) {
            isUpdating = true;
            api.getCurrentTrack().thenAccept(track -> {
                if (track != null) {
                    // Check if track changed
                    if (cachedTrack != null && !track.name.equals(cachedTrack.name)) {
                        // Track changed - start fade animation
                        previousTrack = cachedTrack;
                        isFading = true;
                        fadeStartTime = System.currentTimeMillis();
                    }
                    cachedTrack = track;
                }
                isUpdating = false;
            }).exceptionally(e -> {
                isUpdating = false;
                return null;
            });
        }
        
        // Update fade animation
        if (isFading) {
            long elapsed = System.currentTimeMillis() - fadeStartTime;
            if (elapsed < FADE_DURATION) {
                // Fade out then fade in
                float progress = (float) elapsed / FADE_DURATION;
                if (progress < 0.5f) {
                    // First half: fade out
                    fadeAlpha = 1.0f - (progress * 2.0f);
                } else {
                    // Second half: fade in
                    fadeAlpha = (progress - 0.5f) * 2.0f;
                }
            } else {
                // Animation complete
                isFading = false;
                fadeAlpha = 1.0f;
                previousTrack = null;
            }
        }

        // Always use the cached track - don't check if it's null during rendering
        SpotifyAPI.TrackInfo trackToRender = cachedTrack;
        if (trackToRender != null) {
            FontRenderer fr = mc.fontRendererObj;
            
            // Get HUD position from config
            int x = config.getHudX();
            int y = config.getHudY();
            
            // Calculate HUD dimensions
            hudWidth = Math.max(
                fr.getStringWidth("Now: " + trackToRender.name),
                fr.getStringWidth(trackToRender.artist)
            ) + 10;
            
            // Handle dragging in drag mode
            if (dragMode) {
                ScaledResolution sr = new ScaledResolution(mc);
                int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
                int mouseY = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;
                
                if (Mouse.isButtonDown(0)) {
                    if (!isDragging) {
                        // Check if mouse is over the HUD
                        if (mouseX >= x - 2 && mouseX <= x + hudWidth &&
                            mouseY >= y - 2 && mouseY <= y + hudHeight) {
                            isDragging = true;
                            dragOffsetX = mouseX - x;
                            dragOffsetY = mouseY - y;
                        }
                    }
                    
                    if (isDragging) {
                        x = mouseX - dragOffsetX;
                        y = mouseY - dragOffsetY;
                        
                        // Clamp to screen bounds
                        x = Math.max(0, Math.min(x, sr.getScaledWidth() - hudWidth));
                        y = Math.max(0, Math.min(y, sr.getScaledHeight() - hudHeight));
                        
                        config.setHudX(x);
                        config.setHudY(y);
                    }
                } else {
                    if (isDragging) {
                        // Save position when mouse is released
                        config.save();
                        isDragging = false;
                    }
                }
                
                // Draw border in drag mode
                drawRect(x - 3, y - 3, x + hudWidth + 1, y + hudHeight + 1, 0xFF00FF00);
            }
            
            // Draw a semi-transparent background (if enabled)
            if (config.isHudBackground()) {
                drawRect(x - 2, y - 2, x + hudWidth, y + hudHeight, 0x80000000);
            }
            
            // Get color scheme and current ticks for animation
            String schemeStr = config.getHudColorScheme();
            if (schemeStr == null || schemeStr.isEmpty()) {
                schemeStr = "DEFAULT";
            }
            HudColorScheme scheme = HudColorScheme.fromString(schemeStr);
            long ticks = mc.theWorld != null ? mc.theWorld.getTotalWorldTime() : System.currentTimeMillis() / 50;
            
            // Apply fade alpha to colors
            int alphaComponent = (int) (fadeAlpha * 255) << 24;
            
            // Draw track info with animated colors and fade
            int titleColor = (scheme.getColor(0, ticks) & 0x00FFFFFF) | alphaComponent;
            int artistColor = (scheme.getColor(1, ticks) & 0x00FFFFFF) | alphaComponent;
            int progressColor = (scheme.getColor(2, ticks) & 0x00FFFFFF) | alphaComponent;
            
            fr.drawStringWithShadow("Now: " + trackToRender.name, x, y, titleColor);
            fr.drawStringWithShadow(trackToRender.artist, x, y + 10, artistColor);
            fr.drawStringWithShadow(trackToRender.getProgressString(), x, y + 20, progressColor);
            
            // Draw drag mode indicator
            if (dragMode) {
                fr.drawStringWithShadow("[DRAG MODE]", x, y + hudHeight + 2, 0x55FF55);
            }
        }
    }

    private void drawRect(int left, int top, int right, int bottom, int color) {
        net.minecraft.client.gui.Gui.drawRect(left, top, right, bottom, color);
    }
}
