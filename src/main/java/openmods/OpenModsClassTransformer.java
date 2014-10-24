package openmods;

import net.minecraft.launchwrapper.IClassTransformer;
import openmods.asm.VisitorHelper;
import openmods.asm.VisitorHelper.TransformProvider;
import openmods.include.IncludingClassVisitor;
import openmods.movement.MovementPatcher;
import openmods.renderer.PlayerRendererHookVisitor;
import openmods.world.MapGenStructureVisitor;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import com.google.common.base.Throwables;

public class OpenModsClassTransformer implements IClassTransformer {

	private static boolean applyMovementTransformer = System.getProperty("openmods.legacy_movement") == null;
	private static boolean applyMapgenFix = System.getProperty("openmods.no_mapgen_fix") == null;
	private static boolean applyCorpseTransformer = System.getProperty("openmods.no_player_hook") == null;

	private final static TransformProvider INCLUDING_CV = new TransformProvider() {
		@Override
		public ClassVisitor createVisitor(ClassVisitor cv) {
			return new IncludingClassVisitor(cv);
		}
	};

	@Override
	public byte[] transform(final String name, String transformedName, byte[] bytes) {
		if (bytes == null) return bytes;

		if (applyMovementTransformer && transformedName.equals("net.minecraft.client.entity.EntityPlayerSP")) return VisitorHelper.apply(bytes, ClassWriter.COMPUTE_FRAMES, new TransformProvider() {
			@Override
			public ClassVisitor createVisitor(ClassVisitor cv) {
				Log.info("Trying to apply movement callback (class: %s)", name);
				return new MovementPatcher(name, cv);
			}
		});

		if (applyMapgenFix && transformedName.equals("net.minecraft.world.gen.structure.MapGenStructure")) return VisitorHelper.apply(bytes, ClassWriter.COMPUTE_FRAMES, new TransformProvider() {
			@Override
			public ClassVisitor createVisitor(ClassVisitor cv) {
				Log.info("Trying to patch MapGenStructure (class: %s)", name);
				return new MapGenStructureVisitor(name, cv);
			}
		});

		if (applyCorpseTransformer && transformedName.equals("net.minecraft.client.renderer.entity.RenderPlayer")) return VisitorHelper.apply(bytes, ClassWriter.COMPUTE_FRAMES, new TransformProvider() {
			@Override
			public ClassVisitor createVisitor(ClassVisitor cv) {
				Log.info("Trying to patch RenderPlayer (class: %s)", name);
				return new PlayerRendererHookVisitor(name, cv);
			}
		});

		if (name.startsWith("net.minecraft.")) return bytes;
		// / no need for COMPUTE_FRAMES, we can handle simple stuff

		try {
			return VisitorHelper.apply(bytes, 0, INCLUDING_CV);
		} catch (Throwable t) {
			Log.severe(t, "Failed to apply including transformer on %s(%s)", name, transformedName);
			throw Throwables.propagate(t);
		}
	}
}
