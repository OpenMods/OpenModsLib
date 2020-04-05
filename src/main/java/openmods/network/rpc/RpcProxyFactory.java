package openmods.network.rpc;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraftforge.registries.IForgeRegistry;
import openmods.utils.CommonRegistryCallbacks;
import org.apache.commons.lang3.ArrayUtils;

public class RpcProxyFactory {

	private final IForgeRegistry<MethodEntry> registry;

	RpcProxyFactory(IForgeRegistry<MethodEntry> registry) {
		this.registry = registry;
	}

	@SuppressWarnings("unchecked")
	public <T> T createProxy(ClassLoader loader, final Consumer<RpcCall> sender, final IRpcTarget wrapper, Class<? extends T> mainIntf, Class<?>... extraIntf) {
		Class<?> allInterfaces[] = ArrayUtils.add(extraIntf, mainIntf);

		final Map<Method, MethodEntry> methodMap = CommonRegistryCallbacks.getObjectToEntryMap(registry);

		Object proxy = Proxy.newProxyInstance(loader, allInterfaces, (self, method, args) -> {
			final MethodEntry entry = methodMap.get(method);
			if (entry != null) {
				sender.accept(new RpcCall(wrapper, entry, args));
			}
			return null;
		});

		return (T)proxy;
	}
}
