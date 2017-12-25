package openmods.network.rpc;

@FunctionalInterface
public interface IRpcTargetProvider {
	public IRpcTarget createRpcTarget();
}
