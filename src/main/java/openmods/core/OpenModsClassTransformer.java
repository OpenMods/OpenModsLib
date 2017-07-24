package openmods.core;

import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.common.discovery.ASMDataTable;
import net.minecraftforge.fml.common.discovery.ASMDataTable.ASMData;
import openmods.Log;
import openmods.api.IResultListener;
import openmods.asm.TransformerState;
import openmods.asm.VisitorHelper;
import openmods.asm.VisitorHelper.TransformProvider;
import openmods.config.simple.ConfigProcessor;
import openmods.config.simple.ConfigProcessor.UpdateListener;
import openmods.entity.PlayerDamageEventInjector;
import openmods.include.IncludingClassVisitor;
import openmods.movement.MovementPatcher;
import openmods.renderer.PlayerRendererHookVisitor;
import openmods.renderer.PreWorldRenderHookVisitor;
import openmods.utils.StateTracker;
import openmods.utils.StateTracker.StateUpdater;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class OpenModsClassTransformer implements IClassTransformer {

	private static OpenModsClassTransformer INSTANCE;

	private static final List<String> IGNORED_PREFIXES = ImmutableList.of(
			"cpw.mods.fml.",
			"net.minecraftforge.",
			"io.netty.",
			"gnu.trove.",
			"com.google.",
			"com.mojang.",
			"joptsimple.",
			"tv.twitch.");

	private final Map<String, TransformProvider> vanillaPatches = Maps.newHashMap();

	private final StateTracker<TransformerState> states = StateTracker.create(TransformerState.DISABLED);

	private Set<String> includedClasses;

	private abstract class ConfigOption implements UpdateListener {

		private final StateUpdater<TransformerState> state;

		public ConfigOption(String name) {
			state = states.register(name);
		}

		@Override
		public void valueSet(String value) {
			if ("true".equalsIgnoreCase(value)) {
				state.update(TransformerState.ENABLED);
				onActivate(state);
			}
		}

		protected abstract void onActivate(StateUpdater<TransformerState> state);
	}

	private static IResultListener createResultListener(final StateUpdater<TransformerState> updater) {
		return new IResultListener() {
			@Override
			public void onSuccess() {
				updater.update(TransformerState.FINISHED);
			}

			@Override
			public void onFailure() {
				updater.update(TransformerState.FAILED);
			}
		};
	}

	public OpenModsClassTransformer() {
		INSTANCE = this;
	}

	public static OpenModsClassTransformer instance() {
		return INSTANCE;
	}

	public void addConfigValues(ConfigProcessor config) {
		config.addEntry("activate_movement_callback", 0, "true", new ConfigOption("movement_callback") {
			@Override
			protected void onActivate(final StateUpdater<TransformerState> state) {
				vanillaPatches.put("net.minecraft.client.entity.EntityPlayerSP", new TransformProvider(ClassWriter.COMPUTE_FRAMES) {
					@Override
					public ClassVisitor createVisitor(String name, ClassVisitor cv) {
						Log.debug("Trying to apply movement callback (class: %s)", name);
						state.update(TransformerState.ACTIVATED);
						return new MovementPatcher(name, cv, createResultListener(state));
					}
				});
			}
		},
				"Purpose: this transformer add hook to player movement controls",
				"Modified class: net.minecraft.client.entity.EntityPlayerSP",
				"Known users: OpenBlocks elevator",
				"When disabled: users usually have fallbacks (elevator will use less accurate algorithm)");

		config.addEntry("activate_player_render_hook", 0, "true", new ConfigOption("player_render_hook") {
			@Override
			protected void onActivate(final StateUpdater<TransformerState> state) {
				vanillaPatches.put("net.minecraft.client.renderer.entity.RenderPlayer", new TransformProvider(ClassWriter.COMPUTE_FRAMES) {
					@Override
					public ClassVisitor createVisitor(String name, ClassVisitor cv) {
						Log.debug("Trying to apply player render hook (class: %s)", name);
						state.update(TransformerState.ACTIVATED);
						return new PlayerRendererHookVisitor(name, cv, createResultListener(state));
					}
				});
			}
		},
				"Purpose: add hook to player rendering code",
				"Modified class: net.minecraft.client.renderer.entity.RenderPlayer",
				"Known users: OpenBlocks hangglider",
				"When disabled: code may fallback to less compatible mechanism (like replacing renderer)");

		config.addEntry("activate_player_damage_hook", 0, "true", new ConfigOption("player_damage_hook") {
			@Override
			protected void onActivate(final StateUpdater<TransformerState> state) {
				vanillaPatches.put("net.minecraft.entity.player.EntityPlayer", new TransformProvider(ClassWriter.COMPUTE_FRAMES) {
					@Override
					public ClassVisitor createVisitor(String name, ClassVisitor cv) {
						Log.debug("Trying to patch EntityPlayer (class: %s)", name);
						state.update(TransformerState.ACTIVATED);
						return new PlayerDamageEventInjector(name, cv, createResultListener(state));
					}
				});
			}
		},
				"Purpose: hook for capturing damage to player (after armor and potion calculations)",
				"Modified class: net.minecraft.entity.player.EntityPlayer",
				"Known users: Last Stand enchantment",
				"When disabled: Last Stand enchantment will not work");

		config.addEntry("hook_pre_world_rendering", 0, "true", new ConfigOption("pre_world_render_hook") {
			@Override
			protected void onActivate(final StateUpdater<TransformerState> state) {
				vanillaPatches.put("net.minecraft.client.renderer.EntityRenderer", new TransformProvider(0) {
					@Override
					public ClassVisitor createVisitor(String name, ClassVisitor cv) {
						Log.debug("Trying to patch EntityRenderer (class: %s)", name);
						state.update(TransformerState.ACTIVATED);
						return new PreWorldRenderHookVisitor(name, cv, createResultListener(state));
					}
				});
			}

		});
	}

	private final static TransformProvider INCLUDING_CV = new TransformProvider(0) {
		@Override
		public ClassVisitor createVisitor(String name, ClassVisitor cv) {
			return new IncludingClassVisitor(cv);
		}
	};

	public void injectAsmData(ASMDataTable table) {
		ImmutableSet.Builder<String> includedClasses = ImmutableSet.builder();

		for (ASMData data : table.getAll("openmods.include.IncludeInterface"))
			includedClasses.add(data.getClassName());

		for (ASMData data : table.getAll("openmods.include.IncludeOverride"))
			includedClasses.add(data.getClassName());

		this.includedClasses = includedClasses.build();
	}

	private boolean shouldTryIncluding(String clsName) {
		if (includedClasses != null) return includedClasses.contains(clsName);

		for (String prefix : IGNORED_PREFIXES)
			if (clsName.startsWith(prefix)) return false;

		return true;
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] bytes) {
		if (bytes == null) return null;

		if (transformedName.startsWith("net.minecraft.")) {
			TransformProvider provider = vanillaPatches.get(transformedName);
			return (provider != null)? VisitorHelper.apply(bytes, name, provider) : bytes;
		}

		if (shouldTryIncluding(transformedName)) return applyIncludes(name, transformedName, bytes);

		return bytes;
	}

	protected byte[] applyIncludes(final String name, String transformedName, byte[] bytes) {
		try {
			return VisitorHelper.apply(bytes, name, INCLUDING_CV);
		} catch (Throwable t) {
			Log.severe(t, "Failed to apply including transformer on %s(%s)", name, transformedName);
			throw Throwables.propagate(t);
		}
	}

	public String listStates() {
		return Joiner.on(',').join(Iterables.transform(states.states(), Functions.toStringFunction()));
	}
}
