package openmods.utils;

import com.google.common.base.Preconditions;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class RegistrationContextBase<T extends IForgeRegistryEntry<T>> {

	protected final IForgeRegistry<T> registry;

	protected final String domain;

	private static String getCurrentMod() {
		ModContainer mc = Loader.instance().activeModContainer();
		Preconditions.checkState(mc != null, "This method can be only used during mod initialization");
		String prefix = mc.getModId().toLowerCase();
		return prefix;
	}

	public RegistrationContextBase(IForgeRegistry<T> registry, String domain) {
		this.registry = registry;
		this.domain = domain;
	}

	public RegistrationContextBase(IForgeRegistry<T> registry) {
		this(registry, getCurrentMod());
	}
}
