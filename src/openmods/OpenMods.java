package openmods;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.biome.SpawnListEntry;
import net.minecraftforge.common.Configuration;
import net.minecraftforge.common.MinecraftForge;
import openmods.config.CommandConfig;
import openmods.config.ConfigProcessing;
import openmods.entity.DelayedEntityLoadManager;
import openmods.fakeplayer.FakePlayerPool;
import openmods.integration.Integration;
import openmods.integration.modules.BuildCraftPipes;
import openmods.network.EventPacket;
import openmods.network.PacketHandler;
import openmods.network.events.TileEntityEventHandler;
import openmods.proxy.IOpenModsProxy;
import openmods.sync.SyncableManager;

import com.google.common.collect.Sets;

import cpw.mods.fml.common.*;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.event.*;
import cpw.mods.fml.common.network.NetworkMod;

@Mod(modid = "OpenMods", name = "OpenMods", version = "0.4", dependencies = "required-after:OpenModsCore")
@NetworkMod(serverSideRequired = true, clientSideRequired = false, channels = { PacketHandler.CHANNEL_SYNC, PacketHandler.CHANNEL_EVENTS }, packetHandler = PacketHandler.class)
public class OpenMods {

	@Instance(value = "OpenMods")
	public static OpenMods instance;

	@SidedProxy(clientSide = "openmods.proxy.OpenClientProxy", serverSide = "openmods.proxy.OpenServerProxy")
	public static IOpenModsProxy proxy;

	public static SyncableManager syncableManager;

	@EventHandler
	public void preInit(FMLPreInitializationEvent evt) {
		EventPacket.registerCorePackets();

		final File configFile = evt.getSuggestedConfigurationFile();
		Configuration config = new Configuration(configFile);
		ConfigProcessing.processAnnotations(configFile, "OpenMods", config, LibConfig.class);
		if (config.hasChanged()) config.save();
		
		MinecraftForge.EVENT_BUS.register(new TileEntityEventHandler());

		MinecraftForge.EVENT_BUS.register(DelayedEntityLoadManager.instance);

		MinecraftForge.EVENT_BUS.register(FakePlayerPool.instance);

		proxy.preInit();
	}

	@EventHandler
	public void init(FMLInitializationEvent evt) {
		syncableManager = new SyncableManager();
		proxy.init();
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent evt) {
		Integration.addModule(new BuildCraftPipes());
		Integration.loadModules();
		proxy.postInit();
		
		if (LibConfig.enableNaturalSpawnWhitelist) removeNaturalMobSpawns();
	}

	@EventHandler
	public void severStart(FMLServerStartingEvent evt) {
		evt.registerServerCommand(new CommandConfig("om_config_s", true));
	}

	private void removeNaturalMobSpawns() {

		Set<Class<?>> entityWhitelist = Sets.newIdentityHashSet();

		Set<String> unknownNames = Sets.newHashSet();
		for (String name : LibConfig.naturalSpawnWhitelist) {

			Class<?> cls = (Class<?>)EntityList.stringToClassMapping.get(name);
			if (cls != null) entityWhitelist.add(cls);
			else unknownNames.add(name);
		}

		for (Class<?> cls : (Set<Class<?>>)EntityList.classToStringMapping.keySet()) {
			if (unknownNames.isEmpty()) break;
			if (unknownNames.remove(cls.getName())) entityWhitelist.add(cls);
		}

		if (!unknownNames.isEmpty()) Log.warn("Can't identify mobs for whitelist: %s", unknownNames);

		for (BiomeGenBase biome : BiomeGenBase.biomeList) {
			if (biome == null) continue;

			for (EnumCreatureType type : EnumCreatureType.values()) {
				List<SpawnListEntry> spawnableEntries = (List<SpawnListEntry>)biome.getSpawnableList(type);
				if (spawnableEntries != null) {
					Iterator<SpawnListEntry> it = spawnableEntries.iterator();
					while (it.hasNext()) {
						SpawnListEntry entry = it.next();
						if (!entityWhitelist.contains(entry.entityClass)) {
							System.out.println("Removing " + entry.entityClass);
							it.remove();
						}
					}
				}
			}
		}
	}
}
