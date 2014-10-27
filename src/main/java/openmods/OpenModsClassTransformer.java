package openmods;

import java.util.Map;

import net.minecraft.launchwrapper.IClassTransformer;
import openmods.asm.VisitorHelper;
import openmods.asm.VisitorHelper.TransformProvider;
import openmods.include.IncludingClassVisitor;
import openmods.movement.MovementPatcher;
import openmods.renderer.PlayerRendererHookVisitor;
import openmods.stencil.CapabilitiesHookInjector;
import openmods.stencil.FramebufferInjector;
import openmods.world.MapGenStructureVisitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;

public class OpenModsClassTransformer implements IClassTransformer {

	private static boolean applyMovementTransformer = System.getProperty("openmods.legacy_movement") == null;
	private static boolean applyMapgenFix = System.getProperty("openmods.no_mapgen_fix") == null;
	private static boolean applyCorpseTransformer = System.getProperty("openmods.no_player_hook") == null;

	private final Map<String, TransformProvider> vanillaPatches = Maps.newHashMap();

	public OpenModsClassTransformer() {
		if (applyMovementTransformer) {
			vanillaPatches.put("net.minecraft.client.entity.EntityPlayerSP", new TransformProvider(ClassWriter.COMPUTE_FRAMES) {
				@Override
				public ClassVisitor createVisitor(String name, ClassVisitor cv) {
					Log.info("Trying to apply movement callback (class: %s)", name);
					return new MovementPatcher(name, cv);
				}
			});
		}

		if (applyMapgenFix) {
			vanillaPatches.put("net.minecraft.world.gen.structure.MapGenStructure", new TransformProvider(ClassWriter.COMPUTE_FRAMES) {
				@Override
				public ClassVisitor createVisitor(String name, ClassVisitor cv) {
					Log.info("Trying to patch MapGenStructure (class: %s)", name);
					return new MapGenStructureVisitor(name, cv);
				}
			});
		}

		if (applyCorpseTransformer) {
			vanillaPatches.put("net.minecraft.client.renderer.entity.RenderPlayer", new TransformProvider(ClassWriter.COMPUTE_FRAMES) {
				@Override
				public ClassVisitor createVisitor(String name, ClassVisitor cv) {
					Log.info("Trying to patch RenderPlayer (class: %s)", name);
					return new PlayerRendererHookVisitor(name, cv);
				}
			});
		}

		if (true) {
			vanillaPatches.put("net.minecraft.client.shader.Framebuffer", new TransformProvider(ClassWriter.COMPUTE_FRAMES) {
				@Override
				public ClassVisitor createVisitor(String name, ClassVisitor cv) {
					Log.info("Trying to patch Framebuffer (class: %s)", name);
					return new FramebufferInjector(name, cv);
				}
			});

			vanillaPatches.put("net.minecraft.client.renderer.OpenGlHelper", new TransformProvider(ClassWriter.COMPUTE_FRAMES) {
				@Override
				public ClassVisitor createVisitor(String name, ClassVisitor cv) {
					Log.info("Trying to patch OpenGlHelper (class: %s)", name);
					return new CapabilitiesHookInjector(name, cv);
				}
			});
		}
	}

	private final static TransformProvider INCLUDING_CV = new TransformProvider(0) {
		@Override
		public ClassVisitor createVisitor(String name, ClassVisitor cv) {
			return new IncludingClassVisitor(cv);
		}
	};

	@Override
	public byte[] transform(final String name, String transformedName, byte[] bytes) {
		if (bytes == null) return bytes;

		if (transformedName.startsWith("net.minecraft.")) {
			TransformProvider provider = vanillaPatches.get(transformedName);
			return (provider != null)? VisitorHelper.apply(bytes, name, provider) : bytes;
		}

		try {
			return VisitorHelper.apply(bytes, name, INCLUDING_CV);
		} catch (Throwable t) {
			Log.severe(t, "Failed to apply including transformer on %s(%s)", name, transformedName);
			throw Throwables.propagate(t);
		}
	}
}
