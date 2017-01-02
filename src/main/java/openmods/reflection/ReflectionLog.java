package openmods.reflection;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.Type;

class ReflectionLog {

	private static final boolean ENABLE_LOG = Boolean.parseBoolean(System.getProperty("openmods.logReflection", "false"));

	private static final Logger logger;

	static {
		logger = LogManager.getLogger("OpenMods-Reflection");
	}

	private static final Throwable stackInfo = new Throwable();

	private synchronized static String findCaller() {
		final StackTraceElement[] stack = stackInfo.fillInStackTrace().getStackTrace();

		for (StackTraceElement el : stack) {
			final String cls = el.getClassName();
			if (!cls.startsWith("openmods.reflection.")) return cls;
		}

		return "<invalid>";
	}

	static void logLoad(Class<?> cls) {
		if (ENABLE_LOG) {
			logger.debug(String.format("###C %s %s", findCaller(), cls.getName()));
		}
	}

	static void logLoad(Constructor<?> ctor) {
		if (ENABLE_LOG) {
			logger.debug(String.format("###I %s %s %s", findCaller(), ctor.getDeclaringClass().getName(), Type.getType(ctor).getDescriptor()));
		}
	}

	static void logLoad(Method method) {
		if (ENABLE_LOG) {
			logger.debug(String.format("###M %s %s %s %s", findCaller(), method.getDeclaringClass().getName(), method.getName(), Type.getType(method).getDescriptor()));
		}
	}

	static void logLoad(Field field) {
		if (ENABLE_LOG) {
			logger.debug(String.format("###F %s %s %s %s", findCaller(), field.getDeclaringClass().getName(), field.getName(), Type.getType(field.getType()).getDescriptor()));
		}
	}
}
