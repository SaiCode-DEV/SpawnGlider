package de.kokoio01.spawnglider.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import de.kokoio01.spawnglider.config.SpawnElytraConfig;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class BoostCommand {
    private static SpawnElytraConfig config;

    public static void register(SpawnElytraConfig cfg) {
        config = cfg;
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("spawnglider")
                    .then(literal("boost")
                            .then(literal("max")
                                    .then(argument("count", IntegerArgumentType.integer(0, 100))
                                            .requires(source -> source.hasPermissionLevel(2))
                                            .executes(BoostCommand::setMaxBoosts)))
                            .then(literal("strength")
                                    .then(argument("value", DoubleArgumentType.doubleArg(0.1, 10.0))
                                            .requires(source -> source.hasPermissionLevel(2))
                                            .executes(BoostCommand::setBoostStrength)))
                            .then(literal("cooldown")
                                    .then(argument("ticks", IntegerArgumentType.integer(0, 100))
                                            .requires(source -> source.hasPermissionLevel(2))
                                            .executes(BoostCommand::setBoostCooldown)))
                            .then(literal("info")
                                    .executes(BoostCommand::showInfo))));
        });
    }

    private static int setMaxBoosts(CommandContext<ServerCommandSource> ctx) {
        int count = IntegerArgumentType.getInteger(ctx, "count");
        config.maxBoosts = count;
        config.save();

        ctx.getSource().sendFeedback(() -> Text.literal("Max boosts set to " + count)
                .formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setBoostStrength(CommandContext<ServerCommandSource> ctx) {
        double value = DoubleArgumentType.getDouble(ctx, "value");
        config.boostStrength = value;
        config.save();

        ctx.getSource().sendFeedback(() -> Text.literal("Boost strength set to " + value)
                .formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setBoostCooldown(CommandContext<ServerCommandSource> ctx) {
        int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
        config.boostCooldownTicks = ticks;
        config.save();

        ctx.getSource().sendFeedback(() -> Text.literal("Boost cooldown set to " + ticks + " ticks")
                .formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int showInfo(CommandContext<ServerCommandSource> ctx) {
        ctx.getSource().sendFeedback(() -> Text.literal("=== Boost Configuration ===")
                .formatted(Formatting.GOLD), false);
        ctx.getSource().sendFeedback(() -> Text.literal("Max Boosts: " + config.maxBoosts)
                .formatted(Formatting.YELLOW), false);
        ctx.getSource().sendFeedback(() -> Text.literal("Boost Strength: " + config.boostStrength)
                .formatted(Formatting.YELLOW), false);
        ctx.getSource().sendFeedback(() -> Text.literal("Cooldown: " + config.boostCooldownTicks + " ticks")
                .formatted(Formatting.YELLOW), false);
        return 1;
    }
}
