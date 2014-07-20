package openmods.network.rpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Preconditions;

public class RpcProxyFactory {

	private final MethodIdRegistry registry;

	private final RpcCallDispatcher dispatcher;

	RpcProxyFactory(MethodIdRegistry registry, RpcCallDispatcher dispatcher) {
		this.registry = registry;
		this.dispatcher = dispatcher;
	}

	@SuppressWarnings("unchecked")
	public <T> T createClientProxy(ClassLoader loader, final IRpcTarget wrapper, Class<? extends T> mainIntf, Class<?>... extraIntf) {
		Class<?> allInterfaces[] = ArrayUtils.add(extraIntf, mainIntf);

		for (Class<?> intf : allInterfaces)
			Preconditions.checkState(registry.isClassRegistered(intf), "Class %s is not registered as RPC interface", intf);

		Object proxy = Proxy.newProxyInstance(loader, allInterfaces, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				RpcCall call = new RpcCall(wrapper, method, args);
				dispatcher.sendToServer(call);
				return null;
			}
		});

		return (T)proxy;
	}
}
