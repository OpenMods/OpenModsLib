package openmods;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import cpw.mods.fml.common.FMLLog;

public final class Log {
	private Log() {}

	private static final Logger logger;

	static {
		logger = Logger.getLogger("OpenMods");
		logger.setLevel(Level.ALL);

		Logger rootLogger = FMLLog.getLogger();
		if (rootLogger != null) {
			logger.setParent(rootLogger);
		} else {
			ConsoleHandler handler = new ConsoleHandler();
			handler.setLevel(Level.ALL);
			logger.addHandler(handler);
			logger.setUseParentHandlers(false);
		}
	}

	private static final Throwable stackInfo = new Throwable();

	private static String getLogLocation(Throwable t) {
		// first element is always log function

		// maybe faster but definitely unsafe implementation:
		// JavaLangAccess access = SharedSecrets.getJavaLangAccess();
		// if (access.getStackTraceDepth(t) < 2) return "";
		// final StackTraceElement caller = access.getStackTraceElement(t, 1);

		final StackTraceElement[] stack = t.getStackTrace();
		if (stack.length < 2) return "";
		final StackTraceElement caller = stack[1];
		return caller.getClassName() + "." + caller.getMethodName() + "(" + caller.getFileName() + ":" + caller.getLineNumber() + "): ";
	}

	private static void logWithCaller(Throwable callerStack, Level level, String format, Object... data) {
		logger.log(level, getLogLocation(callerStack) + String.format(format, data));
	}

	public static void log(Level level, String format, Object... data) {
		logWithCaller(stackInfo.fillInStackTrace(), level, format, data);
	}

	public static void severe(String format, Object... data) {
		logWithCaller(stackInfo.fillInStackTrace(), Level.SEVERE, format, data);
	}

	public static void warn(String format, Object... data) {
		logWithCaller(stackInfo.fillInStackTrace(), Level.WARNING, format, data);
	}

	public static void info(String format, Object... data) {
		logWithCaller(stackInfo.fillInStackTrace(), Level.INFO, format, data);
	}

	public static void fine(String format, Object... data) {
		logWithCaller(stackInfo.fillInStackTrace(), Level.FINE, format, data);
	}

	public static void finer(String format, Object... data) {
		logWithCaller(stackInfo.fillInStackTrace(), Level.FINER, format, data);
	}

	public static void finest(String format, Object... data) {
		logWithCaller(stackInfo.fillInStackTrace(), Level.FINEST, format, data);
	}

	public static void log(Level level, Throwable ex, String format, Object... data) {
		logger.log(level, String.format(format, data), ex);
	}

	public static void severe(Throwable ex, String format, Object... data) {
		log(Level.SEVERE, ex, format, data);
	}

	public static void warn(Throwable ex, String format, Object... data) {
		log(Level.WARNING, ex, format, data);
	}
}
