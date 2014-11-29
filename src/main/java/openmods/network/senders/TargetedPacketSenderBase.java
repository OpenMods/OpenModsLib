package openmods.network.senders;

import io.netty.channel.Channel;
import openmods.utils.NetUtils;
import cpw.mods.fml.common.network.FMLOutboundHandler;

public class TargetedPacketSenderBase<M, T> implements ITargetedPacketSender<M, T> {

	private final Channel channel;

	public TargetedPacketSenderBase(Channel channel) {
		this.channel = channel;
	}

	protected void configureChannel(Channel channel, T target) {}

	protected void setTargetAttr(Channel channel, Object attr) {
		channel.attr(FMLOutboundHandler.FML_MESSAGETARGETARGS).set(attr);
	}

	@Override
	public void sendPacket(M msg, T target) {
		configureChannel(channel, target);
		channel.writeAndFlush(msg).addListener(NetUtils.LOGGING_LISTENER);
	}

	@Override
	public IPacketSender<M> bind(final T target) {
		return new IPacketSender<M>() {
			@Override
			public void sendPacket(M msg) {
				TargetedPacketSenderBase.this.sendPacket(msg, target);
			}
		};
	}
}
