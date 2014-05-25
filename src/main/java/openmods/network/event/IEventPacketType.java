package openmods.network.event;

public interface IEventPacketType {
	public abstract EventPacket createPacket();

	public abstract PacketDirectionValidator getDirection();

	public boolean isCompressed();

	public boolean isChunked();

	public int getId();
}
