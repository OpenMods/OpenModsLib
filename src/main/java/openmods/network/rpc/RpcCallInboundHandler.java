package openmods.network.rpc;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import com.google.common.base.Preconditions;

@Sharable
public class RpcCallInboundHandler extends SimpleChannelInboundHandler<RpcCall> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, RpcCall msg) throws Exception {
		Object target = msg.target.getTarget();
		Preconditions.checkNotNull(target, "Target wrapper %s returned null object");
		msg.method.invoke(target, msg.args);
	}

}
