//? if fabric {
package dev.quarkos.villagerreroller.config;

import dev.quarkos.villagerreroller.VillagerReroller;
import dev.quarkos.villagerreroller.client.CycleAutomator;
import dev.quarkos.villagerreroller.price.LibrarianBookPrices;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.IntegerSliderEntry;
import me.shedaniel.clothconfig2.gui.entries.SelectionListEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Optional;

public final class AutomatorConfigScreen {
    private AutomatorConfigScreen() {
    }

    public static Screen create(Screen parent) {
        AutomatorConfig config = AutomatorConfig.get();
        config.ensureListedEnchantment();
        LibrarianBookPrices prices = LibrarianBookPrices.get();
        String[] enchantments = prices.enchantments().stream().map(LibrarianBookPrices.BookEnchantment::id).toArray(String[]::new);

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("text." + VillagerReroller.MOD_ID + ".config.title"))
                .setSavingRunnable(AutomatorConfig::save);
        ConfigEntryBuilder entries = builder.entryBuilder();
        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("text." + VillagerReroller.MOD_ID + ".config.category"));

        category.addEntry(entries.startTextDescription(Component.translatable("text." + VillagerReroller.MOD_ID + ".config.intro")).build());

        category.addEntry(entries.startBooleanToggle(Component.translatable("text." + VillagerReroller.MOD_ID + ".config.enabled"), config.enabled())
                .setDefaultValue(true)
                .setTooltip(Component.translatable("text." + VillagerReroller.MOD_ID + ".config.enabled.tooltip"))
                .setSaveConsumer(value -> {
                    config.setEnabled(value);
                    if (!value) {
                        CycleAutomator.stop(CycleAutomator.StopReason.DISABLED);
                    }
                })
                .build());

        SelectionListEntry<String> enchantmentEntry = entries.startSelector(
                        Component.translatable("text." + VillagerReroller.MOD_ID + ".config.enchantment"),
                        enchantments,
                        config.enchantment())
                .setDefaultValue("minecraft:mending")
                .setNameProvider(AutomatorConfigScreen::enchantmentName)
                .setSaveConsumer(config::setEnchantment)
                .build();

        IntegerSliderEntry levelEntry = entries.startIntSlider(
                        Component.translatable("text." + VillagerReroller.MOD_ID + ".config.level"),
                        clamp(config.level(), 1, 5),
                        1,
                        5)
                .setDefaultValue(1)
                .setTooltip(Component.translatable("text." + VillagerReroller.MOD_ID + ".config.level.tooltip"))
                .setErrorSupplier(level -> problem(prices.validateLevel(enchantmentEntry.getValue(), level)))
                .setSaveConsumer(config::setLevel)
                .build();

        category.addEntry(enchantmentEntry);
        category.addEntry(levelEntry);
        category.addEntry(entries.startIntSlider(
                        Component.translatable("text." + VillagerReroller.MOD_ID + ".config.max_price"),
                        clamp(config.maxPrice(), 5, 64),
                        5,
                        64)
                .setDefaultValue(10)
                .setTooltip(Component.translatable("text." + VillagerReroller.MOD_ID + ".config.max_price.tooltip"))
                .setErrorSupplier(price -> problem(prices.validate(enchantmentEntry.getValue(), levelEntry.getValue(), price)))
                .setSaveConsumer(config::setMaxPrice)
                .build());

        category.addEntry(entries.startIntField(
                        Component.translatable("text." + VillagerReroller.MOD_ID + ".config.max_attempts"),
                        config.maxAttempts())
                .setDefaultValue(500)
                .setMin(1)
                .setMax(10000)
                .setTooltip(Component.translatable("text." + VillagerReroller.MOD_ID + ".config.max_attempts.tooltip"))
                .setSaveConsumer(config::setMaxAttempts)
                .build());

        category.addEntry(entries.startBooleanToggle(Component.translatable("text." + VillagerReroller.MOD_ID + ".config.panic"), false)
                .setDefaultValue(false)
                .setTooltip(Component.translatable("text." + VillagerReroller.MOD_ID + ".config.panic.tooltip"))
                .setSaveConsumer(value -> {
                    if (value) {
                        CycleAutomator.stop(CycleAutomator.StopReason.PANIC);
                    }
                })
                .build());

        return builder.build();
    }

    private static Component enchantmentName(String id) {
        return Component.translatable("enchantment." + id.replace(':', '.'));
    }

    private static Optional<Component> problem(Optional<LibrarianBookPrices.Problem> problem) {
        return problem.map(value -> Component.translatable(
                "text." + VillagerReroller.MOD_ID + ".invalid." + value.kind().name().toLowerCase(),
                value.limit()));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
//?}
