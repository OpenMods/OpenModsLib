package openmods.renderer;

import org.lwjgl.opengl.GL11;

public abstract class DisplayListWrapper {

	private int displayList;

	private boolean isValid;

	private boolean pendingInvalidate;

	public boolean isCompiled() {
		return isValid;
	}

	public void render() {
		if (pendingInvalidate) reset();

		if (!isValid) {
			displayList = GL11.glGenLists(1);
			GL11.glNewList(displayList, GL11.GL_COMPILE);
			compile();
			GL11.glEndList();
			isValid = true;
		}

		GL11.glCallList(displayList);
	}

	public abstract void compile();

	/**
	 * WARNING: this method can be only used in client (rendering) thread. If not possible, use {@link #invalidate()}.
	 */
	public void reset() {
		if (isValid) GL11.glDeleteLists(displayList, 1);
		pendingInvalidate = isValid = false;
	}

	public void invalidate() {
		pendingInvalidate = true;
	}
}
