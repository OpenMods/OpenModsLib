package openmods.network.rpc;

import com.google.common.base.Preconditions;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import java.io.DataOutput;
import java.lang.reflect.Method;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetHandler;
import openmods.OpenMods;
import openmods.utils.ByteUtils;

@Sharable
public class RpcCallCodec extends MessageToMessageCodec<FMLProxyPacket, RpcCall> {

	private final TargetWrapperRegistry targetRegistry;

	private final MethodIdRegistry methodRegistry;

	public RpcCallCodec(TargetWrapperRegistry targetRegistry, MethodIdRegistry methodRegistry) {
		this.targetRegistry = targetRegistry;
		this.methodRegistry = methodRegistry;
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, RpcCall call, List<Object> out) throws Exception {
		ByteBuf buf = Unpooled.buffer();

		DataOutput output = new ByteBufOutputStream(buf);

		{
			final IRpcTarget targetWrapper = call.target;
			int targetId = targetRegistry.getWrapperId(targetWrapper.getClass());
			ByteUtils.writeVLI(output, targetId);
			targetWrapper.writeToStream(output);
		}

		{
			final Method method = call.method;
			int methodId = methodRegistry.methodToId(method);
			ByteUtils.writeVLI(output, methodId);
			MethodParamsCodec paramsCodec = MethodParamsCodec.create(method);
			paramsCodec.writeArgs(output, call.args);
		}

		FMLProxyPacket packet = new FMLProxyPacket(buf.copy(), RpcCallDispatcher.CHANNEL_NAME);
		out.add(packet);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, FMLProxyPacket msg, List<Object> out) throws Exception {
		ByteBufInputStream input = new ByteBufInputStream(msg.payload());

		final IRpcTarget target;
		final Method method;
		final Object[] args;

		{
			int targetId = ByteUtils.readVLI(input);
			target = targetRegistry.createWrapperFromId(targetId);
			EntityPlayer player = getPlayer(msg);
			target.readFromStreamStream(player, input);
		}

		{
			int methodId = ByteUtils.readVLI(input);
			method = methodRegistry.idToMethod(methodId);
			MethodParamsCodec paramsCodec = MethodParamsCodec.create(method);
			args = paramsCodec.readArgs(input);
		}

		int bufferJunkSize = input.available();
		Preconditions.checkState(bufferJunkSize == 0, "%s junk bytes left in buffer, method = %s", bufferJunkSize, method);

		out.add(new RpcCall(target, method, args));
	}

	protected EntityPlayer getPlayer(FMLProxyPacket msg) {
		INetHandler handler = msg.handler();
		EntityPlayer player = OpenMods.proxy.getPlayerFromHandler(handler);
		Preconditions.checkNotNull(player, "Can't get player from handler %s", handler);
		return player;
	}

}
