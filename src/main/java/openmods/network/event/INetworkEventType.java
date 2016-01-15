package openmods.network.event;

public interface INetworkEventType {
	public abstract NetworkEvent createPacket();

	public abstract EventDirection getDirection();

}
