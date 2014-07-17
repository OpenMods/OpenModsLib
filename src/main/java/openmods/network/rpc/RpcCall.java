package openmods.network.rpc;

import java.lang.reflect.Method;

public class RpcCall {
	public final ITargetWrapper target;

	public final Method method;

	public final Object[] args;

	public RpcCall(ITargetWrapper target, Method method, Object[] args) {
		this.target = target;
		this.method = method;
		this.args = args;
	}
}
