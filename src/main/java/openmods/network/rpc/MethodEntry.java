package openmods.network.rpc;

import com.google.common.base.Preconditions;
import java.lang.reflect.Method;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistryEntry;

public class MethodEntry implements IForgeRegistryEntry<MethodEntry> {

	private ResourceLocation name;

	public final Method method;

	public final MethodParamsCodec paramsCodec;

	public MethodEntry(Method method) {
		this.method = method;
		this.paramsCodec = new MethodParamsCodec(method);
	}

	@Override
	public MethodEntry setRegistryName(ResourceLocation name) {
		Preconditions.checkState(this.name == null, "Name already set, %s->%s", this.name, name);
		this.name = name;
		return this;
	}

	@Override
	public ResourceLocation getRegistryName() {
		return name;
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
