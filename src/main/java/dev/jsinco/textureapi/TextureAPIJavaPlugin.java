package dev.jsinco.textureapi;

import org.bukkit.plugin.java.JavaPlugin;

public final class TextureAPIJavaPlugin extends JavaPlugin {

    private TextureAPI textureAPI;

    @Override
    public void onEnable() {
        textureAPI = new TextureAPI(this);
    }

    @Override
    public void onDisable() {
        textureAPI.shutdown();
    }
}
