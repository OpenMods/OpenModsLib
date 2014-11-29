package openmods.network.senders;

public interface ITargetedPacketSender<M, T> {
	public void sendPacket(M msg, T target);

	public IPacketSender<M> bind(T target);
}
