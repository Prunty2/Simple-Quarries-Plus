package com.simplequarries.loot;

import com.simplequarries.SimpleQuarries;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import java.util.Map;

public final class QuarryLootInjectors {
    private static final Map<Identifier, Float> TARGET_CHESTS = Map.ofEntries(
            Map.entry(Identifier.withDefaultNamespace("chests/ruined_portal"), 0.03f),
            Map.entry(Identifier.withDefaultNamespace("chests/abandoned_mineshaft"), 0.10f),
            Map.entry(Identifier.withDefaultNamespace("chests/nether_bridge"), 0.10f),
            Map.entry(Identifier.withDefaultNamespace("chests/bastion_other"), 0.30f),
            Map.entry(Identifier.withDefaultNamespace("chests/bastion_treasure"), 0.30f),
            Map.entry(Identifier.withDefaultNamespace("chests/bastion_bridge"), 0.30f),
            Map.entry(Identifier.withDefaultNamespace("chests/bastion_hoglin_stable"), 0.30f),
            Map.entry(Identifier.withDefaultNamespace("chests/stronghold_corridor"), 0.20f),
            Map.entry(Identifier.withDefaultNamespace("chests/stronghold_crossing"), 0.20f),
            Map.entry(Identifier.withDefaultNamespace("chests/stronghold_library"), 0.20f),
            Map.entry(Identifier.withDefaultNamespace("chests/end_city_treasure"), 0.08f),
            Map.entry(Identifier.withDefaultNamespace("chests/simple_dungeon"), 0.03f),
            Map.entry(Identifier.withDefaultNamespace("chests/desert_pyramid"), 0.05f)
    );

    private QuarryLootInjectors() {}

    public static void register() {
        LootTableEvents.MODIFY.register((ResourceKey<LootTable> key, LootTable.Builder tableBuilder, LootTableSource source, net.minecraft.core.HolderLookup.Provider registries) -> {
            if (!source.isBuiltin()) {
                return;
            }

            Float chance = TARGET_CHESTS.get(key.identifier());
            if (chance == null) {
                return;
            }

            addTemplateEntry(tableBuilder, chance);
        });
    }

    private static void addTemplateEntry(LootTable.Builder tableBuilder, float chance) {
        LootPool.Builder pool = LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .when(LootItemRandomChanceCondition.randomChance(chance))
                .add(LootItem.lootTableItem(SimpleQuarries.QUARRY_UPGRADE_TEMPLATE));
        tableBuilder.withPool(pool);
    }
}
