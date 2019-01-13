package openmods.network.senders;

import java.util.Collection;

public interface IPacketSender {
	void sendMessage(Object msg);

	void sendMessages(Collection<Object> msg);
}
