package openmods.utils.render;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Matrix4f;

public class OpenGLUtils {

	private static FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

	public static synchronized void loadMatrix(Matrix4f transform) {
		transform.store(matrixBuffer);
		matrixBuffer.flip();
		GL11.glMultMatrix(matrixBuffer);
	}

}
