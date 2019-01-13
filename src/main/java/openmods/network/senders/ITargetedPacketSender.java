package openmods.network.senders;

import java.util.Collection;

public interface ITargetedPacketSender<T> {
	void sendMessage(Object msg, T target);

	void sendMessages(Collection<Object> msg, T target);

	IPacketSender bind(T target);
}
