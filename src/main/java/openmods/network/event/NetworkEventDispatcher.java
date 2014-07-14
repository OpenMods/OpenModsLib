package openmods.network.event;

import java.util.Map;

import openmods.network.Dispatcher;
import openmods.network.ExtendedOutboundHandler;
import cpw.mods.fml.common.network.FMLEmbeddedChannel;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;

public class NetworkEventDispatcher extends Dispatcher<NetworkEvent> {

	private final Map<Side, FMLEmbeddedChannel> channels;

	public NetworkEventDispatcher(NetworkEventRegistry registry) {
		this.channels = NetworkRegistry.INSTANCE.newChannel("OpenMods|E", new NetworkEventCodec(registry), new NetworkEventInboundHandler());
		ExtendedOutboundHandler.install(this.channels);
	}

	@Override
	protected FMLEmbeddedChannel getChannel(NetworkEvent msg, Side side) {
		return channels.get(side);
	}

}
