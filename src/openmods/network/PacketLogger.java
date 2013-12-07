package openmods.network;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.*;

import net.minecraft.network.packet.Packet250CustomPayload;
import openmods.OpenMods;

import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;

public class PacketLogger {
	public static void log(Packet250CustomPayload packet, boolean incoming, String... extras) {
		log(packet, incoming, Arrays.asList(extras));
	}

	public static void log(Packet250CustomPayload packet, boolean incoming, List<String> extras) {
		List<String> fields = Lists.newArrayList();
		fields.add(packet.channel);
		fields.add(OpenMods.proxy.isServerThread()? "server" : "client");
		fields.add(incoming? "incoming" : "outgoing");
		fields.add(Integer.toString(packet.data.length));
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
				Handler handler = new FileHandler("test.log", 1024 * 1024, 5);
				handler.setFormatter(new PacketLogFormatter());
				debugLog.addHandler(handler);
			} catch (IOException e) {
				Throwables.propagate(e);
			}
		}
		return debugLog;
	}
}
