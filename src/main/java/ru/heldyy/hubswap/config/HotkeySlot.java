package ru.heldyy.hubswap.config;

import com.google.gson.annotations.Expose;

public class HotkeySlot {

    
    @Expose
    private int keyCode = -1;

    
    @Expose
    private String mode = "light";

    
    @Expose
    private int serverNumber = 1;

    @Expose
    private boolean enabled = false;

    public HotkeySlot() {}

    public HotkeySlot(int keyCode, String mode, int serverNumber, boolean enabled) {
        this.keyCode = keyCode;
        this.mode = mode;
        this.serverNumber = serverNumber;
        this.enabled = enabled;
    }

    public int getKeyCode() { return keyCode; }
    public String getMode() { return mode; }
    public int getServerNumber() { return serverNumber; }
    public boolean isEnabled() { return enabled; }

    public void setKeyCode(int keyCode) { this.keyCode = keyCode; }
    public void setMode(String mode) { this.mode = mode; }
    public void setServerNumber(int serverNumber) { this.serverNumber = serverNumber; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    
    public String getKeyName() {
        if (keyCode < 0) return "---";
        return org.lwjgl.glfw.GLFW.glfwGetKeyName(keyCode, 0) != null
                ? org.lwjgl.glfw.GLFW.glfwGetKeyName(keyCode, 0).toUpperCase()
                : "Key" + keyCode;
    }

    
    public String getCommandDisplay(ModConfig config) {
        String cmd = switch (mode) {
            case "classic" -> config.getClassicCommand();
            case "light120" -> config.getLight120Command();
            case "prime" -> config.getPrimeCommand();
            default -> config.getLightCommand();
        };
        return cmd + " " + serverNumber;
    }
}
