package openmods.network.rpc;

import com.google.common.base.Preconditions;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistryEntry;

public abstract class TargetTypeProvider implements IForgeRegistryEntry<TargetTypeProvider> {

	private ResourceLocation name;

	public abstract IRpcTarget createRpcTarget();

	public abstract Class<? extends IRpcTarget> getTargetClass();

	@Override
	public TargetTypeProvider setRegistryName(ResourceLocation name) {
		Preconditions.checkState(this.name == null, "Name already set, %s->%s", this.name, name);
		this.name = name;
		return this;
	}

	@Override
	public ResourceLocation getRegistryName() {
		return name;
	}

	@Override
	public Class<TargetTypeProvider> getRegistryType() {
		return TargetTypeProvider.class;
	}

}
