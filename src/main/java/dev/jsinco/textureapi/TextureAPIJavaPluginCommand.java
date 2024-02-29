package dev.jsinco.textureapi;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import dev.jsinco.textureapi.storage.CachedTexture;
import dev.jsinco.textureapi.storage.Database;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class TextureAPIJavaPluginCommand implements TabExecutor {

    private final Database sql;

    public TextureAPIJavaPluginCommand(Database sql) {
        this.sql = sql;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length < 1) {
            sender.sendMessage("""
                    §6TextureAPI §7v§e%s
                    §7Developed by §eJsinco
                    """.formatted(TextureAPI.VERSION));
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "update-all" -> new BukkitRunnable() {
                @Override
                public void run() {
                    sender.sendMessage("§6Running async task to update all textures in database for online players.");
                    TextureAPI.log("Running async task to update all textures in database for online players.");
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        UUID uuid = player.getUniqueId();
                        PlayerProfile profile = player.getPlayerProfile();
                        String base64 = null;
                        for (ProfileProperty property : profile.getProperties()) {
                            base64 = property.getValue();
                        }

                        if (base64 != null) {
                            TextureAPI.log("Updating texture for %s...".formatted(player.getName()));
                            sql.saveTexture(uuid, base64, true);
                        }
                    }
                }
            }.runTaskAsynchronously(TextureAPI.getPlugin());

            case "show-cache" -> {
                sender.sendMessage("§6Cached Textures:");
                for (CachedTexture cachedTexture : sql.getCache()) {
                    TextComponent textComponent = new TextComponent("§e" + cachedTexture.getUuid() + " §7- §dBase64(clickToCopy)" + " §akeepAlive: " + cachedTexture.keepAlive());
                    textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.LIGHT_PURPLE + cachedTexture.getBase64())));
                    textComponent.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, cachedTexture.getBase64()));

                    sender.sendMessage(textComponent);
                }
            }

            case "verbose" -> {
                TextureAPI.setVerbose(!TextureAPI.isVerbose());
                sender.sendMessage("§6Verbose mode is now %s".formatted(TextureAPI.isVerbose() ? "§aenabled" : "§cdisabled"));
            }

            case "download" -> {
                sender.sendMessage("§6Attempting to download texture from Mojang...");
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (args.length < 2) {
                            sender.sendMessage("§cUsage: /textureapi download <username>");
                            return;
                        }

                        try {
                            UUID.fromString(args[1]);
                            TextureAPI.getBase64ThruAPI(args[1]);
                        } catch (IOException | IllegalArgumentException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }.runTaskAsynchronously(TextureAPI.getPlugin());
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("show-cache", "update-all", "verbose", "download");
        }
        return null;
    }
}
