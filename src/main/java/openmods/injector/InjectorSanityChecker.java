package openmods.injector;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import cpw.mods.fml.common.ICrashCallable;
import java.util.List;
import java.util.Random;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class InjectorSanityChecker implements ICrashCallable {

	private static final Random RANDOM = new Random();

	@Override
	public String call() throws Exception {
		final ClassLoader loader = getClass().getClassLoader();

		if (loader instanceof LaunchClassLoader) return findDerps((LaunchClassLoader)loader);
		else return "loader is not LaunchClassLoader, actual class: " + loader.getClass().getName();
	}

	private static String findDerps(LaunchClassLoader loader) {
		final List<String> unsafeTransformers = findUnsafeTransformers(loader);
		if (unsafeTransformers.isEmpty()) return "all safe";

		StringBuilder result = new StringBuilder("found misbehaving transformers: ");
		Joiner.on(',').appendTo(result, unsafeTransformers);
		return result.toString();
	}

	private static List<String> findUnsafeTransformers(LaunchClassLoader loader) {
		List<String> result = Lists.newArrayList();

		final List<IClassTransformer> transformers = loader.getTransformers();
		for (IClassTransformer transformer : transformers) {
			if (transformer == null) result.add("<null>");
			else {
				try {
					final String fakeCls = "test.test.test.test.Test$" + RANDOM.nextInt();
					byte[] transformed = transformer.transform(fakeCls, fakeCls, null);
					if (transformed != null) {
						result.add(String.format("%s(%s) returned non-null result: %d", transformer.getClass().getName(), transformer, transformed.length));
					}
				} catch (Throwable t) {
					result.add(String.format("%s(%s) crashed with %s(%s)", transformer.getClass().getName(), transformer, t.getClass().getName(), t.getMessage()));
				}
			}
		}

		return result;
	}

	@Override
	public String getLabel() {
		return "Class transformer null safety";
	}

}
