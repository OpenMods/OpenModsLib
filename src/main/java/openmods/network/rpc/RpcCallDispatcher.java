package openmods.network.rpc;

import java.util.Map;

import openmods.network.Dispatcher;

import com.google.common.base.Preconditions;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;
import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;

public class RpcCallDispatcher extends Dispatcher<RpcCall> {

	public static final RpcCallDispatcher INSTANCE = new RpcCallDispatcher();

	public static final String CHANNEL_NAME = "OpenMods|RPC";

	private final MethodIdRegistry methodRegistry = new MethodIdRegistry();

	private final TargetWrapperRegistry targetRegistry = new TargetWrapperRegistry();

	private RpcSetup setup = new RpcSetup();

	private RpcProxyFactory proxyFactory = new RpcProxyFactory(methodRegistry, this);

	private final Map<Side, FMLEmbeddedChannel> channels;

	private RpcCallDispatcher() {
		this.channels = NetworkRegistry.INSTANCE.newChannel(CHANNEL_NAME, new RpcCallCodec(targetRegistry, methodRegistry), new RpcCallInboundHandler());
	}

	@Override
	protected FMLEmbeddedChannel getChannel(Side side) {
		return channels.get(side);
	}

	public RpcSetup startRegistration() {
		Preconditions.checkState(Loader.instance().isInState(LoaderState.PREINITIALIZATION), "This method can only be called in pre-initialization state");
		return setup;
	}

	public void finishRegistration() {
		setup.finish(methodRegistry, targetRegistry);
		setup = null;
	}

	public <T> T createProxy(ITargetWrapper wrapper, Class<? extends T> mainIntf, Class<?>... extraIntf) {
		return proxyFactory.createClientProxy(getClass().getClassLoader(), wrapper, mainIntf, extraIntf);
	}
}
