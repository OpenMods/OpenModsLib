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

import cpw.mods.fml.common.network.*;
import cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;

public class NetworkEventDispatcher {

	private final Map<Class<? extends NetworkEvent>, ModEventChannel> registeredEvents = Maps.newHashMap();

	private void sendEvent(NetworkEvent event, Object target, Object arg, Side side) {
		if (event == null) return;

		ModEventChannel modChannel = registeredEvents.get(event.getClass());
		Preconditions.checkNotNull(modChannel, "Event type %s not registered", event.getClass());
		final FMLEmbeddedChannel channel = modChannel.channel(side);

		if (target instanceof OutboundTarget) {
			channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set((OutboundTarget)target);
		} else if (target instanceof IPacketTargetSelector) {
			channel.attr(ExtendedOutboundHandler.MESSAGETARGET).set((IPacketTargetSelector)target);
		} else throw new IllegalArgumentException("Invalid target class: " + target);

		if (arg != null) channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(arg);
		channel.writeAndFlush(event).addListener(NetUtils.THROWING_LISTENER);
	}

	public void register(Class<? extends NetworkEvent> cls, ModEventChannel channel) {
		ModEventChannel prev = registeredEvents.put(cls, channel);
		if (prev != null) throw new IllegalStateException(String.format("Conflict for class %s, channels: %s, %s", cls, channel.modId, prev.modId));
	}

	public void unregister(Class<? extends NetworkEvent> cls) {
		registeredEvents.remove(cls);
	}

	public void sendToPlayer(NetworkEvent event, EntityPlayerMP player) {
		sendEvent(event, OutboundTarget.PLAYER, player, Side.SERVER);
	}

	public void sendToDimension(NetworkEvent event, int dimensionId) {
		sendEvent(event, OutboundTarget.DIMENSION, dimensionId, Side.SERVER);
	}

	public void sendToAll(NetworkEvent event) {
		sendEvent(event, OutboundTarget.ALL, null, Side.SERVER);
	}

	public void sendToAllAround(NetworkEvent event, TargetPoint point) {
		sendEvent(event, OutboundTarget.ALLAROUNDPOINT, point, Side.SERVER);
	}

	public void sendToBlockWatchers(NetworkEvent event, DimCoord point) {
		sendEvent(event, new SelectChunkWatchers(), point, Side.SERVER);
	}

	public void sendToEntityWatchers(NetworkEvent event, Entity entity) {
		sendEvent(event, new SelectEntityWatchers(), entity, Side.SERVER);
	}

	public void sendToServer(NetworkEvent event) {
		sendEvent(event, OutboundTarget.TOSERVER, null, Side.CLIENT);
	}

}
