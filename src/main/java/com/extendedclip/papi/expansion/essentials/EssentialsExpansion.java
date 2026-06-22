/*
 *
 * Essentials-Expansion
 * Copyright (C) 2019 Ryan McCarthy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 */
package com.extendedclip.papi.expansion.essentials;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.Kit;
import com.earth2me.essentials.User;
import com.earth2me.essentials.utils.DateUtil;
import com.earth2me.essentials.utils.DescParseTickFormat;
import com.google.common.primitives.Ints;
import me.clip.placeholderapi.PlaceholderAPIPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.NotNull;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class EssentialsExpansion extends PlaceholderExpansion {

    private static final DecimalFormat COORDS_FORMAT = new DecimalFormat("#.###");
    private static final LegacyComponentSerializer LEGACY_AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final LegacyComponentSerializer LEGACY_SECTION = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN_TEXT = PlainTextComponentSerializer.plainText();
    private static final List<String> PLACEHOLDERS;

    static {
        PLACEHOLDERS = Stream.of(
                Stream.of(
                        "is_clearinventory_confirm",
                        "is_teleport_enabled",
                        "is_muted",
                        "muted",

                        "afk",
                        "afk_reason",
                        "afk_player_count",

                        "msg_ignore",
                        "fly",

                        "nickname",
                        "nickname_stripped",

                        "muted_time_remaining",
                        "geolocation",
                        "godmode",
                        "unique",

                        "homes_set",
                        "homes_max",

                        "jailed",
                        "jailed_time_remaining",

                        "pm_recipient",
                        "safe_online",
                        "tp_cooldown",

                        "world_date",
                        "world_time",
                        "world_time_24"
                ),

                Stream.of(
                        "last_use",
                        "is_available",
                        "time_until_available",
                        "has"
                ).map(placeholder -> "kit_" + placeholder + "_<kit>"),

                Stream.of("has_kit_<kit>"),

                Stream.of(
                        Stream.of(
                                "total",
                                "max",
                                "name_<index>",
                                "has_<name>"
                        ),
                        Stream.of(
                                "world",
                                "x",
                                "y",
                                "z"
                        ).map(placeholder -> placeholder + "_<name|index>")
                ).flatMap(placeholders -> placeholders).map(placeholder -> "home_" + placeholder)

        ).flatMap(placeholders -> placeholders)
                .map(placeholder -> "%essentials_" + placeholder + "%")
                .toList();
    }

    private Essentials essentials;

    @Override
    public boolean canRegister() {
        return getEssentials() != null;
    }

    @Override
    public boolean register() {
        essentials = getEssentials();
        return essentials != null && super.register();
    }

    @Override
    public @NotNull String getAuthor() {
        return "clip";
    }

    @Override
    public @NotNull String getIdentifier() {
        return "essentials";
    }

    @Override
    public @NotNull String getVersion() {
        String version = getClass().getPackage().getImplementationVersion();
        return version == null ? "2.0.0" : version;
    }

    @Override
    public @NotNull List<String> getPlaceholders() {
        return PLACEHOLDERS;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        Object output = request(player, params);
        if (output == null) {
            return null;
        }
        if (output instanceof Boolean bool) {
            return bool ? PlaceholderAPIPlugin.booleanTrue() : PlaceholderAPIPlugin.booleanFalse();
        }
        return output.toString();
    }

    private Essentials getEssentials() {
        final PluginManager pluginManager = Bukkit.getPluginManager();
        final Plugin plugin = pluginManager.getPlugin("Essentials-OG");
        final Plugin fallback = plugin != null ? plugin : pluginManager.getPlugin("Essentials");
        return fallback instanceof Essentials && fallback.isEnabled() ? (Essentials) fallback : null;
    }

    private Object request(OfflinePlayer player, String params) {
        final User user = essentials.getUser(player.getUniqueId());
        return switch (params) {
            case "is_clearinventory_confirm" -> user.isPromptingClearConfirm();
            case "is_teleport_enabled" -> user.isTeleportEnabled();
            case "is_muted", "muted" -> user.isMuted();

            case "afk" -> user.isAfk();
            case "afk_reason" -> user.getAfkMessage() == null ? "" : translateLegacyAmpersand(user.getAfkMessage());
            case "afk_player_count" -> String.valueOf(essentials.getUserMap().getAllUniqueUsers().stream()
                    .map(uuid -> essentials.getUser(uuid))
                    .filter(User::isAfk)
                    .count());

            case "msg_ignore" -> user.isIgnoreMsg();
            case "fly" -> user.getBase().getAllowFlight();

            case "nickname" -> user.getNickname() != null ? user.getNickname() : player.getName();
            case "nickname_stripped" -> stripLegacyFormatting(user.getNickname() != null ? user.getNickname() : player.getName());

            case "muted_time_remaining" -> user.isMuted() ? DateUtil.formatDateDiff(user.getMuteTimeout()) : "";
            case "geolocation" -> user.getGeoLocation() != null ? user.getGeoLocation() : "";
            case "godmode" -> user.isGodModeEnabled();
            case "unique" -> NumberFormat.getInstance().format(essentials.getUserMap().getUniqueUsers());

            case "homes_set" -> user.getHomes().isEmpty() ? "0" : String.valueOf(user.getHomes().size());
            case "homes_max" -> String.valueOf(essentials.getSettings().getHomeLimit(user));

            case "jailed" -> user.isJailed();
            case "jailed_time_remaining" -> user.isJailed() ? user.getFormattedJailTime() : "";

            case "pm_recipient" -> user.getReplyRecipient() != null ? user.getReplyRecipient().getName() : "";
            case "safe_online" -> String.valueOf(StreamSupport
                    .stream(essentials.getOnlineUsers().spliterator(), false)
                    .filter(onlineUser -> !onlineUser.isHidden())
                    .count());
            case "tp_cooldown" -> {
                final double cooldown = essentials.getSettings().getTeleportCooldown();
                final long now = System.currentTimeMillis();
                final long lastTeleport = user.getLastTeleportTimestamp();
                final long diff = TimeUnit.MILLISECONDS.toSeconds(now - lastTeleport);

                yield diff < cooldown ? String.valueOf((int) (cooldown - diff)) : "0";
            }

            case "world_date" -> DateFormat.getDateInstance(DateFormat.MEDIUM, essentials.getI18n().getCurrentLocale())
                    .format(DescParseTickFormat.ticksToDate(user.getWorld() == null ? 0 : user.getWorld().getFullTime()));
            case "world_time" -> DescParseTickFormat.format12(user.getWorld() == null ? 0 : user.getWorld().getTime());
            case "world_time_24" -> DescParseTickFormat.format24(user.getWorld() == null ? 0 : user.getWorld().getTime());
            default -> requestPrefixed(player, user, params);
        };
    }

    private Object requestPrefixed(OfflinePlayer player, User user, String params) {
        String[] args = params.split("_");
        String prefix = args[0];
        String remaining = params.substring(prefix.length() + (params.contains("_") ? 1 : 0));

        return switch (prefix) {
            case "kit" -> requestKit(player, user, remaining);
            case "has" -> remaining.startsWith("kit_") ? playerHasKit(player, remaining.substring("kit_".length())) : null;
            case "home" -> requestHome(user, remaining);
            default -> null;
        };
    }

    private Object requestKit(OfflinePlayer player, User user, String params) {
        if (params.startsWith("last_use_")) {
            String kitName = params.substring("last_use_".length()).toLowerCase(Locale.ROOT);

            try {
                Kit kit = new Kit(kitName, essentials);
                long time = user.getKitTimestamp(kit.getName());
                return time <= 0 ? "0" : PlaceholderAPIPlugin.getDateFormat().format(new Date(time));
            } catch (Exception e) {
                return "Invalid kit name";
            }
        }

        if (params.startsWith("is_available_")) {
            String kitName = params.substring("is_available_".length()).toLowerCase(Locale.ROOT);

            try {
                Kit kit = new Kit(kitName, essentials);
                try {
                    return kit.getNextUse(user) == 0;
                } catch (Exception e) {
                    return false;
                }
            } catch (Exception e) {
                return "Invalid kit name";
            }
        }

        if (params.startsWith("time_until_available_")) {
            String kitName = params.substring("time_until_available_".length()).toLowerCase(Locale.ROOT);
            boolean raw = false;

            if (kitName.startsWith("raw_")) {
                raw = true;
                kitName = kitName.substring("raw_".length());
            }
            if (kitName.isEmpty()) {
                return "Invalid kit name";
            }

            try {
                Kit kit = new Kit(kitName, essentials);
                try {
                    long time = kit.getNextUse(user);
                    if (time <= System.currentTimeMillis()) {
                        return raw ? "0" : DateUtil.formatDateDiff(System.currentTimeMillis());
                    }
                    return raw ? String.valueOf(Instant.now().until(Instant.ofEpochMilli(time), ChronoUnit.MILLIS)) : DateUtil.formatDateDiff(time);
                } catch (Exception e) {
                    return "-1";
                }
            } catch (Exception e) {
                return "Invalid kit name";
            }
        }

        if (params.startsWith("has_")) {
            return playerHasKit(player, params.substring("has_".length()));
        }

        return null;
    }

    private Object requestHome(User user, String params) {
        List<String> homes = user.getHomes();

        if (isLegacyHome(params)) {
            return requestLegacyHome(user, homes, params);
        }

        String[] args = params.split("_", 2);
        String type = args[0];
        String homeName = args.length == 2 ? args[1] : "";
        Integer homeNumber = Ints.tryParse(homeName);

        return switch (type) {
            case "total" -> String.valueOf(homes.size());
            case "max" -> String.valueOf(essentials.getSettings().getHomeLimit(user));
            case "has" -> user.hasHome(homeName);
            case "name" -> homeNumber == null || homeNumber < 0 || homeNumber >= homes.size() ? "" : homes.get(homeNumber);
            case "w", "world", "x", "y", "z" -> requestHomeLocation(user, homes, homeName, homeNumber, type);
            default -> null;
        };
    }

    private Object requestHomeLocation(User user, List<String> homes, String homeName, Integer homeNumber, String type) {
        if (homeNumber != null && (homeNumber < 0 || homeNumber >= homes.size())) {
            return "Invalid home";
        }

        Location home = user.getHome(homeNumber == null ? homeName : homes.get(homeNumber));
        if (home == null) {
            return "Invalid home";
        }

        Object output = switch (type) {
            case "w", "world" -> home.getWorld() == null ? "null" : home.getWorld().getName();
            case "x" -> home.getX();
            case "y" -> home.getY();
            case "z" -> home.getZ();
            default -> null;
        };
        return output instanceof Double coords ? COORDS_FORMAT.format(coords) : output;
    }

    private Object requestLegacyHome(User user, List<String> homes, String params) {
        String[] args = params.split("_", 2);
        Integer homeNumber = Ints.tryParse(args[0]);
        if (homeNumber == null) {
            return null;
        }

        homeNumber -= 1;
        if (homeNumber >= homes.size() || homeNumber < 0) {
            return "";
        }

        if (args.length == 1) {
            return homes.get(homeNumber);
        }

        try {
            Location home = user.getHome(homes.get(homeNumber));
            if (home == null) {
                return null;
            }

            return switch (args[1]) {
                case "w" -> home.getWorld() == null ? "null" : home.getWorld().getName();
                case "x" -> COORDS_FORMAT.format(home.getX());
                case "y" -> String.valueOf((int) home.getY());
                case "z" -> COORDS_FORMAT.format(home.getZ());
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private boolean playerHasKit(OfflinePlayer player, String kit) {
        Player onlinePlayer = player.getPlayer();
        return onlinePlayer != null && onlinePlayer.hasPermission("essentials.kits." + kit);
    }

    private boolean isLegacyHome(String params) {
        return params.matches("\\d+(_[wxyz])?");
    }

    private String translateLegacyAmpersand(String input) {
        return LEGACY_SECTION.serialize(LEGACY_AMPERSAND.deserialize(input));
    }

    private String stripLegacyFormatting(String input) {
        return PLAIN_TEXT.serialize(LEGACY_SECTION.deserialize(input));
    }
}
