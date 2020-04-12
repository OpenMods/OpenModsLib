package openmods.network.rpc;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import io.netty.buffer.Unpooled;
import java.io.IOException;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.registries.IForgeRegistry;
import openmods.utils.CommonRegistryCallbacks;

public class RpcCallCodec {

	private final IForgeRegistry<TargetTypeProvider> targetRegistry;

	private final IForgeRegistry<MethodEntry> methodRegistry;

	public RpcCallCodec(IForgeRegistry<TargetTypeProvider> targetRegistry, IForgeRegistry<MethodEntry> methodRegistry) {
		this.targetRegistry = targetRegistry;
		this.methodRegistry = methodRegistry;
	}

	protected PacketBuffer encode(RpcCall call) throws IOException {
		final PacketBuffer output = new PacketBuffer(Unpooled.buffer());

		{
			final IRpcTarget targetWrapper = call.target;
			int targetId = CommonRegistryCallbacks.mapObjectToId(targetRegistry, targetWrapper.getClass());
			output.writeVarInt(targetId);
			targetWrapper.writeToStream(output);
		}

		{
			final BiMap<MethodEntry, Integer> eventIdMap = CommonRegistryCallbacks.getEntryIdMap(methodRegistry);
			int methodId = eventIdMap.get(call.method);
			output.writeVarInt(methodId);
			MethodParamsCodec paramsCodec = call.method.paramsCodec;
			paramsCodec.writeArgs(output, call.args);
		}

		return output;
	}

	protected RpcCall decode(PacketBuffer input,NetworkEvent.Context context) throws IOException {
		final LogicalSide side = context.getDirection().getReceptionSide();

		final IRpcTarget target;
		final MethodEntry method;
		final Object[] args;

		{
			final int targetId = input.readVarInt();
			final BiMap<Integer, TargetTypeProvider> idToEntryMap = CommonRegistryCallbacks.getEntryIdMap(targetRegistry).inverse();
			final TargetTypeProvider entry = idToEntryMap.get(targetId);
			target = entry.createRpcTarget();
			target.readFromStreamStream(side, input);
		}

		{
			final BiMap<MethodEntry, Integer> eventIdMap = CommonRegistryCallbacks.getEntryIdMap(methodRegistry);
			final int methodId = input.readVarInt();
			method = eventIdMap.inverse().get(methodId);
			args = method.paramsCodec.readArgs(input);
		}

		int bufferJunkSize = input.readableBytes();
		Preconditions.checkState(bufferJunkSize == 0, "%s junk bytes left in buffer, method = %s", bufferJunkSize, method);

		return new RpcCall(target, method, args);
	}
}
