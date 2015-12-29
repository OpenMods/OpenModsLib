package openmods.network;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import java.util.Map;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.LoaderState;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import net.minecraftforge.fml.common.network.NetworkHandshakeEstablished;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import openmods.Log;
import openmods.OpenMods;
import openmods.datastore.*;

import com.google.common.base.Preconditions;

// TODO compression!
public class IdSyncManager extends DataStoreManager {

	private static final String CHANNEL_NAME = "OpenMods|I";
	public static final IdSyncManager INSTANCE = new IdSyncManager();

	@Sharable
	private class InboundHandler extends SimpleChannelInboundHandler<FMLProxyPacket> {
		@Override
		protected void channelRead0(ChannelHandlerContext ctx, FMLProxyPacket msg) throws Exception {
			final PacketBuffer buf = new PacketBuffer(msg.payload());
			decodeIds(buf);
		}

		@Override
		public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
			if (evt instanceof NetworkHandshakeEstablished) {
				Log.debug("Sending id data for player: %s", OpenMods.proxy.getPlayerFromHandler(((NetworkHandshakeEstablished)evt).netHandler));
				sendAllIds(ctx);
			} else {
				ctx.fireUserEventTriggered(evt);
			}
		}
	}

	private static FMLProxyPacket serializeToPacket(DataStoreKey<?, ?> key, DataStoreWriter<?, ?> writer) {
		final PacketBuffer payload = new PacketBuffer(Unpooled.buffer());

		payload.writeString(key.id);
		writer.write(payload);

		return new FMLProxyPacket(payload, CHANNEL_NAME);
	}

	private IdSyncManager() {
		NetworkRegistry.INSTANCE.newChannel(CHANNEL_NAME, new InboundHandler());
	}

	public <K, V> DataStoreBuilder<K, V> createDataStore(String domain, String id, Class<? extends K> keyClass, Class<? extends V> valueClass) {
		final String fullId = domain + ":" + id;
		return createDataStore(fullId, keyClass, valueClass);
	}

	@Override
	public <K, V> DataStoreBuilder<K, V> createDataStore(String id, Class<? extends K> keyClass, Class<? extends V> valueClass) {
		Preconditions.checkState(!Loader.instance().hasReachedState(LoaderState.POSTINITIALIZATION), "This method cannot be called in post-initialization state and later");
		return super.createDataStore(id, keyClass, valueClass);
	}

	private void sendAllIds(ChannelHandlerContext ctx) {
		validate();

		for (Map.Entry<DataStoreKey<?, ?>, DataStoreWrapper<?, ?>> e : dataStoreMeta.entrySet()) {
			FMLProxyPacket packet = serializeToPacket(e.getKey(), e.getValue().createWriter());
			ctx.write(packet);
		}
	}

	private void decodeIds(PacketBuffer buf) {
		final String keyId = buf.readStringFromBuffer(0xFFFF);

		Log.debug("Received data store for key %s, packet size = %d", keyId, buf.writerIndex());
		DataStoreWrapper<?, ?> wrapper = getDataStoreMeta(keyId);
		DataStoreReader<?, ?> reader = wrapper.createReader();
		reader.read(buf);
	}

	@SubscribeEvent
	public void onDisconnect(ClientDisconnectionFromServerEvent evt) {
		Log.debug("Disconnected, restoring local data");
		activateLocalData();
	}

	public void finishLoading() {
		validate();
		MinecraftForge.EVENT_BUS.register(this);
	}
}
