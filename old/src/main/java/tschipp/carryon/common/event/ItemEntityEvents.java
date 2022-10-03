package tschipp.carryon.common.event;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import tschipp.carryon.client.keybinds.CarryOnKeybinds;
import tschipp.carryon.common.config.Configs.Settings;
import tschipp.carryon.common.handler.ListHandler;
import tschipp.carryon.common.handler.PickupHandler;
import tschipp.carryon.common.handler.RegistrationHandler;
import tschipp.carryon.common.item.ItemCarryonEntity;
import tschipp.carryon.common.scripting.CarryOnOverride;
import tschipp.carryon.common.scripting.ScriptChecker;

public class ItemEntityEvents
{

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onBlockClick(PlayerInteractEvent.RightClickBlock event)
	{
		Player player = event.getEntity();
		ItemStack stack = player.getMainHandItem();
		if (!stack.isEmpty() && stack.getItem() == RegistrationHandler.itemEntity.get() && ItemCarryonEntity.hasEntityData(stack))
		{
			player.getPersistentData().remove("carrySlot");
			event.setUseBlock(Result.DENY);

			if (!player.level.isClientSide)
			{
				CarryOnOverride override = ScriptChecker.getOverride(player);
				if (override != null)
				{
					String command = override.getCommandPlace();

					if (command != null)
						player.getServer().getCommands().performCommand(player.getServer().createCommandSourceStack(), "/execute as " + player.getGameProfile().getName() + " run " + command);
				}
			}
		}

	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onItemDropped(EntityJoinLevelEvent event)
	{
		Entity e = event.getEntity();
		Level level = event.getLevel();
		if (e instanceof net.minecraft.world.entity.item.ItemEntity eitem)
		{
			ItemStack stack = eitem.getItem();
			Item item = stack.getItem();
			if (item == RegistrationHandler.itemEntity.get() && ItemCarryonEntity.hasEntityData(stack))
			{
				BlockPos pos = eitem.blockPosition();
				Entity entity = ItemCarryonEntity.getEntity(stack, level);
				entity.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
				level.addFreshEntity(entity);

				ItemCarryonEntity.clearEntityData(stack);
				eitem.setItem(ItemStack.EMPTY);
			}
		}
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onEntityRightClick(PlayerInteractEvent.EntityInteract event)
	{
		Player player = event.getEntity();

		if (player instanceof ServerPlayer)
		{
			ItemStack main = player.getMainHandItem();
			ItemStack off = player.getOffhandItem();
			Level level = event.getLevel();
			Entity entity = event.getTarget();
			BlockPos pos = entity.blockPosition();

			if (main.isEmpty() && off.isEmpty() && CarryOnKeybinds.isKeyPressed(player))
			{
				ItemStack stack = new ItemStack(RegistrationHandler.itemEntity.get());

				if (entity.invulnerableTime == 0)
				{
					if (entity instanceof Animal)
						((Animal) entity).dropLeash(true, true);

					if (PickupHandler.canPlayerPickUpEntity((ServerPlayer) player, entity) && ItemCarryonEntity.storeEntityData(entity, level, stack))
					{
						LazyOptional<IItemHandler> handler = entity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);

						handler.ifPresent(hand -> {
							for (int i = 0; i < hand.getSlots(); i++)
							{
								hand.extractItem(i, 64, false);
							}
						});

						CarryOnOverride override = ScriptChecker.inspectEntity(entity);
						int overrideHash = 0;
						if (override != null)
							overrideHash = override.hashCode();

						ItemEvents.sendPacket(player, player.getInventory().selected, overrideHash);

						if (entity instanceof LivingEntity)
							((LivingEntity) entity).setHealth(0);

						entity.ejectPassengers();
						entity.setPos(entity.getX(), 0, entity.getZ());
						entity.discard();
						player.setItemInHand(InteractionHand.MAIN_HAND, stack);
						event.setCanceled(true);
						event.setCancellationResult(InteractionResult.FAIL);
					}
				}

			}
			else if (!main.isEmpty() && main.getItem() == RegistrationHandler.itemEntity.get() && ItemCarryonEntity.hasEntityData(main) && !CarryOnKeybinds.isKeyPressed(player) && Settings.stackableEntities.get())
			{
				Entity entityHeld = ItemCarryonEntity.getEntity(main, level);

				if (entity.invulnerableTime == 0 && entityHeld instanceof LivingEntity)
				{

					if (!level.isClientSide && entityHeld.getUUID() != entity.getUUID() && entity.isAlive())
					{

						double sizeHeldEntity = entityHeld.getBbHeight() * entityHeld.getBbWidth();
						double distance = pos.distSqr(player.blockPosition());
						Entity lowestEntity = entity.getRootVehicle();
						int numPassengers = getAllPassengers(lowestEntity);
						if (numPassengers < Settings.maxEntityStackLimit.get() - 1)
						{
							Entity topEntity = getTopPassenger(lowestEntity);

							if (Settings.useWhitelistStacking.get() ? ListHandler.isStackingAllowed(topEntity) : !ListHandler.isStackingForbidden(topEntity))
							{
								double sizeEntity = topEntity.getBbHeight() * topEntity.getBbWidth();
								if (Settings.entitySizeMattersStacking.get() && sizeHeldEntity <= sizeEntity || !Settings.entitySizeMattersStacking.get())
								{
									if (topEntity instanceof Horse horse)
									{
										horse.setTamed(true);
									}

									if (distance < 6)
									{
										double tempX = entity.getX();
										double tempY = entity.getY();
										double tempZ = entity.getZ();
										entityHeld.setPos(tempX, tempY + 2.6, tempZ);
										level.addFreshEntity(entityHeld);
										entityHeld.startRiding(topEntity, false);
										entityHeld.teleportTo(tempX, tempY, tempZ);
									}
									else
									{
										entityHeld.setPos(entity.getX(), entity.getY(), entity.getZ());
										level.addFreshEntity(entityHeld);
										entityHeld.startRiding(topEntity, false);
									}

									ItemCarryonEntity.clearEntityData(main);
									player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
									ItemEvents.sendPacket(player, 9, 0);
									event.setCanceled(true);
									event.setCancellationResult(InteractionResult.FAIL);
									level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.HORSE_SADDLE, SoundSource.PLAYERS, 0.5F, 1.5F);
								}
								else
								{
									level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BASS, SoundSource.PLAYERS, 0.5F, 1.5F);
								}
							}
						}
						else
						{
							level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BASS, SoundSource.PLAYERS, 0.5F, 1.5F);
						}
					}

				}

			}
		}

	}

	public static int getAllPassengers(Entity entity)
	{
		int passengers = 0;
		while (entity.isVehicle())
		{
			List<Entity> pass = entity.getPassengers();
			if (!pass.isEmpty())
			{
				entity = pass.get(0);
				passengers++;
			}
		}

		return passengers;
	}

	public static Entity getTopPassenger(Entity entity)
	{
		Entity top = entity;
		while (entity.isVehicle())
		{
			List<Entity> pass = entity.getPassengers();
			if (!pass.isEmpty())
			{
				entity = pass.get(0);
				top = entity;
			}
		}

		return top;
	}

	@SubscribeEvent
	public void onLivingUpdate(LivingTickEvent event)
	{
		LivingEntity entity = event.getEntity();
		Level level = entity.level;
		ItemStack main = entity.getMainHandItem();
		if (!main.isEmpty() && main.getItem() == RegistrationHandler.itemEntity.get() && ItemCarryonEntity.hasEntityData(main))
		{
			BlockPos pos = entity.blockPosition();
			BlockPos below = pos.relative(Direction.DOWN);

			if (level.getBlockState(pos).getMaterial() == Material.WATER || level.getBlockState(below).getMaterial() == Material.WATER)
			{
				Entity contained = ItemCarryonEntity.getEntity(main, level);
				if (contained != null)
				{
					float height = contained.getBbWidth();
					float width = contained.getBbWidth();

					entity.push(0, -0.01 * height * width, 0);
				}
			}
		}
	}

}
