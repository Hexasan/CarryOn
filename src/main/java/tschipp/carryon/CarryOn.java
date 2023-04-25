package tschipp.carryon;

import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import tschipp.carryon.common.config.Configs;
import tschipp.carryon.common.handler.RegistrationHandler;
import tschipp.carryon.network.client.CarrySlotPacket;
import tschipp.carryon.network.client.ScriptReloadPacket;
import tschipp.carryon.network.server.SyncKeybindPacket;

import java.io.File;
import java.util.Optional;

@Mod(CarryOn.MODID)
@EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CarryOn
{

	public static final String MODID = "carryon";
	public static final Logger LOGGER = LogManager.getFormatterLogger("CarryOn");
	public static final String DEPENDENCIES = "required-after:forge@[13.20.1.2386,);after:gamestages;";
	public static final String CERTIFICATE_FINGERPRINT = "55e88f24d04398481ae6f1ce76f65fd776f14227";
	public static File CONFIGURATION_FILE;

	public static boolean FINGERPRINT_VIOLATED = false;

	public static SimpleChannel network;
	public static IModInfo info;

	public CarryOn()
	{
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);

		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Configs.CLIENT_CONFIG);
		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Configs.SERVER_CONFIG);

		info = ModLoadingContext.get().getActiveContainer().getModInfo();
	}

	private void setup(final FMLCommonSetupEvent event)
	{
		String version = info.getVersion().toString();
		// PreInitevent.
		CarryOn.network = NetworkRegistry.newSimpleChannel(new ResourceLocation(CarryOn.MODID, "carryonpackets"), () -> version, version::equals, version::equals);

		// CLIENT PACKETS
		CarryOn.network.registerMessage(0, CarrySlotPacket.class, CarrySlotPacket::toBytes, CarrySlotPacket::new, CarrySlotPacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
		CarryOn.network.registerMessage(1, ScriptReloadPacket.class, ScriptReloadPacket::toBytes, ScriptReloadPacket::new, ScriptReloadPacket::handle,  Optional.of(NetworkDirection.PLAY_TO_CLIENT));

		// SERVER PACKETS
		CarryOn.network.registerMessage(2, SyncKeybindPacket.class, SyncKeybindPacket::toBytes, SyncKeybindPacket::new, SyncKeybindPacket::handle,  Optional.of(NetworkDirection.PLAY_TO_SERVER));

		RegistrationHandler.regCommonEvents();
		RegistrationHandler.regCaps();
		RegistrationHandler.regOverrideList();
	}

	@SubscribeEvent
	public static void onRegistry(RegistryEvent.Register<Item> event)
	{
		RegistrationHandler.regItems();

		event.getRegistry().register(RegistrationHandler.itemEntity);
		event.getRegistry().register(RegistrationHandler.itemTile);
	}
}