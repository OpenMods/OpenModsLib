package openmods.network.event;

import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import openmods.network.DimCoord;
import openmods.network.ExtendedOutboundHandler;
import openmods.network.IPacketTargetSelector;
import openmods.network.targets.SelectChunkWatchers;
import openmods.network.targets.SelectEntityWatchers;
import openmods.utils.NetUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.network.*;
import cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;

public class EventPacketManager {

	private EventPacketManager() {}

	public static final EventPacketManager INSTANCE = new EventPacketManager();

	private static class ModChannel {
		private final Map<Side, FMLEmbeddedChannel> channels;
		private final EventPacketRegistry registry;

		public ModChannel(String modId) {
			this.registry = new EventPacketRegistry(modId);
			EventPacketCodec codec = new EventPacketCodec(registry);

			String channelId = modId + "|E";
			this.channels = NetworkRegistry.INSTANCE.newChannel(channelId, codec, new EventPacketInboundHandler());

			ExtendedOutboundHandler.install(this.channels);
		}

	}

	private final Map<String, ModChannel> channels = Maps.newHashMap();

	private final Map<Class<? extends EventPacket>, ModChannel> registeredEvents = Maps.newHashMap();

	private ModChannel getModChannel(String modId) {
		ModChannel channel = channels.get(modId);
		if (channel == null) {
			channel = new ModChannel(modId);
			channels.put(modId, channel);
		}

		return channel;
	}

	public void registerEvent(Class<? extends EventPacket> eventCls) {
		ModContainer container = Loader.instance().activeModContainer();
		Preconditions.checkNotNull(container, "This method can only be called in during mod initialization");
		registerEvent(eventCls, container.getModId());
	}

	public void registerEvent(Class<? extends EventPacket> eventCls, String modId) {
		ModChannel channel = getModChannel(modId);
		channel.registry.registerType(eventCls);
		registeredEvents.put(eventCls, channel);
	}

	private void sendEvent(EventPacket event, Object target, Object arg, Side side) {
		if (event == null) return;

		ModChannel modChannel = registeredEvents.get(event.getClass());
		Preconditions.checkNotNull(modChannel, "Event type %s not registered", event.getClass());
		final FMLEmbeddedChannel channel = modChannel.channels.get(side);

		if (target instanceof OutboundTarget) {
			channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set((OutboundTarget)target);
		} else if (target instanceof IPacketTargetSelector) {
			channel.attr(ExtendedOutboundHandler.MESSAGETARGET).set((IPacketTargetSelector)target);
		} else throw new IllegalArgumentException("Invalid target class: " + target);

		if (arg != null) channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(arg);
		channel.writeAndFlush(event).addListener(NetUtils.THROWING_LISTENER);
	}

	public void sendToPlayer(EventPacket event, EntityPlayerMP player) {
		sendEvent(event, OutboundTarget.PLAYER, player, Side.SERVER);
	}

	public void sendToDimension(EventPacket event, int dimensionId) {
		sendEvent(event, OutboundTarget.DIMENSION, dimensionId, Side.SERVER);
	}

	public void sendToAll(EventPacket event) {
		sendEvent(event, OutboundTarget.ALL, null, Side.SERVER);
	}

	public void sendToAllAround(EventPacket event, TargetPoint point) {
		sendEvent(event, OutboundTarget.ALLAROUNDPOINT, point, Side.SERVER);
	}

	public void sendToBlockWatchers(EventPacket event, DimCoord point) {
		sendEvent(event, new SelectChunkWatchers(), point, Side.SERVER);
	}

	public void sendToEntityWatchers(EventPacket event, Entity entity) {
		sendEvent(event, new SelectEntityWatchers(), entity, Side.SERVER);
	}

	public void sendToServer(EventPacket event) {
		sendEvent(event, OutboundTarget.TOSERVER, null, Side.CLIENT);
	}
}
