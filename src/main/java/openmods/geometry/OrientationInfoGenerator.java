package openmods.geometry;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.vecmath.AxisAngle4f;
import net.minecraft.client.renderer.block.model.ModelRotation;
import openmods.block.BlockRotationMode;
import org.apache.commons.io.IOUtils;
import org.lwjgl.util.vector.Matrix4f;

// NOTE: this class is supposed to be called by Gradle task or manually
public class OrientationInfoGenerator {

	private static final File OUTPUT_DIR = new File(new File("etc"), "orientations");

	private static Matrix3d convertMatrix(org.lwjgl.util.vector.Matrix4f m) {
		Preconditions.checkState(m.m03 == 0);
		Preconditions.checkState(m.m13 == 0);
		Preconditions.checkState(m.m23 == 0);
		Preconditions.checkState(m.m33 == 1);
		return new Matrix3d(
				Math.round(m.m00), Math.round(m.m01), Math.round(m.m02),
				Math.round(m.m10), Math.round(m.m11), Math.round(m.m12),
				Math.round(m.m20), Math.round(m.m21), Math.round(m.m22));
	}

	private static Matrix3d convertMatrix(javax.vecmath.Matrix4f m) {
		Preconditions.checkState(m.m03 == 0);
		Preconditions.checkState(m.m13 == 0);
		Preconditions.checkState(m.m23 == 0);
		Preconditions.checkState(m.m33 == 1);
		return new Matrix3d(
				Math.round(m.m00), Math.round(m.m01), Math.round(m.m02),
				Math.round(m.m10), Math.round(m.m11), Math.round(m.m12),
				Math.round(m.m20), Math.round(m.m21), Math.round(m.m22));
	}

	private enum Rotation {
		R0(0), R90(90), R180(180), R270(270);

		public final String name;

		public final float angle;

