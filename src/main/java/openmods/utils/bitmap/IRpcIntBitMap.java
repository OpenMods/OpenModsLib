package openmods.utils.bitmap;

import openmods.network.rpc.RpcMethod;

public interface IRpcIntBitMap {
	@RpcMethod("mark")
	void mark(Integer value);

	@RpcMethod("clear")
	void clear(Integer value);

	@RpcMethod("set")
	void set(Integer key, boolean value);

	@RpcMethod("toggle")
	void toggle(Integer value);

	@RpcMethod("clear_all")
	void clearAll();
}
