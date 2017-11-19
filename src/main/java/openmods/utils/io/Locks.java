package openmods.utils.io;

import com.google.common.base.Optional;
import com.google.common.io.Closer;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import openmods.Log;
import org.apache.logging.log4j.Level;

public class Locks {

	public abstract static class Lock implements Closeable {
		public final File file;

		public final FileChannel channel;

		protected final FileLock lock;

		private Lock(File file, FileChannel channel, FileLock lock) {
			this.file = file;
			this.channel = channel;
			this.lock = lock;
		}

		public boolean isValid() {
			return lock.isValid();
		}

		public abstract boolean isShared();

		@Override
		public void close() throws IOException {
			lock.release();
			channel.close();
		}

		public abstract SharedLock degrade() throws IOException;

		public abstract ExclusiveLock upgrade() throws IOException;
	}

	public static class ExclusiveLock extends Lock {
		private ExclusiveLock(File file, FileChannel fc, FileLock lock) {
			super(file, fc, lock);
			if (lock.isShared()) throw new AssertionError("Invalid lock: " + lock);
		}

		@Override
		public SharedLock degrade() throws IOException {
			// Can't find way to do it as atomic operation.
			// This may cause some glitches and creates attach area (gasp!)
			lock.release();
			final FileLock sharedLock = channel.lock(0, Long.MAX_VALUE, true);
			if (sharedLock == null) throw new IllegalStateException("Failed to re-lock file " + file);
			return new SharedLock(file, channel, sharedLock);
		}

		@Override
		public ExclusiveLock upgrade() {
			return this;
		}

		@Override
		public boolean isShared() {
			return false;
		}
	}

	public static class SharedLock extends Lock {
		private SharedLock(File file, FileChannel fc, FileLock lock) {
			super(file, fc, lock);
			if (!lock.isShared()) throw new AssertionError("Invalid lock: " + lock);
		}

		@Override
		public SharedLock degrade() {
			return this;
		}

		@Override
		public ExclusiveLock upgrade() throws IOException {
			// Can't find way to do it as atomic operation.
			// This may cause some glitches and creates attach area (gasp!)
			lock.release();
			final FileLock exclusiveLock = channel.tryLock();
			if (exclusiveLock == null) throw new IllegalStateException("Failed to re-lock file " + file);
			return new ExclusiveLock(file, channel, exclusiveLock);
		}

		@Override
		public boolean isShared() {
			return true;
		}
	}

	public static Optional<ExclusiveLock> tryExclusiveLock(File file) throws IOException {
		Closer closer = Closer.create();
		try {
			final RandomAccessFile s = closer.register(new RandomAccessFile(file, "rw"));
			final FileChannel ch = closer.register(s.getChannel());

			final FileLock lock = ch.tryLock();
			if (lock != null) return Optional.of(new ExclusiveLock(file, ch, lock));
		} catch (FileNotFoundException e) {
			// just let it skip, user has to retry
			Log.log(Level.DEBUG, e, "Failed to create or lock file %s, possible permission issue", file);
		} catch (Throwable t) {
			throw closer.rethrow(t);
		}

		closer.close();
		return Optional.absent();
	}

	public static ExclusiveLock exclusiveLock(File file) throws IOException {
		Closer closer = Closer.create();
		try {
			// using RandomAccessFile, since FileOutputStream can truncate even locked file
			final RandomAccessFile s = closer.register(new RandomAccessFile(file, "rw"));
			final FileChannel ch = closer.register(s.getChannel());

			final FileLock lock = ch.lock();
			return new ExclusiveLock(file, ch, lock);
		} catch (Throwable t) {
			throw closer.rethrow(t);
		}
	}

	public static Optional<SharedLock> trySharedLock(File file) throws IOException {
		Closer closer = Closer.create();
		try {
			final RandomAccessFile s = closer.register(new RandomAccessFile(file, "rw"));
			final FileChannel ch = closer.register(s.getChannel());

			final FileLock lock = ch.tryLock(0, Long.MAX_VALUE, true);
			if (lock != null) return Optional.of(new SharedLock(file, ch, lock));
		} catch (Throwable t) {
			throw closer.rethrow(t);
		}

		closer.close();
		return Optional.absent();
	}

	public static SharedLock sharedLock(File file) throws IOException {
		Closer closer = Closer.create();
		try {
			final RandomAccessFile s = closer.register(new RandomAccessFile(file, "rw"));
			final FileChannel ch = closer.register(s.getChannel());

			final FileLock lock = ch.lock(0, Long.MAX_VALUE, true);
			return new SharedLock(file, ch, lock);
		} catch (Throwable t) {
			throw closer.rethrow(t);
		}
	}
}
