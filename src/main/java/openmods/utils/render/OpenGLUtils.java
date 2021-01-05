package openmods.utils.render;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.mojang.blaze3d.platform.GLX;
import java.nio.FloatBuffer;
import java.util.Set;
import javax.vecmath.Matrix4f;
import openmods.Log;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class OpenGLUtils {

	private static final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

	private static int bufferIndex(int column, int row) {
		return row * 4 + column;
	}

	public static synchronized void loadMatrix(Matrix4f m) {
		matrixBuffer.put(bufferIndex(0, 0), m.m00);
		matrixBuffer.put(bufferIndex(0, 1), m.m01);
		matrixBuffer.put(bufferIndex(0, 2), m.m02);
		matrixBuffer.put(bufferIndex(0, 3), m.m03);
		matrixBuffer.put(bufferIndex(1, 0), m.m10);
		matrixBuffer.put(bufferIndex(1, 1), m.m11);
		matrixBuffer.put(bufferIndex(1, 2), m.m12);
		matrixBuffer.put(bufferIndex(1, 3), m.m13);
		matrixBuffer.put(bufferIndex(2, 0), m.m20);
		matrixBuffer.put(bufferIndex(2, 1), m.m21);
		matrixBuffer.put(bufferIndex(2, 2), m.m22);
		matrixBuffer.put(bufferIndex(2, 3), m.m23);
		matrixBuffer.put(bufferIndex(3, 0), m.m30);
		matrixBuffer.put(bufferIndex(3, 1), m.m31);
		matrixBuffer.put(bufferIndex(3, 2), m.m32);
		matrixBuffer.put(bufferIndex(3, 3), m.m33);
		matrixBuffer.rewind();
		GL11.glMultMatrixf(matrixBuffer);
	}

	public static Set<Integer> getGlErrors() {
		int glError = GL11.glGetError();

		// early return, to skip allocation
		if (glError == GL11.GL_NO_ERROR) return ImmutableSet.of();

		ImmutableSet.Builder<Integer> result = ImmutableSet.builder();
		do {
			result.add(glError);
		} while ((glError = GL11.glGetError()) != GL11.GL_NO_ERROR);

		return result.build();
	}

	public static void flushGlErrors(String location) {
		Set<Integer> errors = getGlErrors();
		if (!errors.isEmpty()) Log.warn("OpenGl errors detected in '%s': [%s]", location, errorsToString(errors));
	}

	@SuppressWarnings("null")
	public static String errorsToString(Iterable<Integer> errors) {
		return Joiner.on(',').join(Iterables.transform(errors, GLX::getErrorString));
	}
}
