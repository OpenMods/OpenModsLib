package openmods.network.rpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import net.minecraftforge.registries.IForgeRegistry;
import openmods.network.senders.IPacketSender;
import openmods.utils.CommonRegistryCallbacks;
import org.apache.commons.lang3.ArrayUtils;

public class RpcProxyFactory {

	private final IForgeRegistry<MethodEntry> registry;

	RpcProxyFactory(IForgeRegistry<MethodEntry> registry) {
		this.registry = registry;
	}

	@SuppressWarnings("unchecked")
	public <T> T createProxy(ClassLoader loader, final IPacketSender sender, final IRpcTarget wrapper, Class<? extends T> mainIntf, Class<?>... extraIntf) {
		Class<?> allInterfaces[] = ArrayUtils.add(extraIntf, mainIntf);

		final Map<Method, MethodEntry> methodMap = CommonRegistryCallbacks.getObjectToEntryMap(registry);

		Object proxy = Proxy.newProxyInstance(loader, allInterfaces, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				final MethodEntry entry = methodMap.get(method);
				if (entry != null) {
					RpcCall call = new RpcCall(wrapper, entry, args);
					sender.sendMessage(call);
				}
				return null;
			}
		});

		return (T)proxy;
	}
}
