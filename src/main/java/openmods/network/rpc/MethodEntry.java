package openmods.network.rpc;

import java.lang.reflect.Method;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.IForgeRegistryEntry;
import net.minecraftforge.fml.common.registry.PersistentRegistryManager;
import net.minecraftforge.fml.common.registry.RegistryDelegate;

public class MethodEntry implements IForgeRegistryEntry<MethodEntry> {

	public final RegistryDelegate<MethodEntry> delegate = PersistentRegistryManager.makeDelegate(this, MethodEntry.class);

	private ResourceLocation initialName;

	public final Method method;

	public final MethodParamsCodec paramsCodec;

	public MethodEntry(Method method) {
		this.method = method;
		this.paramsCodec = new MethodParamsCodec(method);
	}

	@Override
	public MethodEntry setRegistryName(ResourceLocation name) {
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
	public Class<MethodEntry> getRegistryType() {
		return MethodEntry.class;
	}

	@Override
	public String toString() {
		return "Method{" + method + "}";
	}

}