		private Rotation(int angle) {
			this.angle = (float)Math.toRadians(angle);
			this.name = Integer.toString(angle);
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private static class XYZRotation implements Comparable<XYZRotation> {
		public final Rotation x;
		public final Rotation y;
		public final Rotation z;

		public XYZRotation(Rotation x, Rotation y, Rotation z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		private static int countComponent(Rotation r) {
			return r == Rotation.R0? 0 : 1;
		}

		private int componentCount() {
			return countComponent(x) + countComponent(y) + countComponent(z);
		}

		private float angleSum() {
			return x.angle + y.angle + z.angle;
		}

		@Override
		public int compareTo(XYZRotation o) {
			int result = Ints.compare(this.componentCount(), o.componentCount());
			if (result != 0) return result;

			return Floats.compare(this.angleSum(), o.angleSum());
		}

		@Override
		public String toString() {
			List<String> r = Lists.newArrayList();
			if (x != Rotation.R0) r.add("X" + x);
			if (y != Rotation.R0) r.add("Y" + y);
			if (z != Rotation.R0) r.add("Z" + z);
			return Joiner.on('_').join(r);
		}
	}

	private static List<XYZRotation> sorted(final Collection<XYZRotation> l) {
		final List<XYZRotation> tmp = Lists.newArrayList(l);
		Collections.sort(tmp);
		return tmp;
	}

	private static void dumpBlockRotationsRotations(BlockRotationMode brm, Multimap<Orientation, ModelRotation> vanilla, Multimap<Orientation, XYZRotation> xyz) throws IOException {
		final File outFile = new File(OUTPUT_DIR, brm.name().toLowerCase(Locale.ROOT) + ".txt");
		System.out.println("Generating file: " + outFile.getAbsolutePath());
		PrintWriter out = null;
		try {
			out = new PrintWriter(outFile);
			for (Orientation o : Sets.newTreeSet(brm.getValidDirections())) {
				final StringBuilder line = new StringBuilder();
				line.append('"');
				line.append(o.toString().toLowerCase(Locale.ROOT));
				line.append("\": {");
				final Collection<ModelRotation> v = vanilla.get(o);
				if (!v.isEmpty()) {
					final ModelRotation m = v.iterator().next();
					line.append(modelRotationToJson(m));
				} else {
					final XYZRotation f = sorted(xyz.get(o)).iterator().next();
					line.append(forgeRotationToJson(f));

				}
				line.append("}, // front: ");
				line.append(brm.getFront(o));
				line.append(", top: ");
				line.append(brm.getTop(o));
				out.println(line.toString());
			}
		} finally {
			IOUtils.closeQuietly(out);
		}
	}

	private static String forgeRotationToJson(XYZRotation f) {
		List<String> result = Lists.newArrayList();
		if (f.x != Rotation.R0) result.add("{\"x\": " + f.x.toString() + "}");
		if (f.y != Rotation.R0) result.add("{\"y\": " + f.y.toString() + "}");
		if (f.z != Rotation.R0) result.add("{\"z\": " + f.z.toString() + "}");

		return "{ \"transform\": { \"rotation\": [" + Joiner.on(", ").join(result) + "]}}";
	}

	private static Pattern namePattern = Pattern.compile("X(\\d+)_Y(\\d+)");

	private static String modelRotationToJson(ModelRotation m) {
		final Matcher matcher = namePattern.matcher(m.name());
		Preconditions.checkState(matcher.matches());
		final String x = matcher.group(1);
		final String y = matcher.group(2);

		List<String> result = Lists.newArrayList();
		if (!x.equals("0")) result.add("\"x\": " + x);
		if (!y.equals("0")) result.add("\"y\": " + y);
		return Joiner.on(", ").join(result);
	}

	private static Multimap<Orientation, XYZRotation> calculateXyzRotations(Map<Matrix3d, Orientation> fromMatrix) {
		final Multimap<Orientation, XYZRotation> toXYZRotation = HashMultimap.create();
		for (Rotation x : Rotation.values())
			for (Rotation y : Rotation.values())
				for (Rotation z : Rotation.values()) {
					final XYZRotation rotation = new XYZRotation(x, y, z);
					javax.vecmath.Matrix4f m = new javax.vecmath.Matrix4f();
					m.setIdentity();

					javax.vecmath.Matrix4f tmp = new javax.vecmath.Matrix4f();
					tmp.set(new AxisAngle4f(1, 0, 0, x.angle));
					m.mul(tmp);

					tmp.set(new AxisAngle4f(0, 1, 0, y.angle));
					m.mul(tmp);

					tmp.set(new AxisAngle4f(0, 0, 1, z.angle));
					m.mul(tmp);

					final Matrix3d key = convertMatrix(m);
					final Orientation orientation = fromMatrix.get(key);
					Preconditions.checkNotNull(orientation, rotation);
					toXYZRotation.put(orientation, rotation);
				}

		return toXYZRotation;
	}

	private static Multimap<Orientation, ModelRotation> calculateVanillaRotations(Map<Matrix3d, Orientation> fromMatrix) {
		final Multimap<Orientation, ModelRotation> toVanilla = HashMultimap.create();

		for (ModelRotation rot : ModelRotation.values()) {
			final Matrix4f rotMatrix = rot.getMatrix4d();
			final Matrix3d key = convertMatrix(rotMatrix);
			final Orientation orientation = fromMatrix.get(key);
			Preconditions.checkNotNull(orientation, rot);
			toVanilla.put(orientation, rot);
		}

		return toVanilla;
	}

	private static void dumpOrientations(Multimap<Orientation, ModelRotation> vanilla, Multimap<Orientation, XYZRotation> xyz) throws IOException {
		final File outFile = new File(OUTPUT_DIR, "all.txt");
		System.out.println("Generating file: " + outFile.getAbsolutePath());
		PrintWriter out = null;
		try {
			out = new PrintWriter(outFile);

			for (Orientation o : Orientation.VALUES) {
				out.println(String.format("%s -> XY: %s, XYZ: %s", o, vanilla.get(o), sorted(xyz.get(o))));
			}
		} finally

		{
			IOUtils.closeQuietly(out);
		}
	}

	public static void main(String[] args) throws IOException {
		OUTPUT_DIR.mkdirs();
		final Map<Matrix3d, Orientation> fromMatrix = Maps.newHashMap();

		for (Orientation o : Orientation.VALUES)
			fromMatrix.put(o.createTransformMatrix().invertCopy(), o);

		final Multimap<Orientation, ModelRotation> vanilla = calculateVanillaRotations(fromMatrix);

		final Multimap<Orientation, XYZRotation> xyz = calculateXyzRotations(fromMatrix);

		dumpOrientations(vanilla, xyz);

		for (BlockRotationMode brm : BlockRotationMode.values())
			dumpBlockRotationsRotations(brm, vanilla, xyz);
	}

}
