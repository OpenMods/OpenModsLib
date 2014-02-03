package openmods;

import net.minecraft.launchwrapper.IClassTransformer;
import openmods.asm.VisitorHelper;
import openmods.asm.VisitorHelper.TransformProvider;
import openmods.include.IncludingClassVisitor;

import org.objectweb.asm.ClassVisitor;

public class OpenModsClassTransformer implements IClassTransformer {

	private final static TransformProvider INCLUDING_CV = new TransformProvider() {
		@Override
		public ClassVisitor createVisitor(ClassVisitor cv) {
			return new IncludingClassVisitor(cv);
		}
	};

	@Override
	public byte[] transform(final String name, String transformedName, byte[] bytes) {
		if (bytes == null || name.startsWith("openmods.asm") || name.startsWith("openmods.include") || name.startsWith("net.minecraft.")) return bytes;
		// / no need for COMPUTE_FRAMES, we can handle simple stuff
		return VisitorHelper.apply(bytes, 0, INCLUDING_CV);
	}
}
