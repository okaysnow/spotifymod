package com.spotifymod.gui;

public enum HudColorScheme {
    DEFAULT("Default", 0xFFFFFF, 0xAAAAAA, 0x888888),
    CHROMA("Chroma", -1, -1, -1), // Special case for animated rainbow
    OCEAN("Ocean", 0x00CED1, 0x4682B4, 0x191970),
    SUNSET("Sunset", 0xFF6B6B, 0xFF8E53, 0xFEC84E),
    FOREST("Forest", 0x90EE90, 0x32CD32, 0x228B22),
    PURPLE("Purple", 0xDA70D6, 0x9370DB, 0x4B0082),
    FIRE("Fire", 0xFF4500, 0xFF6347, 0xDC143C),
    ICE("Ice", 0xE0FFFF, 0x87CEEB, 0x4682B4),
    GOLD("Gold", 0xFFD700, 0xFFA500, 0xDAA520),
    PINK("Pink", 0xFFB6C1, 0xFF69B4, 0xFF1493);

    private final String displayName;
    private final int color1;
    private final int color2;
    private final int color3;

    HudColorScheme(String displayName, int color1, int color2, int color3) {
        this.displayName = displayName;
        this.color1 = color1;
        this.color2 = color2;
        this.color3 = color3;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Get interpolated color for animated gradient effect
     * @param line Which line (0, 1, 2) to get color for
     * @param ticks Current game ticks for animation
     * @return RGB color
     */
    public int getColor(int line, long ticks) {
        if (this == CHROMA) {
            // Fast flowing rainbow effect - synchronized with offset per line
            float hue = ((ticks * 3 + line * 30) % 360) / 360.0f;
            return hsvToRgb(hue, 0.9f, 1.0f);
        }

        // Single synchronized wave for all lines
        float mainWave = (float) Math.sin(ticks * 0.08);
        float progress = (mainWave + 1.0f) / 2.0f; // Normalize to 0-1
        
        // Add a secondary slower wave for more complex movement
        float slowWave = (float) Math.sin(ticks * 0.03);
        float blend = (slowWave + 1.0f) / 4.0f + 0.25f; // 0.25-0.75 range
        
        int baseColor;
        int midColor;
        int targetColor;
        
        // Each line shows a different color from the same position in the flow
        // Creating a harmonious gradient effect where all lines move together
        switch (line) {
            case 0: // Title line - shows current position
                baseColor = color1;
                midColor = color2;
                targetColor = color3;
                break;
            case 1: // Artist line - shows position +120 degrees ahead
                baseColor = color2;
                midColor = color3;
                targetColor = color1;
                break;
            case 2: // Progress line - shows position +240 degrees ahead  
                baseColor = color3;
                midColor = color1;
                targetColor = color2;
                break;
            default:
                return color1;
        }

        // All lines use the same progress value for synchronized movement
        // Blend between three colors for smooth flowing effect
        if (progress < 0.5f) {
            // First half: blend from base to mid
            return interpolateColor(baseColor, midColor, progress * 2.0f * blend);
        } else {
            // Second half: blend from mid to target
            return interpolateColor(midColor, targetColor, (progress - 0.5f) * 2.0f * blend);
        }
    }

    private static int interpolateColor(int color1, int color2, float ratio) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int r = (int) (r1 + (r2 - r1) * ratio);
        int g = (int) (g1 + (g2 - g1) * ratio);
        int b = (int) (b1 + (b2 - b1) * ratio);

        return (r << 16) | (g << 8) | b;
    }

    private static int hsvToRgb(float h, float s, float v) {
        int rgb = java.awt.Color.HSBtoRGB(h, s, v);
        return rgb & 0x00FFFFFF; // Remove alpha channel
    }

    public static HudColorScheme fromString(String name) {
        for (HudColorScheme scheme : values()) {
            if (scheme.name().equalsIgnoreCase(name)) {
                return scheme;
            }
        }
        return DEFAULT;
    }

    public HudColorScheme next() {
        HudColorScheme[] values = values();
        int nextIndex = (this.ordinal() + 1) % values.length;
        return values[nextIndex];
    }

    public HudColorScheme previous() {
        HudColorScheme[] values = values();
        int prevIndex = (this.ordinal() - 1 + values.length) % values.length;
        return values[prevIndex];
    }
}
