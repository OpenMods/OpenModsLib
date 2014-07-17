package openmods.network;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import openmods.network.targets.SelectChunkWatchers;
import openmods.network.targets.SelectEntityWatchers;
import openmods.utils.NetUtils;
import cpw.mods.fml.common.network.*;
import cpw.mods.fml.common.network.FMLOutboundHandler.OutboundTarget;
import cpw.mods.fml.common.network.NetworkRegistry.TargetPoint;
import cpw.mods.fml.relauncher.Side;

public abstract class Dispatcher<M> {

	protected abstract FMLEmbeddedChannel getChannel(Side side);

	private void sendmsg(M msg, Object target, Object arg, Side side) {
		if (msg == null) return;

		final FMLEmbeddedChannel channel = getChannel(side);

		if (target instanceof OutboundTarget) {
			channel.attr(FMLOutboundHandler.FML_MESSAGETARGET).set((OutboundTarget)target);
		} else if (target instanceof IPacketTargetSelector) {
			channel.attr(ExtendedOutboundHandler.MESSAGETARGET).set((IPacketTargetSelector)target);
		} else throw new IllegalArgumentException("Invalid target class: " + target);

		if (arg != null) channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(arg);
		channel.writeAndFlush(msg).addListener(NetUtils.THROWING_LISTENER);
	}

	public void sendToPlayer(M msg, EntityPlayerMP player) {
		sendmsg(msg, OutboundTarget.PLAYER, player, Side.SERVER);
	}

	public void sendToDimension(M msg, int dimensionId) {
		sendmsg(msg, OutboundTarget.DIMENSION, dimensionId, Side.SERVER);
	}

	public void sendToAll(M msg) {
		sendmsg(msg, OutboundTarget.ALL, null, Side.SERVER);
	}

	public void sendToAllAround(M msg, TargetPoint point) {
		sendmsg(msg, OutboundTarget.ALLAROUNDPOINT, point, Side.SERVER);
	}

	public void sendToBlockWatchers(M msg, DimCoord point) {
		sendmsg(msg, new SelectChunkWatchers(), point, Side.SERVER);
	}

	public void sendToEntityWatchers(M msg, Entity entity) {
		sendmsg(msg, new SelectEntityWatchers(), entity, Side.SERVER);
	}

	public void sendToServer(M msg) {
		sendmsg(msg, OutboundTarget.TOSERVER, null, Side.CLIENT);
	}

}
