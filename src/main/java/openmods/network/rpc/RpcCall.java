package openmods.network.rpc;

public class RpcCall {
	public final IRpcTarget target;

	public final MethodEntry method;

	public final Object[] args;

	public RpcCall(IRpcTarget target, MethodEntry method, Object[] args) {
		this.target = target;
		this.method = method;
		this.args = args;
	}
}
