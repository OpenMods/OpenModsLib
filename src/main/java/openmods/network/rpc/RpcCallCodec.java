package openmods.network.rpc;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import java.util.List;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.INetHandler;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.common.registry.IForgeRegistry;
import openmods.OpenMods;
import openmods.utils.CommonRegistryCallbacks;

@Sharable
public class RpcCallCodec extends MessageToMessageCodec<FMLProxyPacket, RpcCall> {

	private final IForgeRegistry<TargetTypeProvider> targetRegistry;

	private final IForgeRegistry<MethodEntry> methodRegistry;

	public RpcCallCodec(IForgeRegistry<TargetTypeProvider> targetRegistry, IForgeRegistry<MethodEntry> methodRegistry) {
		this.targetRegistry = targetRegistry;
		this.methodRegistry = methodRegistry;
	}

	@Override
	protected void encode(ChannelHandlerContext ctx, RpcCall call, List<Object> out) throws Exception {
		final PacketBuffer output = new PacketBuffer(Unpooled.buffer());

		{
			final IRpcTarget targetWrapper = call.target;
			int targetId = CommonRegistryCallbacks.mapObjectToId(targetRegistry, targetWrapper.getClass());
			output.writeVarIntToBuffer(targetId);
			targetWrapper.writeToStream(output);
		}

		{
			final BiMap<MethodEntry, Integer> eventIdMap = CommonRegistryCallbacks.getEntryIdMap(methodRegistry);
			int methodId = eventIdMap.get(call.method);
			output.writeVarIntToBuffer(methodId);
			MethodParamsCodec paramsCodec = call.method.paramsCodec;
			paramsCodec.writeArgs(output, call.args);
		}

		FMLProxyPacket packet = new FMLProxyPacket(output, RpcCallDispatcher.CHANNEL_NAME);
		out.add(packet);
	}

	@Override
	protected void decode(ChannelHandlerContext ctx, FMLProxyPacket msg, List<Object> out) throws Exception {
		final PacketBuffer input = new PacketBuffer(msg.payload());

		final IRpcTarget target;
		final MethodEntry method;
		final Object[] args;

		{
			final int targetId = input.readVarIntFromBuffer();
			final BiMap<Integer, TargetTypeProvider> idToEntryMap = CommonRegistryCallbacks.getEntryIdMap(targetRegistry).inverse();
			final TargetTypeProvider entry = idToEntryMap.get(targetId);
			target = entry.createRpcTarget();
			EntityPlayer player = getPlayer(msg);
			target.readFromStreamStream(player, input);
		}

		{
			final BiMap<MethodEntry, Integer> eventIdMap = CommonRegistryCallbacks.getEntryIdMap(methodRegistry);
			final int methodId = input.readVarIntFromBuffer();
			method = eventIdMap.inverse().get(methodId);
			args = method.paramsCodec.readArgs(input);
		}

		int bufferJunkSize = input.readableBytes();
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
