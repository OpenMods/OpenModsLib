package openmods.network.rpc;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.IForgeRegistryEntry;
import net.minecraftforge.fml.common.registry.PersistentRegistryManager;
import net.minecraftforge.fml.common.registry.RegistryDelegate;

public abstract class TargetTypeProvider implements IForgeRegistryEntry<TargetTypeProvider> {

	public final RegistryDelegate<TargetTypeProvider> delegate = PersistentRegistryManager.makeDelegate(this, TargetTypeProvider.class);

	private ResourceLocation initialName;

	public abstract IRpcTarget createRpcTarget();

	public abstract Class<? extends IRpcTarget> getTargetClass();

	@Override
	public TargetTypeProvider setRegistryName(ResourceLocation name) {
		this.initialName = name;
		return this;
	}

	@Override
	public ResourceLocation getRegistryName() {
		final ResourceLocation registryName = delegate.name();
		if (registryName != null) return registryName;
		return initialName != null? initialName : null;
	}

	@Override
	public Class<? super TargetTypeProvider> getRegistryType() {
		return TargetTypeProvider.class;
	}

}
