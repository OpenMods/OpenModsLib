package openmods.network;

public interface IEventPacketType {
	public abstract EventPacket createPacket();

	public abstract PacketDirection getDirection();

	public boolean isCompressed();

	public boolean isChunked();

	public int getId();
}
