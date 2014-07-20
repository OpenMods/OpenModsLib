package openmods.utils;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Set;

import net.minecraftforge.common.util.ForgeDirection;
import openmods.utils.ByteUtils.CountingBitIterator;

public class DirUtils {

	public static final Set<ForgeDirection> VALID_DIRECTIONS = EnumSet.complementOf(EnumSet.of(ForgeDirection.UNKNOWN));

	private static class DirectionBitsetIterator extends CountingBitIterator<ForgeDirection> {
		public DirectionBitsetIterator(int value) {
			super(value);
		}

		@Override
		protected ForgeDirection convert(int bit) {
			return ForgeDirection.getOrientation(bit);
		}
	}

	public static Iterator<ForgeDirection> bitsToValidDirs(int value) {
		return new DirectionBitsetIterator(value & 0x3F);
	}

	public static Iterator<ForgeDirection> bitsToAllDirs(int value) {
		return new DirectionBitsetIterator(value & 0x7F);
	}
}
