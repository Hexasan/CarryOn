package tschipp.carryon.common.command;

import java.util.Collection;
import java.util.Collections;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import tschipp.carryon.CarryOn;
import tschipp.carryon.common.handler.CustomPickupOverrideHandler;
import tschipp.carryon.common.handler.ModelOverridesHandler;
import tschipp.carryon.common.handler.RegistrationHandler;
import tschipp.carryon.common.item.ItemCarryonBlock;
import tschipp.carryon.common.item.ItemCarryonEntity;
import tschipp.carryon.network.client.CarrySlotPacket;

public class CommandCarryOn
{
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
	{
		LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("carryon")

				.then(Commands.literal("debug").executes(cmd -> handleDebug(cmd.getSource())))

				.then(Commands.literal("clear").executes(cmd -> handleClear(cmd.getSource(), Collections.singleton(cmd.getSource().getPlayerOrException()))))

				.then(Commands.literal("clear").then(Commands.argument("target", EntityArgument.players()).requires(src -> src.hasPermission(2)).executes(cmd -> handleClear(cmd.getSource(), EntityArgument.getPlayers(cmd, "target")))))

		;

		dispatcher.register(builder);

	}

	private static int handleDebug(CommandSourceStack source)
	{
		try
		{
			if (source.getEntityOrException() != null)
			{
				ServerPlayer player = source.getPlayerOrException();

				ItemStack main = player.getMainHandItem();
				if (!main.isEmpty() && main.getItem() == RegistrationHandler.itemTile.get())
				{
					source.sendSuccess(Component.literal("Block: " + ItemCarryonBlock.getBlock(main)), true);
					source.sendSuccess(Component.literal("BlockState: " + ItemCarryonBlock.getBlockState(main)), true);
					source.sendSuccess(Component.literal("ItemStack: " + ItemCarryonBlock.getItemStack(main)), true);

					if (ModelOverridesHandler.hasCustomOverrideModel(ItemCarryonBlock.getBlockState(main), ItemCarryonBlock.getTileData(main)))
						source.sendSuccess(Component.literal("Override Model: " + ModelOverridesHandler.getOverrideObject(ItemCarryonBlock.getBlockState(main), ItemCarryonBlock.getTileData(main))), true);

					if (CustomPickupOverrideHandler.hasSpecialPickupConditions(ItemCarryonBlock.getBlockState(main)))
						source.sendSuccess(Component.literal("Custom Pickup Condition: " + CustomPickupOverrideHandler.getPickupCondition(ItemCarryonBlock.getBlockState(main))), true);

					CarryOn.LOGGER.info("Block: " + ItemCarryonBlock.getBlock(main));
					CarryOn.LOGGER.info("BlockState: " + ItemCarryonBlock.getBlockState(main));
					CarryOn.LOGGER.info("ItemStack: " + ItemCarryonBlock.getItemStack(main));

					if (ModelOverridesHandler.hasCustomOverrideModel(ItemCarryonBlock.getBlockState(main), ItemCarryonBlock.getTileData(main)))
						CarryOn.LOGGER.info("Override Model: " + ModelOverridesHandler.getOverrideObject(ItemCarryonBlock.getBlockState(main), ItemCarryonBlock.getTileData(main)));

					if (CustomPickupOverrideHandler.hasSpecialPickupConditions(ItemCarryonBlock.getBlockState(main)))
						CarryOn.LOGGER.info("Custom Pickup Condition: " + CustomPickupOverrideHandler.getPickupCondition(ItemCarryonBlock.getBlockState(main)));

					return 1;
				}
				else if (!main.isEmpty() && main.getItem() == RegistrationHandler.itemEntity.get())
				{
					source.sendSuccess(Component.literal("Entity: " + ItemCarryonEntity.getEntity(main, player.level)), true);
					source.sendSuccess(Component.literal("Entity Name: " + ItemCarryonEntity.getEntityName(main)), true);

					if (CustomPickupOverrideHandler.hasSpecialPickupConditions(ItemCarryonEntity.getEntity(main, player.level)))
						source.sendSuccess(Component.literal("Custom Pickup Condition: " + CustomPickupOverrideHandler.getPickupCondition(ItemCarryonEntity.getEntity(main, player.level))), true);

					CarryOn.LOGGER.info("Entity: " + ItemCarryonEntity.getEntity(main, player.level));
					CarryOn.LOGGER.info("Entity Name: " + ItemCarryonEntity.getEntityName(main));

					if (CustomPickupOverrideHandler.hasSpecialPickupConditions(ItemCarryonEntity.getEntity(main, player.level)))
						CarryOn.LOGGER.info("Custom Pickup Condition: " + CustomPickupOverrideHandler.getPickupCondition(ItemCarryonEntity.getEntity(main, player.level)));

					return 1;
				}
			}

		}
		catch (CommandSyntaxException e)
		{
		}

		return 0;
	}

	private static int handleClear(CommandSourceStack source, Collection<ServerPlayer> players)
	{
		for (ServerPlayer player : players)
		{
			int cleared = 0;
			cleared += player.getInventory().clearOrCountMatchingItems(stack -> !stack.isEmpty() && stack.getItem() == RegistrationHandler.itemTile.get(), 64, player.inventoryMenu.getCraftSlots()); // TODO
			cleared += player.getInventory().clearOrCountMatchingItems(stack -> !stack.isEmpty() && stack.getItem() == RegistrationHandler.itemEntity.get(), 64, player.inventoryMenu.getCraftSlots());

			CarryOn.network.send(PacketDistributor.PLAYER.with(() -> player), new CarrySlotPacket(9, player.getId()));

			if (cleared != 1)
				source.sendSuccess(Component.literal("Cleared " + cleared + " Items!"), true);
			else
				source.sendSuccess(Component.literal("Cleared " + cleared + " Item!"), true);

			return 1;
		}

		return 0;
	}
}
