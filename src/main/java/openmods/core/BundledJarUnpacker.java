package openmods.core;

import com.google.common.base.Splitter;
import com.google.common.io.Files;
import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import net.minecraft.launchwrapper.LaunchClassLoader;

public class BundledJarUnpacker {

	private static final String JAR_JARS_ATTRIBUTE = "JarJars";

	public static void setup(Map<String, Object> data) {
		try {
			final File coremodFile = (File)data.get("coremodLocation");
			final LaunchClassLoader classLoader = (LaunchClassLoader)data.get("classLoader");

			final JarFile coremodJar = new JarFile(coremodFile);
			try {
				final String jarJars = coremodJar.getManifest().getMainAttributes().getValue(JAR_JARS_ATTRIBUTE);

				final File jarJarsDir = Files.createTempDir();
				jarJarsDir.deleteOnExit();

				for (String jarJar : Splitter.on(" ").split(jarJars)) {
					final ZipEntry entry = coremodJar.getEntry(jarJar);
					if (entry == null) throw new IllegalAccessException("Can't find entry " + jarJar + " in jar " + coremodFile);
					final File jarJarTmpFile = new File(jarJarsDir, jarJar);
					jarJarTmpFile.deleteOnExit();

					final InputStream jarJarStream = coremodJar.getInputStream(entry);
					Files.asByteSink(jarJarTmpFile).writeFrom(jarJarStream);
					jarJarStream.close();

					classLoader.addURL(jarJarTmpFile.toURI().toURL());
				}
			} finally {
				coremodJar.close();
			}
		} catch (Exception e) {
			throw new IllegalStateException("Failed to inject dependecies, data: " + data.toString(), e);
		}
	}

}
