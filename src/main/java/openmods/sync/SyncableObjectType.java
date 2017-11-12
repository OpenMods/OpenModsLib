package openmods.sync;

import com.google.common.base.Preconditions;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistryEntry;

public abstract class SyncableObjectType implements IForgeRegistryEntry<SyncableObjectType> {

	private ResourceLocation name;

	public abstract ISyncableObject createDummyObject();

	public abstract Class<? extends ISyncableObject> getObjectClass();

	public boolean isValidType(ISyncableObject object) {
		return getObjectClass().isInstance(object);
	}

	@Override
	public SyncableObjectType setRegistryName(ResourceLocation name) {
		Preconditions.checkState(this.name == null, "Name already set, %s->%s", this.name, name);
		this.name = name;
		return this;
	}

	@Override
	public ResourceLocation getRegistryName() {
		return name;
	}

	@Override
	public Class<SyncableObjectType> getRegistryType() {
		return SyncableObjectType.class;
	}

}
