package openmods.binding;

import java.util.EnumSet;
import java.util.Map;

import net.minecraft.client.settings.KeyBinding;
import cpw.mods.fml.client.registry.KeyBindingRegistry.KeyHandler;
import cpw.mods.fml.common.TickType;

public class KeyDispatcher extends KeyHandler {

	private final Map<KeyBinding, ActionBind> bindings;

	KeyDispatcher(KeyBinding[] keyBindings, boolean[] repeatings, Map<KeyBinding, ActionBind> bindings) {
		super(keyBindings, repeatings);
		this.bindings = bindings;
	}

	@Override
	public String getLabel() {
		return "OpenModsKeyHandler";
	}

	@Override
	public void keyDown(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd, boolean isRepeat) {
		ActionBind binding = bindings.get(kb);
		if (binding != null) binding.keyDown(tickEnd, isRepeat);
	}

	@Override
	public void keyUp(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd) {
		ActionBind binding = bindings.get(kb);
		if (binding != null) binding.keyUp(tickEnd);
	}

	@Override
	public EnumSet<TickType> ticks() {
		return EnumSet.of(TickType.CLIENT);
	}

}