package openmods.renderer;

import com.google.common.base.Preconditions;

public class PreWorldRenderHookVisitor {

	// TODO 1.14 ASM hook

	private static boolean active;

	private static Runnable hook = () -> {};

	public static boolean isActive() {
		return active;
	}

	public static void setHook(Runnable hook) {
		Preconditions.checkNotNull(hook);
		PreWorldRenderHookVisitor.hook = hook;
	}
}
