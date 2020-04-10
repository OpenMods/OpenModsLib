package openmods.utils.bitmap;

import net.minecraft.util.Direction;
import openmods.network.rpc.RpcMethod;

public interface IRpcDirectionBitMap {
	@RpcMethod("mark")
	void mark(Direction value);

	@RpcMethod("clear")
	void clear(Direction value);

	@RpcMethod("set")
	void set(Direction key, boolean value);

	@RpcMethod("toggle")
	void toggle(Direction value);

	@RpcMethod("clear_all")
	void clearAll();
}
