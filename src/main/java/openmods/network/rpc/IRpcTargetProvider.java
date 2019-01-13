package openmods.network.rpc;

@FunctionalInterface
public interface IRpcTargetProvider {
	IRpcTarget createRpcTarget();
}
