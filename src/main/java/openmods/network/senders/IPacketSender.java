package openmods.network.senders;

import java.util.Collection;

public interface IPacketSender {
	public void sendMessage(Object msg);

	public void sendMessages(Collection<Object> msg);
}
