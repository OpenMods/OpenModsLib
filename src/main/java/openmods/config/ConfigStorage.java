package openmods.config;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class ConfigStorage {

	public static final ConfigStorage instance = new ConfigStorage();

	private Multimap<String, Configuration> configs = ArrayListMultimap.create();

	public void register(Configuration value) {
		ModContainer mod = Loader.instance().activeModContainer();
		Preconditions.checkNotNull(mod, "Can't register outside initialization");
		final String modId = mod.getModId();

		configs.put(modId, value);
	}

	public Collection<Configuration> getConfigs(String modId) {
		return configs.get(modId);
	}

	public void saveAll(String modId) {
		for (Configuration config : configs.get(modId))
			if (config.hasChanged()) config.save();
	}

	@SubscribeEvent
	public void onConfigChange(ConfigChangedEvent.PostConfigChangedEvent evt) {
		saveAll(evt.modID);
	}
}
