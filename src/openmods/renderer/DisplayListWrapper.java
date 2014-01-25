package openmods.renderer;

import org.lwjgl.opengl.GL11;

public abstract class DisplayListWrapper {

	private Integer displayList;

	@Override
	protected void finalize() {
		if (displayList != null) GL11.glDeleteLists(displayList, 1);
	}
	
	public boolean isCompiled() {
		return displayList != null;
	}

	public void render() {
		if (displayList == null) {
			displayList = GL11.glGenLists(1);
			GL11.glNewList(displayList, GL11.GL_COMPILE);
			compile();
			GL11.glEndList();
		}
		
		GL11.glCallList(displayList);
	}
	
	public abstract void compile();
}
