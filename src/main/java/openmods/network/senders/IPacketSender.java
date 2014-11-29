package openmods.network.senders;

public interface IPacketSender<M> {
	public void sendPacket(M msg);
}
