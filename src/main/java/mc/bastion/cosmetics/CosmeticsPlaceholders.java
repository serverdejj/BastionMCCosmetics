package mc.bastion.cosmetics;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Lee el saldo existente, sin duplicar ni modificar los datos de gemas. */
public final class CosmeticsPlaceholders extends PlaceholderExpansion {
    private final BastionMCCosmetics plugin;

    public CosmeticsPlaceholders(BastionMCCosmetics plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "bastioncosmetics"; }
    @Override public @NotNull String getAuthor() { return "BASTION MC"; }
    @Override public @NotNull String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override public String onRequest(OfflinePlayer offline, @NotNull String params) {
        if (!params.equalsIgnoreCase("gemas") && !params.equalsIgnoreCase("gems")) return null;
        if (offline == null) return "0";
        Player online = offline.getPlayer();
        if (online != null) return Integer.toString(plugin.balance(online));
        return Integer.toString(plugin.offlineBalance(offline));
    }
}
