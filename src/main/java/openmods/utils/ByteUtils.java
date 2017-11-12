package openmods.utils;

import com.google.common.base.Preconditions;
import com.google.common.collect.UnmodifiableIterator;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;

public class ByteUtils {

	public abstract static class BitIterator extends UnmodifiableIterator<Boolean> {
		private int value;

		public BitIterator(int value) {
			this.value = value;
		}

		@Override
		public boolean hasNext() {
			return value != 0;
		}

		@Override
		public Boolean next() {
			boolean result = (value & 1) != 0;
			value >>= 1;
			return result;
		}
	}

	public abstract static class CountingBitIterator<T> extends UnmodifiableIterator<T> {
		private int value;
		private int count;

		public CountingBitIterator(int value) {
			this.value = value;
		}

		protected abstract T convert(int bit);

		@Override
		public boolean hasNext() {
			return value != 0;
		}

		@Override
		public T next() {
			while (value != 0) {
				final boolean result = (value & 1) != 0;

				value >>= 1;

				if (result) return convert(count++);
				else count++;
			}
			throw new IllegalStateException();
		}
	}

	public static int on(int val, int bit) {
		return val | (1 << bit);
	}

	public static int off(int val, int bit) {
		return val & ~(1 << bit);
	}

	public static int set(int val, int bit, boolean flag) {
		return flag? (val | (1 << bit)) : (val & ~(1 << bit));
	}

	public static long set(long val, int bit, boolean flag) {
		return flag? (val | (1 << bit)) : (val & ~(1 << bit));
	}

	public static boolean get(int val, int slot) {
		return (val & (1 << slot)) != 0;
	}

	public static boolean get(long val, int slot) {
		return (val & (1 << slot)) != 0;
	}

	public static void writeVLI(DataOutput output, int value) {
		// I'm not touching signed integers.
		Preconditions.checkArgument(value >= 0, "Value cannot be negative");

		try {
			while (true) {
				int b = value & 0x7F;
				int next = value >> 7;
				if (next > 0) {
					b |= 0x80;
					output.writeByte(b);
					value = next;
				} else {
					output.writeByte(b);
					break;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static int readVLI(DataInput input) {
		int result = 0;
		int shift = 0;
		int b;
		try {
			do {
				b = input.readByte();
				result = result | ((b & 0x7F) << shift);
				shift += 7;
			} while (b < 0);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return result;
	}

	public static int nextPowerOf2(int v) {
		v--;
		v |= v >> 1;
		v |= v >> 2;
		v |= v >> 4;
		v |= v >> 8;
		v |= v >> 16;
		v++;

		return v;
	}

	public static boolean isPowerOfTwo(int size) {
		return (size & (size - 1)) == 0;
	}

	public static int enumSetToBits(Set<? extends Enum<?>> dirs) {
		int value = 0;

		for (Enum<?> e : dirs) {
			final int bit = e.ordinal();
			Preconditions.checkArgument(bit < Integer.SIZE, "Enum %s has too many values", e.getClass());
			value = on(value, bit);
		}

		return value;
	}
}
