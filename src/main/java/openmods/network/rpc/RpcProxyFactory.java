package openmods.network.rpc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import openmods.network.senders.IPacketSender;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.base.Preconditions;

public class RpcProxyFactory {

	private final MethodIdRegistry registry;

	RpcProxyFactory(MethodIdRegistry registry) {
		this.registry = registry;
	}

	@SuppressWarnings("unchecked")
	public <T> T createProxy(ClassLoader loader, final IPacketSender sender, final IRpcTarget wrapper, Class<? extends T> mainIntf, Class<?>... extraIntf) {
		Class<?> allInterfaces[] = ArrayUtils.add(extraIntf, mainIntf);

		for (Class<?> intf : allInterfaces)
			Preconditions.checkState(registry.isClassRegistered(intf), "Class %s is not registered as RPC interface", intf);

		Object proxy = Proxy.newProxyInstance(loader, allInterfaces, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				RpcCall call = new RpcCall(wrapper, method, args);
				sender.sendMessage(call);
				return null;
			}
		});

		return (T)proxy;
	}
}
