package ru.heldyy.hubswap.config;

import com.google.gson.annotations.Expose;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

public class HotkeySlot {
    @Expose
    private int keyCode = -1;

    @Expose
    private String mode = "lite";

    @Expose
    private int serverNumber = 1;

    @Expose
    private boolean enabled = false;

    public HotkeySlot() {}

    public HotkeySlot(int keyCode, String mode, int serverNumber, boolean enabled) {
        this.keyCode = keyCode;
        this.mode = normalizeMode(mode);
        this.serverNumber = serverNumber;
        this.enabled = enabled;
    }

    public int getKeyCode() { return keyCode; }
    public String getMode() { return normalizeMode(mode); }
    public int getServerNumber() { return serverNumber; }
    public boolean isEnabled() { return enabled; }

    public void setKeyCode(int keyCode) { this.keyCode = keyCode; }
    public void setMode(String mode) { this.mode = normalizeMode(mode); }
    public void setServerNumber(int serverNumber) { this.serverNumber = Math.max(1, serverNumber); }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getKeyName() {
        if (keyCode < 0) return "---";
        String name = GLFW.glfwGetKeyName(keyCode, 0);
        return name != null ? name.toUpperCase(Locale.ROOT) : "Key" + keyCode;
    }

    public static String getModeDisplayName(String mode) {
        return switch (normalizeMode(mode)) {
            case "lite" -> "Lite";
            case "lite120" -> "Lite120";
            case "classic" -> "Classic";
            case "prime" -> "Prime";
            default -> mode == null ? "Lite" : mode;
        };
    }

    private static String normalizeMode(String mode) {
        if (mode == null) return "lite";
        return switch (mode.toLowerCase(Locale.ROOT)) {
            case "light" -> "lite";
            case "light120" -> "lite120";
            default -> mode.toLowerCase(Locale.ROOT);
        };
    }
}
