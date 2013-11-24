package openmods.binding;

import java.util.List;
import java.util.Map;

import net.minecraft.client.settings.KeyBinding;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class KeyDispatcherBuilder {

	private final List<ActionBind> bindings = Lists.newArrayList();

	public KeyDispatcherBuilder addBinding(ActionBind binding) {
		bindings.add(binding);
		return this;
	}

	public KeyDispatcher build() {
		Map<KeyBinding, ActionBind> bindingMap = Maps.newIdentityHashMap();
		KeyBinding[] bindingsArray = new KeyBinding[bindings.size()];
		boolean repeatings[] = new boolean[bindings.size()];
		for (int i = 0; i < bindings.size(); i++) {
			ActionBind action = bindings.get(i);
			KeyBinding binding = action.createBinding();
			bindingMap.put(binding, action);
			bindingsArray[i] = binding;
			repeatings[i] = action.isRepeatable();
		}

		return new KeyDispatcher(bindingsArray, repeatings, bindingMap);
	}
}
