package openmods;

import java.util.*;

import net.minecraft.launchwrapper.IClassTransformer;
import openmods.api.IResultListener;
import openmods.asm.*;
import openmods.asm.VisitorHelper.TransformProvider;
import openmods.config.simple.ConfigProcessor;
import openmods.config.simple.ConfigProcessor.UpdateListener;
import openmods.context.ContextClassTransformer;
import openmods.include.IncludingClassVisitor;
import openmods.movement.MovementPatcher;
import openmods.renderer.PlayerRendererHookVisitor;
import openmods.stencil.CapabilitiesHookInjector;
import openmods.stencil.FramebufferInjector;
import openmods.utils.StateTracker;
import openmods.utils.StateTracker.StateUpdater;
import openmods.world.MapGenStructureVisitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Throwables;
import com.google.common.collect.*;

import cpw.mods.fml.common.discovery.ASMDataTable;
import cpw.mods.fml.common.discovery.ASMDataTable.ASMData;

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
			"tv.twitch."
			);

	private static final Multimap<String, MethodMatcher> wrapMatchers = ArrayListMultimap.create();

	private static void addWrapTarget(String clsName, String methodMcpName, String methodSrgName, String methodDesc) {
		MappedType cls = MappedType.of(clsName);
		MethodMatcher matcher = new MethodMatcher(cls, methodDesc, methodMcpName, methodSrgName);
		wrapMatchers.put(clsName, matcher);
	}

	static {
		addWrapTarget("net.minecraft.server.management.ItemInWorldManager", "tryHarvestBlock", "func_73084_b", "(III)Z");
	}

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
						Log.info("Trying to apply movement callback (class: %s)", name);
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

		config.addEntry("activate_map_gen_fix", 0, "true", new ConfigOption("map_gen_fix") {
			@Override
			protected void onActivate(final StateUpdater<TransformerState> state) {
				vanillaPatches.put("net.minecraft.world.gen.structure.MapGenStructure", new TransformProvider(ClassWriter.COMPUTE_FRAMES) {
					@Override
					public ClassVisitor createVisitor(String name, ClassVisitor cv) {
						Log.info("Trying to patch MapGenStructure (class: %s)", name);
						state.update(TransformerState.ACTIVATED);
						return new MapGenStructureVisitor(name, cv, createResultListener(state));
					}
				});
			}
		},
				"Purpose: fix bug in vanilla code used to find nearby structures",
				"Modified class: net.minecraft.world.gen.structure.MapGenStructure",
				"Known users: OpenBlocks golden eye",
				"When disabled: features may not work (either silently fail or cause crash)");

		config.addEntry("activate_player_render_hook", 0, "true", new ConfigOption("player_render_hook") {
			@Override
			protected void onActivate(final StateUpdater<TransformerState> state) {
				vanillaPatches.put("net.minecraft.client.renderer.entity.RenderPlayer", new TransformProvider(ClassWriter.COMPUTE_FRAMES) {
					@Override
					public ClassVisitor createVisitor(String name, ClassVisitor cv) {
						Log.info("Trying to patch RenderPlayer (class: %s)", name);
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

		config.addEntry("activate_stencil_patches", 0, "true", new ConfigOption("stencil_patches") {
			@Override
			protected void onActivate(final StateUpdater<TransformerState> state) {
				vanillaPatches.put("net.minecraft.client.shader.Framebuffer", new TransformProvider(ClassWriter.COMPUTE_FRAMES) {
					@Override
					public ClassVisitor createVisitor(String name, ClassVisitor cv) {
						Log.info("Trying to patch Framebuffer (class: %s)", name);
						state.update(TransformerState.ACTIVATED);
						return new FramebufferInjector(name, cv, createResultListener(state));
					}
				});
			}
		},
				"Purpose: to re-enable stencil buffer on FBO objects. This is was disabled due to problems on some configurations",
				"Modified class: net.minecraft.client.shader.Framebuffer",
				"Known users: OpenBlocks skyblocks",
				"When disabled: no stencil buffer available unless unlocked with Forge flag. Mods may not use some graphic features");

		config.addEntry("activate_gl_capabilities_hook", 0, "true", new ConfigOption("gl_capabilities_hook") {
			@Override
			protected void onActivate(final StateUpdater<TransformerState> state) {
				vanillaPatches.put("net.minecraft.client.renderer.OpenGlHelper", new TransformProvider(ClassWriter.COMPUTE_FRAMES) {
					@Override
					public ClassVisitor createVisitor(String name, ClassVisitor cv) {
						Log.info("Trying to patch OpenGlHelper (class: %s)", name);
						state.update(TransformerState.ACTIVATED);
						return new CapabilitiesHookInjector(name, cv, createResultListener(state));
					}
				});
			}
		},
				"Purpose: hook to get check additional OpenGL capabilities (mostly stencil buffer related)",
				"Modified class: net.minecraft.client.renderer.OpenGlHelper",
				"Known users: OpenBlocks skyblocks",
				"When disabled: no stencil buffer available unless unlocked with Forge flag. Mods may not use some graphic features");
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
	public byte[] transform(final String name, String transformedName, byte[] bytes) {
		if (bytes == null) return bytes;

		bytes = tryApplyContextWrapper(name, transformedName, bytes);

		if (transformedName.startsWith("net.minecraft.")) {
			TransformProvider provider = vanillaPatches.get(transformedName);
			return (provider != null)? VisitorHelper.apply(bytes, name, provider) : bytes;
		}

		if (shouldTryIncluding(transformedName)) return applyIncludes(name, transformedName, bytes);

		return bytes;
	}

	protected byte[] tryApplyContextWrapper(final String name, String transformedName, byte[] bytes) {
		final Collection<MethodMatcher> matchers = wrapMatchers.get(transformedName);
		if (matchers.isEmpty()) return bytes;

		Log.info("Adding context wrappers to %s(%s)", name, transformedName);
		return VisitorHelper.apply(bytes, name, new TransformProvider(0) {
			@Override
			public ClassVisitor createVisitor(String name, ClassVisitor cv) {
				return new ContextClassTransformer(cv, matchers);
			}
		});
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
