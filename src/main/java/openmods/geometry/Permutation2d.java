package openmods.geometry;

import com.google.common.base.Preconditions;
import java.util.Arrays;

public class Permutation2d {
	public final int width;

	public final int height;

	private final int[] permutation;

	public Permutation2d(int width, int height, int... permutation) {
		Preconditions.checkArgument(permutation.length == width * height, "Not enough points: %s != %s*%s", permutation.length, width, height);
		this.width = width;
		this.height = height;
		this.permutation = permutation;
	}

	public int apply(int index) {
		return permutation[index];
	}

	public Permutation2d compose(Permutation2d inner) {
		Preconditions.checkArgument(inner.width == this.width, "Incompatible width: %s != %s", this.width, inner.width);
		Preconditions.checkArgument(inner.height == this.height, "Incompatible height: %s != %s", this.height, inner.height);

		final int count = this.permutation.length;
		final int[] newPermutation = new int[count];
		for (int i = 0; i < count; i++) {
			final int outerTransfrom = this.permutation[i];
			final int innerTransform = inner.permutation[outerTransfrom];
			newPermutation[i] = innerTransform;
		}

		return new Permutation2d(this.width, this.height, newPermutation);
	}

	public static Permutation2d identity(int width, int height) {
		final int[] permutation = new int[width * height];
		for (int i = 0; i < width * height; i++)
			permutation[i] = i;

		return new Permutation2d(width, height, permutation);
	}

	public Permutation2d mirrorVertical() {
		final int[] newPermutation = new int[width * height];
		int index = 0;
		for (int row = 0; row < height; row++)
			for (int column = width - 1; column >= 0; column--)
				newPermutation[index++] = permutation[row * width + column];

		return new Permutation2d(width, height, newPermutation);
	}

	public Permutation2d mirrorHorizontal() {
		final int[] newPermutation = new int[width * height];
		int index = 0;
		for (int row = height - 1; row >= 0; row--)
			for (int column = 0; column < width; column++)
				newPermutation[index++] = permutation[row * width + column];

		return new Permutation2d(width, height, newPermutation);
	}

	public Permutation2d transpose() {
		final int[] newPermutation = new int[width * height];
		int index = 0;
		for (int row = 0; row < width; row++)
			for (int column = 0; column < height; column++)
				newPermutation[index++] = permutation[column * width + row];

		return new Permutation2d(height, width, newPermutation);
	}

	public Permutation2d rotateCW() {
		return transpose().mirrorVertical();
	}

	public Permutation2d rotateCCW() {
		return transpose().mirrorHorizontal();
	}

	public Permutation2d reverse() {
		return mirrorHorizontal().mirrorVertical();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + height;
		result = prime * result + width;
		result = prime * result + Arrays.hashCode(permutation);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		if (obj instanceof Permutation2d) {
			Permutation2d other = (Permutation2d)obj;
			return this.width == other.width &&
					this.height == other.height &&
					Arrays.equals(this.permutation, other.permutation);
		}

		return false;
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		int index = 0;
		for (int row = 0; row < height; row++) {
			result.append('[');
			for (int column = 0; column < width; column++) {
				result.append(permutation[index++]);
				if (column != width - 1)
					result.append(' ');
			}
			result.append(']');
		}
		return result.toString();
	}
}
