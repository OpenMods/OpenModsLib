package openmods.network.event;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.IForgeRegistryEntry;
import net.minecraftforge.fml.common.registry.PersistentRegistryManager;
import net.minecraftforge.fml.common.registry.RegistryDelegate;

public abstract class NetworkEventEntry implements IForgeRegistryEntry<NetworkEventEntry> {

	public final RegistryDelegate<NetworkEventEntry> delegate = PersistentRegistryManager.makeDelegate(this, NetworkEventEntry.class);

	private ResourceLocation initialName;

	public abstract Class<? extends NetworkEvent> getPacketType();

	public abstract NetworkEvent createPacket();

	public abstract EventDirection getDirection();

	@Override
	public NetworkEventEntry setRegistryName(ResourceLocation name) {
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
	public Class<NetworkEventEntry> getRegistryType() {
		return NetworkEventEntry.class;
	}

}
