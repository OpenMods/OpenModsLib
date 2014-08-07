package openmods.network;

import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.channel.ChannelHandler.Sharable;

import java.io.*;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.minecraft.network.INetHandler;
import openmods.Log;
import openmods.datastore.*;

import com.google.common.base.Preconditions;
import com.google.common.io.Closer;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.*;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.CustomPacketRegistrationEvent;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import cpw.mods.fml.relauncher.Side;

public class IdSyncManager extends DataStoreManager {

	private static final String CHANNEL_NAME = "OpenMods|I";

	private final Map<Side, FMLEmbeddedChannel> channels;

	public static final IdSyncManager INSTANCE = new IdSyncManager();

	private class ServerHandshakeHijacker extends ChannelOutboundHandlerAdapter {

		@Override
		public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
			ctx.writeAndFlush(msg);
			if (msg instanceof FMLProxyPacket) {
				FMLProxyPacket hijackedMsg = (FMLProxyPacket)msg;
				if (hijackedMsg.channel().equals("FML|HS")) {
					ByteBuf payload = hijackedMsg.payload();
					byte discriminator = payload.readByte();

					if (discriminator == 3 /* FMLHandshakeMessage.ModIdData */) {
						sendAllIds(ctx);
						ctx.pipeline().remove(this);
					} else if (discriminator == -1 /* FMLHandshakeMessage.HandshakeAck */) {
						ctx.pipeline().remove(this);
					}
				}
			}
		}
	}

	private class ClientHandshakeHijacker extends SimpleChannelInboundHandler<FMLProxyPacket> {
		@Override
		protected void channelRead0(ChannelHandlerContext ctx, FMLProxyPacket msg) throws Exception {
			if (msg.channel().equals(CHANNEL_NAME)) {
				channels.get(Side.CLIENT).writeInbound(msg);
			} else {
				ctx.fireChannelRead(msg);
			}
		}
	}

	@Sharable
	private class InboundHandler extends SimpleChannelInboundHandler<FMLProxyPacket> {
		@Override
		protected void channelRead0(ChannelHandlerContext ctx, FMLProxyPacket msg) throws Exception {
			ByteBuf buf = msg.payload();

			try {
				Closer closer = Closer.create();
				try {
					InputStream raw = closer.register(new ByteBufInputStream(buf));
					InputStream compressed = closer.register(new GZIPInputStream(raw));
					DataInput input = new DataInputStream(compressed);

					String keyId = input.readUTF();

					Log.debug("Received data store for key %s, packet size = %d", keyId, buf.writerIndex());
					DataStoreWrapper<?, ?> wrapper = getDataStoreMeta(keyId);
					DataStoreReader<?, ?> reader = wrapper.createReader();
					reader.read(input);
				} finally {
					closer.close();
				}
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public class NetEventHandler {

		@SubscribeEvent
		public void onJoin(CustomPacketRegistrationEvent<? extends INetHandler> evt) {
			if (!evt.manager.isLocalChannel() && evt.registrations.contains(CHANNEL_NAME)) {
				ChannelHandler handler = (evt.side == Side.SERVER)? new ServerHandshakeHijacker() : new ClientHandshakeHijacker();
				evt.manager.channel().pipeline().addAfter("fml:packet_handler", "openmods:id_injector", handler);
			}
		}

		@SubscribeEvent
		public void onHandshakeComplete(ClientConnectedToServerEvent evt) {
			if (!evt.manager.isLocalChannel()) {
				try {
					evt.manager.channel().pipeline().remove("openmods:id_injector");
				} catch (NoSuchElementException e) {
					// NO-OP - possibly removed earlier
				}
			}
		}

		@SubscribeEvent
		public void onDisconnect(ClientDisconnectionFromServerEvent evt) {
			activateLocalData();
		}
	}

	private static FMLProxyPacket serializeToPacket(DataStoreKey<?, ?> key, DataStoreWriter<?, ?> writer) {
		ByteBuf payload = Unpooled.buffer();

		Closer closer = Closer.create();

		try {
			try {
				OutputStream raw = closer.register(new ByteBufOutputStream(payload));
				OutputStream compressed = closer.register(new GZIPOutputStream(raw));
				DataOutput output = new DataOutputStream(compressed);
				output.writeUTF(key.id);
				writer.write(output);
			} finally {
				closer.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return new FMLProxyPacket(payload.copy(), CHANNEL_NAME);
	}

	private IdSyncManager() {
		this.channels = NetworkRegistry.INSTANCE.newChannel(CHANNEL_NAME, new InboundHandler());
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

	public void finishLoading() {
		validate();
		FMLCommonHandler.instance().bus().register(new NetEventHandler());
	}
}
