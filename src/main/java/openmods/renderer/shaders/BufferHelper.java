package openmods.renderer.shaders;

import java.nio.ByteBuffer;
import org.lwjgl.opengl.ARBVertexBufferObject;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GLCapabilities;

public class BufferHelper {
	static {
		initialize();
	}

	public static IBufferMethods methods;

	static void initialize() {
		GLCapabilities caps = GL.getCapabilities();
		if (GLBufferMethods.isSupported(caps)) methods = new GLBufferMethods();
		else if (ARBBufferMethods.isSupported(caps)) methods = new ARBBufferMethods();
	}

	public static boolean isSupported() {
		return methods != null;
	}

	public static IBufferMethods methods() {
		return methods;
	}

	public interface IBufferMethods {
		int glGenBuffers();

		void glBindBuffer(int target, int buffer);

		void glBufferData(int target, ByteBuffer data, int usage);

		void glDeleteBuffers(int buffer);
	}

	private static class GLBufferMethods implements IBufferMethods {

		public static boolean isSupported(GLCapabilities caps) {
			return caps.OpenGL15;
		}

		@Override
		public int glGenBuffers() {
			return GL15.glGenBuffers();
		}

		@Override
		public void glBindBuffer(int target, int buffer) {
			GL15.glBindBuffer(target, buffer);
		}

		@Override
		public void glBufferData(int target, ByteBuffer data, int usage) {
			GL15.glBufferData(target, data, usage);
		}

		@Override
		public void glDeleteBuffers(int buffer) {
			GL15.glDeleteBuffers(buffer);
		}
	}

	private static class ARBBufferMethods implements IBufferMethods {

		public static boolean isSupported(GLCapabilities caps) {
			return caps.GL_ARB_vertex_buffer_object;
		}

		@Override
		public int glGenBuffers() {
			return ARBVertexBufferObject.glGenBuffersARB();
		}

		@Override
		public void glBindBuffer(int target, int buffer) {
			ARBVertexBufferObject.glBindBufferARB(target, buffer);
		}

		@Override
		public void glBufferData(int target, ByteBuffer data, int usage) {
			ARBVertexBufferObject.glBufferDataARB(target, data, usage);
		}

		@Override
		public void glDeleteBuffers(int buffer) {
			ARBVertexBufferObject.glDeleteBuffersARB(buffer);
		}
	}
}