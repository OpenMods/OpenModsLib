package openmods.network.event;

import com.google.common.base.Preconditions;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistryEntry;

public abstract class NetworkEventEntry implements IForgeRegistryEntry<NetworkEventEntry> {

	private ResourceLocation name;

	public abstract Class<? extends NetworkEvent> getPacketType();

	public abstract NetworkEvent createInstance();

	public abstract EventDirection getDirection();

	@Override
	public NetworkEventEntry setRegistryName(ResourceLocation name) {
		Preconditions.checkState(this.name == null, "Name already set, %s->%s", this.name, name);
		this.name = name;
		return this;
	}

	@Override
	public ResourceLocation getRegistryName() {
		return name;
	}

	@Override
	public Class<NetworkEventEntry> getRegistryType() {
		return NetworkEventEntry.class;
	}

}
