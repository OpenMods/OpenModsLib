package openmods.core;

import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closer;
import java.io.File;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import jline.internal.Log;
import net.minecraft.launchwrapper.LaunchClassLoader;
import openmods.utils.io.Locks;
import openmods.utils.io.Locks.ExclusiveLock;
import openmods.utils.io.Locks.SharedLock;

public class BundledJarUnpacker {

	private static final String JAR_JARS_ATTRIBUTE = "JarJars";

	private static ExclusiveLock findLockableFile(File dir, String name) throws IOException {
		for (int i = 0; i < 1000; i++) {
			final File subDir = new File(dir, Integer.toString(i));

			if (subDir.isFile()) continue;

			if (!subDir.exists())
				if (!subDir.mkdirs()) continue;

			final File file = new File(subDir, name);
			final Optional<ExclusiveLock> lock = Locks.tryExclusiveLock(file);
			if (lock.isPresent()) return lock.get();
		}

		throw new IllegalStateException("Failed to find temporary dir for libs in " + dir);
	}

	public static void setup(Map<String, Object> data) {
		try {
			final File coremodFile = (File)data.get("coremodLocation");
			if (coremodFile == null) return;

			final LaunchClassLoader classLoader = (LaunchClassLoader)data.get("classLoader");

			final File tmpDir = new File(System.getProperty("java.io.tmpdir"));
			final File libDir = new File(tmpDir, "mc_libs");
			if (!libDir.exists()) libDir.mkdir();

			final JarFile coremodJar = new JarFile(coremodFile);
			final Closer closer = Closer.create();
			try {
				final String jarJars = coremodJar.getManifest().getMainAttributes().getValue(JAR_JARS_ATTRIBUTE);

				for (String jarJar : Splitter.on(" ").split(jarJars)) {
					final ZipEntry entry = coremodJar.getEntry(jarJar);
					if (entry == null) throw new IllegalAccessException("Can't find entry " + jarJar + " in jar " + coremodFile);

					final ExclusiveLock lockedFile = findLockableFile(libDir, jarJar);

					Log.debug("Copying file %s from %s to %s", jarJar, coremodFile, lockedFile.file);
					final ReadableByteChannel jarJarStream = Channels.newChannel(coremodJar.getInputStream(entry));
					ByteStreams.copy(jarJarStream, lockedFile.channel);
					jarJarStream.close();

					// keep shared lock to prevent file overwrite
					final SharedLock sharedFile = closer.register(lockedFile.degrade());
					sharedFile.file.deleteOnExit();

					classLoader.addURL(sharedFile.file.toURI().toURL());
				}
			} catch (Throwable t) {
				throw closer.rethrow(t);
			} finally {
				coremodJar.close();
			}

			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
				public void run() {
					try {
						closer.close();
					} catch (IOException e) {
						// welp
					}
				}
			});
		} catch (Exception e) {
			throw new IllegalStateException("Failed to inject dependecies, data: " + data.toString(), e);
		}
	}

}
