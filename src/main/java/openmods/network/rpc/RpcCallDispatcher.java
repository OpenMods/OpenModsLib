package openmods.network.rpc;

import java.util.Map;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderState;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import openmods.network.Dispatcher;
import openmods.network.ExtendedOutboundHandler;
import openmods.network.senders.IPacketSender;

import com.google.common.base.Preconditions;

public class RpcCallDispatcher extends Dispatcher {

	public static final RpcCallDispatcher INSTANCE = new RpcCallDispatcher();

	public static final String CHANNEL_NAME = "OpenMods|RPC";

	public final Senders senders;

	private final MethodIdRegistry methodRegistry = new MethodIdRegistry();

	private final TargetWrapperRegistry targetRegistry = new TargetWrapperRegistry();

	private RpcSetup setup = new RpcSetup();

	private RpcProxyFactory proxyFactory = new RpcProxyFactory(methodRegistry);

	private final Map<Side, FMLEmbeddedChannel> channels;

	private RpcCallDispatcher() {
		this.channels = NetworkRegistry.INSTANCE.newChannel(CHANNEL_NAME, new RpcCallCodec(targetRegistry, methodRegistry), new RpcCallInboundHandler());
		ExtendedOutboundHandler.install(this.channels);

		this.senders = new Senders();
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

	public <T> T createProxy(IRpcTarget wrapper, IPacketSender sender, Class<? extends T> mainIntf, Class<?>... extraIntf) {
		return proxyFactory.createProxy(getClass().getClassLoader(), sender, wrapper, mainIntf, extraIntf);
	}
}
