package openmods.utils.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import net.minecraft.util.math.vector.Vector3d;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class ProjectionHelper {

	private static final float[] IDENTITY_MATRIX = new float[] {
			1.0f, 0.0f, 0.0f, 0.0f,
			0.0f, 1.0f, 0.0f, 0.0f,
			0.0f, 0.0f, 1.0f, 0.0f,
			0.0f, 0.0f, 0.0f, 1.0f };

	private final FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
	private final FloatBuffer projection = BufferUtils.createFloatBuffer(16);
	private final FloatBuffer pmv = BufferUtils.createFloatBuffer(16);
	private final FloatBuffer ipmv = BufferUtils.createFloatBuffer(16);
	private final FloatBuffer temp = BufferUtils.createFloatBuffer(16);
	private final IntBuffer viewport = BufferUtils.createIntBuffer(4);

	public void updateMatrices() {
		GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, modelView);
		GL11.glGetFloatv(GL11.GL_PROJECTION_MATRIX, projection);
		GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);

		multMatrices(modelView, projection, pmv);
		invertMatrix(pmv, ipmv, temp);
	}

	private static void multMatrixVec(FloatBuffer m, float[] in, float[] out) {
		out[0] = in[0] * m.get(0 * 4 + 0)
				+ in[1] * m.get(1 * 4 + 0)
				+ in[2] * m.get(2 * 4 + 0)
				+ in[3] * m.get(3 * 4 + 0);

		out[1] = in[0] * m.get(0 * 4 + 1)
				+ in[1] * m.get(1 * 4 + 1)
				+ in[2] * m.get(2 * 4 + 1)
				+ in[3] * m.get(3 * 4 + 1);

		out[2] = in[0] * m.get(0 * 4 + 2)
				+ in[1] * m.get(1 * 4 + 2)
				+ in[2] * m.get(2 * 4 + 2)
				+ in[3] * m.get(3 * 4 + 2);

		out[3] = in[0] * m.get(0 * 4 + 3)
				+ in[1] * m.get(1 * 4 + 3)
				+ in[2] * m.get(2 * 4 + 3)
				+ in[3] * m.get(3 * 4 + 3);

	}

	private static void multMatrices(FloatBuffer a, FloatBuffer b, FloatBuffer r) {
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				r.put(i * 4 + j,
						a.get(i * 4 + 0) * b.get(0 * 4 + j)
								+ a.get(i * 4 + 1) * b.get(1 * 4 + j)
								+ a.get(i * 4 + 2) * b.get(2 * 4 + j)
								+ a.get(i * 4 + 3) * b.get(3 * 4 + j));
			}
		}
	}

	private static void invertMatrix(FloatBuffer src, FloatBuffer inverse, FloatBuffer temp) {
		temp.position(0);
		src.position(0);
		temp.put(src);

		inverse.position(0);
		inverse.put(IDENTITY_MATRIX);

		for (int i = 0; i < 4; i++) {
			int swap = i;
			for (int j = i + 1; j < 4; j++) {
				if (Math.abs(temp.get(j * 4 + i)) > Math.abs(temp.get(i * 4 + i))) {
					swap = j;
				}
			}

			if (swap != i) {
				for (int k = 0; k < 4; k++) {
					float t = temp.get(i * 4 + k);
					temp.put(i * 4 + k, temp.get(swap * 4 + k));
					temp.put(swap * 4 + k, t);

					t = inverse.get(i * 4 + k);
					inverse.put(i * 4 + k, inverse.get(swap * 4 + k));
					inverse.put(swap * 4 + k, t);
				}
			}

			if (temp.get(i * 4 + i) == 0) {
				return;
			}

			float t = temp.get(i * 4 + i);
			for (int k = 0; k < 4; k++) {
				temp.put(i * 4 + k, temp.get(i * 4 + k) / t);
				inverse.put(i * 4 + k, inverse.get(i * 4 + k) / t);
			}
			for (int j = 0; j < 4; j++) {
				if (j != i) {
					t = temp.get(j * 4 + i);
					for (int k = 0; k < 4; k++) {
						temp.put(j * 4 + k, temp.get(j * 4 + k) - temp.get(i * 4 + k) * t);
						inverse.put(j * 4 + k, inverse.get(j * 4 + k) - inverse.get(i * 4 + k) * t);
					}
				}
			}
		}
	}

	public Vector3d project(float x, float y, float z) {
		float[] in = new float[] { x, y, z, 1.0f };
		float[] out = new float[4];

		multMatrixVec(pmv, in, out);

		if (in[3] == 0.0)
			return Vector3d.ZERO;

		in[3] = (1.0f / in[3]) * 0.5f;

		in[0] = in[0] * in[3] + 0.5f;
		in[1] = in[1] * in[3] + 0.5f;
		in[2] = in[2] * in[3] + 0.5f;

		float wx = in[0] * viewport.get(2) + viewport.get(0);
		float wy = in[1] * viewport.get(3) + viewport.get(1);
		float wz = in[2];
		return new Vector3d(wx, wy, wz);
	}

	public Vector3d unproject(float x, float y, float z) {
		float[] in = new float[] { x, y, z, 1.0f };
		float[] out = new float[4];

		in[0] = (in[0] - viewport.get(0)) / viewport.get(2);
		in[1] = (in[1] - viewport.get(1)) / viewport.get(3);

		in[0] = in[0] * 2 - 1;
		in[1] = in[1] * 2 - 1;
		in[2] = in[2] * 2 - 1;

		multMatrixVec(ipmv, in, out);

		if (out[3] == 0.0)
			return Vector3d.ZERO;

		out[3] = 1.0f / out[3];

		final float ox = out[0] * out[3];
		final float oy = out[1] * out[3];
		final float oz = out[2] * out[3];
		return new Vector3d(ox, oy, oz);
	}
}