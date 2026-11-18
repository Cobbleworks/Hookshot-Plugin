package com.hookshot.harness;

import com.hookshot.config.HookshotSettings;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Creates and identifies the grapple harness and owns its crafting recipe.
 * Harnesses are recognised by a persistent data tag, not by name or material.
 */
public final class HarnessItem {

    private final NamespacedKey itemKey;
    private final NamespacedKey recipeKey;
    private final Logger logger;
    private HookshotSettings settings;
    private boolean recipeRegistered;

    public HarnessItem(Plugin plugin, HookshotSettings settings) {
        this.itemKey = new NamespacedKey(plugin, "grapple_harness");
        this.recipeKey = new NamespacedKey(plugin, "grapple_harness");
        this.logger = plugin.getLogger();
        this.settings = settings;
    }

    public NamespacedKey recipeKey() {
        return recipeKey;
    }

    public boolean isRecipeRegistered() {
        return recipeRegistered;
    }

    public ItemStack create() {
        ItemStack item = new ItemStack(settings.harnessMaterial());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text(settings.harnessName(), NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                plain("Wear as leggings to grapple.", NamedTextColor.GRAY),
                Component.empty(),
                control("Right-click with sword", "fire a hook"),
                control("Hold Sneak", "reel in (uses gas)"),
                control("Jump in mid-air", "let go / gas burst"),
                control("W A S D in mid-air", "steer your swing")));
        if (meta instanceof LeatherArmorMeta leather) {
            leather.setColor(settings.harnessColor());
            meta.addItemFlags(ItemFlag.HIDE_DYE);
        }
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isHarness(ItemStack item) {
        return item != null && !item.isEmpty() && item.getPersistentDataContainer().has(itemKey);
    }

    public boolean isWearing(Player player) {
        return isHarness(player.getInventory().getLeggings());
    }

    /** Applies new settings and (re)registers the crafting recipe. */
    public void apply(HookshotSettings newSettings) {
        this.settings = newSettings;
        unregisterRecipe();
        if (!newSettings.recipeEnabled()) {
            return;
        }
        ShapedRecipe recipe = buildRecipe(newSettings);
        if (recipe == null) {
            return;
        }
        recipeRegistered = Bukkit.addRecipe(recipe, true);
        if (!recipeRegistered) {
            logger.warning("Could not register the harness recipe.");
        }
    }

    public void unregisterRecipe() {
        if (recipeRegistered) {
            Bukkit.removeRecipe(recipeKey, true);
            recipeRegistered = false;
        }
    }

    private ShapedRecipe buildRecipe(HookshotSettings s) {
        List<String> shape = s.recipeShape();
        if (shape.isEmpty() || shape.size() > 3 || shape.stream().anyMatch(row -> row.isEmpty() || row.length() > 3)) {
            logger.warning("recipe.shape must have 1-3 rows of 1-3 characters; harness recipe disabled.");
            return null;
        }
        Set<Character> used = new HashSet<>();
        shape.forEach(row -> row.chars().filter(c -> c != ' ').forEach(c -> used.add((char) c)));
        if (used.isEmpty()) {
            logger.warning("recipe.shape is empty; harness recipe disabled.");
            return null;
        }
        for (char c : used) {
            if (!s.recipeIngredients().containsKey(c)) {
                logger.warning("recipe.shape uses '" + c + "' but it has no valid ingredient; harness recipe disabled.");
                return null;
            }
        }
        ShapedRecipe recipe = new ShapedRecipe(recipeKey, create());
        recipe.shape(shape.toArray(String[]::new));
        for (char c : used) {
            Material material = s.recipeIngredients().get(c);
            recipe.setIngredient(c, material);
        }
        return recipe;
    }

    private static Component plain(String text, NamedTextColor color) {
        return Component.text(text, color).decoration(TextDecoration.ITALIC, false);
    }

    private static Component control(String key, String action) {
        return Component.text()
                .append(Component.text(key, NamedTextColor.YELLOW))
                .append(Component.text(" - " + action, NamedTextColor.GRAY))
                .decoration(TextDecoration.ITALIC, false)
                .build();
    }
}
