package openmods.renderer;

import com.google.common.base.Preconditions;
import org.lwjgl.opengl.GL11;

public class ManualDisplayList {

	public interface Renderer {
		void render();
	}

	private int displayList;

	private boolean isAllocated;

	private boolean isValid;

	public boolean isCompiled() {
		return isValid;
	}

	public void render() {
		Preconditions.checkState(isValid, "Display list not initialized");
		GL11.glCallList(displayList);
	}

	public void compile(Renderer renderer) {
		if (isAllocated) GL11.glDeleteLists(displayList, 1);

		displayList = GL11.glGenLists(1);
		GL11.glNewList(displayList, GL11.GL_COMPILE);
		renderer.render();
		GL11.glEndList();

		isAllocated = isValid = true;
	}

	public void invalidate() {
		isValid = false;
	}

}
