package openmods.utils;

import com.google.common.base.Preconditions;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class RegistrationContextBase<T extends IForgeRegistryEntry<T>> {

	protected final IForgeRegistry<T> registry;

	protected final String domain;

	private static String getCurrentMod() {
		final ModContainer mc = ModLoadingContext.get().getActiveContainer();
		Preconditions.checkState(mc != null, "This method can be only used during mod initialization");
		return mc.getModId().toLowerCase();
	}

	public RegistrationContextBase(IForgeRegistry<T> registry, String domain) {
		this.registry = registry;
		this.domain = domain;
	}

	public RegistrationContextBase(IForgeRegistry<T> registry) {
		this(registry, getCurrentMod());
	}
}
