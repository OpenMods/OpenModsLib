package openmods.sync;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.IForgeRegistryEntry;
import net.minecraftforge.fml.common.registry.PersistentRegistryManager;
import net.minecraftforge.fml.common.registry.RegistryDelegate;

public abstract class SyncableObjectType implements IForgeRegistryEntry<SyncableObjectType> {

	public final RegistryDelegate<SyncableObjectType> delegate = PersistentRegistryManager.makeDelegate(this, SyncableObjectType.class);

	private ResourceLocation initialName;

	public abstract ISyncableObject createDummyObject();

	public abstract Class<? extends ISyncableObject> getObjectClass();

	public boolean isValidType(ISyncableObject object) {
		return getObjectClass().isInstance(object);
	}

	@Override
	public SyncableObjectType setRegistryName(ResourceLocation name) {
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
	public Class<? super SyncableObjectType> getRegistryType() {
		return SyncableObjectType.class;
	}

}
