package openmods;

import net.minecraft.launchwrapper.IClassTransformer;
import openmods.asm.VisitorHelper;
import openmods.asm.VisitorHelper.TransformProvider;
import openmods.include.IncludingClassVisitor;
import openmods.movement.MovementPatcher;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class OpenModsClassTransformer implements IClassTransformer {

	private static boolean applyMovementTransformer = System.getProperty("openmods.legacy_movement") == null;

	private final static TransformProvider INCLUDING_CV = new TransformProvider() {
		@Override
		public ClassVisitor createVisitor(ClassVisitor cv) {
			return new IncludingClassVisitor(cv);
		}
	};

	@Override
	public byte[] transform(final String name, String transformedName, byte[] bytes) {
		if (bytes == null || name.startsWith("openmods.asm") || name.startsWith("openmods.include")) return bytes;

		if (applyMovementTransformer && transformedName.equals("net.minecraft.client.entity.EntityPlayerSP")) return VisitorHelper.apply(bytes, ClassWriter.COMPUTE_FRAMES, new TransformProvider() {
			@Override
			public ClassVisitor createVisitor(ClassVisitor cv) {
				OpenModsCorePlugin.log.info(String.format("Trying to apply movement callback (class: %s)", name));
				return new MovementPatcher(name, cv);
			}
		});

		if (name.startsWith("net.minecraft.")) return bytes;
		// / no need for COMPUTE_FRAMES, we can handle simple stuff
		return VisitorHelper.apply(bytes, 0, INCLUDING_CV);
	}
}
