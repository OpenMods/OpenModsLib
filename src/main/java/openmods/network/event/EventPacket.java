package openmods.network.event;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;

import com.google.common.collect.Lists;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.network.handshake.NetworkDispatcher;

public abstract class EventPacket extends Event {

	final List<EventPacket> replies = Lists.newArrayList();

	NetworkDispatcher dispatcher;

	protected abstract void readFromStream(DataInput input) throws IOException;

	protected abstract void writeToStream(DataOutput output) throws IOException;

	protected void appendLogInfo(List<String> info) {}

	public void reply(EventPacket reply) {
		reply.dispatcher = dispatcher;
		this.replies.add(reply);
	}
}
