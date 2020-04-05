package openmods.utils.render;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.mojang.blaze3d.platform.GLX;
import java.nio.FloatBuffer;
import java.util.Set;
import javax.vecmath.Matrix4f;
import net.minecraftforge.common.model.TRSRTransformation;
import openmods.Log;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

public class OpenGLUtils {

	private static final FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);

	public static synchronized void loadMatrix(Matrix4f transform) {
		net.minecraft.client.renderer.Matrix4f m = TRSRTransformation.toMojang(transform);
		m.write(matrixBuffer);
		matrixBuffer.flip();
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
