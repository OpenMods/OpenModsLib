package openmods.renderer.rotations;

import com.google.common.collect.Maps;
import java.nio.FloatBuffer;
import java.util.Map;
import net.minecraftforge.common.model.TRSRTransformation;
import openmods.geometry.Orientation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;

public class TransformProvider {

	public static final TransformProvider instance = new TransformProvider();

	private static class Transformation {
		private final Matrix4f asMatrix;
		private final Matrix4f asInverseMatrix;
		private final FloatBuffer asBuffer;
		private final FloatBuffer asInverseBuffer;

		public Transformation(Orientation orientation) {
			final javax.vecmath.Matrix4f originalMatrix = new javax.vecmath.Matrix4f();
			originalMatrix.set(orientation.getLocalToWorldMatrix());

			asMatrix = TRSRTransformation.toLwjgl(originalMatrix);

			asBuffer = BufferUtils.createFloatBuffer(16);
			asMatrix.store(asBuffer);
			asBuffer.rewind();

			asInverseMatrix = new Matrix4f();
			Matrix4f.invert(asMatrix, asInverseMatrix);

			asInverseBuffer = BufferUtils.createFloatBuffer(16);
			asInverseMatrix.store(asInverseBuffer);
			asInverseBuffer.rewind();
		}
	}

	private final Map<Orientation, Transformation> transformations = Maps.newEnumMap(Orientation.class);

	private TransformProvider() {

	}

	private Transformation getTransformation(Orientation orientation) {
		Transformation r = transformations.get(orientation);
		if (r == null) {
			r = new Transformation(orientation);
			transformations.put(orientation, r);
		}

		return r;
	}

	public Matrix4f getMatrixForOrientation(Orientation orientation) {
		final Transformation transformation = getTransformation(orientation);
		return new Matrix4f(transformation.asMatrix);
	}

	public Matrix4f getInverseMatrixForOrientation(Orientation orientation) {
		final Transformation transformation = getTransformation(orientation);
		return new Matrix4f(transformation.asInverseMatrix);
	}

	public void multMatrix(Orientation orientation) {
		final Transformation transformation = getTransformation(orientation);
		GL11.glMultMatrix(transformation.asBuffer);
		transformation.asBuffer.rewind();
	}

	public void multInverseMatrix(Orientation orientation) {
		final Transformation transformation = getTransformation(orientation);
		GL11.glMultMatrix(transformation.asInverseBuffer);
		transformation.asInverseBuffer.rewind();
	}
}
