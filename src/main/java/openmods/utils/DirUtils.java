package openmods.utils;

import java.util.Iterator;

import net.minecraft.util.EnumFacing;
import openmods.utils.ByteUtils.CountingBitIterator;

public class DirUtils {

	private static class DirectionBitsetIterator extends CountingBitIterator<EnumFacing> {
		public DirectionBitsetIterator(int value) {
			super(value);
		}

		@Override
		protected EnumFacing convert(int bit) {
			return EnumFacing.VALUES[bit];
		}
	}

	public static Iterator<EnumFacing> bitsToValidDirs(int value) {
		return new DirectionBitsetIterator(value & 0x3F);
	}

	public static Iterator<EnumFacing> bitsToAllDirs(int value) {
		return new DirectionBitsetIterator(value & 0x7F);
	}
}
