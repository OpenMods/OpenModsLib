package openmods.binding;

import net.minecraft.client.settings.KeyBinding;

public abstract class ActionBind {
	public abstract KeyBinding createBinding();

	public boolean isRepeatable() {
		return false;
	}

	public abstract void keyDown(boolean tickEnd, boolean isRepeat);

	public void keyUp(boolean tickEnd) {}
}
