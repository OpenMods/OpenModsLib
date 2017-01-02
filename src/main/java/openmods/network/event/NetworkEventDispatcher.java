package openmods.network.event;

import java.util.Map;
import net.minecraftforge.fml.common.network.FMLEmbeddedChannel;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import openmods.network.Dispatcher;
import openmods.network.ExtendedOutboundHandler;

public class NetworkEventDispatcher extends Dispatcher {

	public static final String CHANNEL_NAME = "OpenMods|E";

	private final Map<Side, FMLEmbeddedChannel> channels;

	public final Senders senders;

	public NetworkEventDispatcher(NetworkEventRegistry registry) {
		this.channels = NetworkRegistry.INSTANCE.newChannel(CHANNEL_NAME, new NetworkEventCodec(registry), new NetworkEventInboundHandler());
		ExtendedOutboundHandler.install(this.channels);

		this.senders = new Senders();
	}

	@Override
	protected FMLEmbeddedChannel getChannel(Side side) {
		return channels.get(side);
	}

}
