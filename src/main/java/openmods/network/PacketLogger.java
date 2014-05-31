package openmods.network;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.*;
import java.util.logging.Formatter;

import net.minecraft.network.packet.Packet131MapData;
import net.minecraft.network.packet.Packet250CustomPayload;
import openmods.OpenMods;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

public class PacketLogger {
	private static final String CHANNEL_PACKET_TYPE = "packet250";
	private static final String TINY_PACKET_TYPE = "packet131";

	public static void log(Packet250CustomPayload packet, boolean incoming, Collection<String> extras) {
		log(CHANNEL_PACKET_TYPE, packet.channel, packet.getPacketSize(), incoming, extras);
	}

	public static void log(Packet250CustomPayload packet, boolean incoming, String... extras) {
		log(CHANNEL_PACKET_TYPE, packet.channel, packet.getPacketSize(), incoming, Arrays.asList(extras));
	}

	public static void log(Packet131MapData packet, boolean incoming, Collection<String> extras) {
		log(TINY_PACKET_TYPE, packet.uniqueID, packet.getPacketSize(), incoming, extras);
	}

	public static void log(Packet131MapData packet, boolean incoming, String... extras) {
		log(TINY_PACKET_TYPE, packet.uniqueID, packet.getPacketSize(), incoming, Arrays.asList(extras));
	}

	public static void log(String type, Object source, int payloadLength, boolean incoming, Collection<String> extras) {
		List<String> fields = Lists.newArrayList();
		fields.add(type.toString());
		fields.add(source.toString());
		fields.add(OpenMods.proxy.isServerThread()? "server" : "client");
		fields.add(incoming? "incoming" : "outgoing");
		fields.add(Integer.toString(payloadLength));
		fields.addAll(extras);
		String extra = Joiner.on('\t').join(fields);
		getDebugLog().info(extra);
	}

	private static Logger debugLog;

	private static class PacketLogFormatter extends Formatter {

		private static final String LINE_SEPARATOR = System.getProperty("line.separator");
		private static final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS");

		@Override
		public String format(LogRecord record) {

			String isoDate = df.format(new Date(record.getMillis()));
			return isoDate + "\t" + formatMessage(record) + LINE_SEPARATOR;
		}
	}

	private static Logger getDebugLog() {
		if (debugLog == null) {
			debugLog = Logger.getLogger("packets");

			try {
				File dir = OpenMods.proxy.getMinecraftDir();
				File logPattern = new File(dir, "open-mods-packets.log");
				Handler handler = new FileHandler(logPattern.getCanonicalPath(), 1024 * 1024, 5);
				handler.setFormatter(new PacketLogFormatter());
				debugLog.addHandler(handler);
			} catch (IOException e) {
				Throwables.propagate(e);
			}
		}
		return debugLog;
	}
}
