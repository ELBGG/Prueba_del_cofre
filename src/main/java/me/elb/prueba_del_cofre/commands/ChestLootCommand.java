package me.elb.prueba_del_cofre.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import me.elb.prueba_del_cofre.Prueba_del_cofre;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.CommandSourceStack;

public class ChestLootCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("chestloot")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("reload")
                                .executes(ChestLootCommand::reload)
                        )
        );
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        try {
            Prueba_del_cofre.getConfigLoader().loadConfig();
            context.getSource().sendSuccess(
                    Component.literal("§a[ChestLoot] Configuration reloaded successfully!"),
                    false
            );
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(
                    Component.literal("§c[ChestLoot] Failed to reload configuration: " + e.getMessage())
            );
            Prueba_del_cofre.LOGGER.error("Failed to reload configuration", e);
            return 0;
        }
    }
}
