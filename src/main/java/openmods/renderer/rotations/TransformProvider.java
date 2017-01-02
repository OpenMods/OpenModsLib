package openmods.renderer.rotations;

import com.google.common.collect.Maps;
import java.nio.FloatBuffer;
import java.util.Map;
import openmods.geometry.Matrix3d;
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
			final Matrix3d originalMatrix = orientation.createTransformMatrix();

			asMatrix = new Matrix4f();
			asMatrix.setIdentity();
			asMatrix.m00 = (float)originalMatrix.m00;
			asMatrix.m10 = (float)originalMatrix.m10;
			asMatrix.m20 = (float)originalMatrix.m20;

			asMatrix.m01 = (float)originalMatrix.m01;
			asMatrix.m11 = (float)originalMatrix.m11;
			asMatrix.m21 = (float)originalMatrix.m21;

			asMatrix.m02 = (float)originalMatrix.m02;
			asMatrix.m12 = (float)originalMatrix.m12;
			asMatrix.m22 = (float)originalMatrix.m22;

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
