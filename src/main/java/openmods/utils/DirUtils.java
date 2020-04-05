package openmods.utils;

import java.util.Iterator;
import net.minecraft.util.Direction;
import openmods.utils.ByteUtils.CountingBitIterator;

public class DirUtils {

	private static class DirectionBitsetIterator extends CountingBitIterator<Direction> {
		public DirectionBitsetIterator(int value) {
			super(value);
		}

		@Override
		protected Direction convert(int bit) {
			return Direction.values()[bit];
		}
	}

	public static Iterator<Direction> bitsToValidDirs(int value) {
		return new DirectionBitsetIterator(value & 0x3F);
	}

	public static Iterator<Direction> bitsToAllDirs(int value) {
		return new DirectionBitsetIterator(value & 0x7F);
	}
}
