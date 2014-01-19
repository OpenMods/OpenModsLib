package openmods.asm;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import com.google.common.base.Preconditions;

public class OpenModsClassTransformer implements IClassTransformer {

	public static interface TransformProvider {
		public ClassVisitor createVisitor(ClassVisitor cv);
	}

	private final static TransformProvider INCLUDING_CV = new TransformProvider() {

		@Override
		public ClassVisitor createVisitor(ClassVisitor cv) {
			return new IncludingClassVisitor(cv);
		}
	};

	public static byte[] applyVisitor(byte[] bytes, int flags, TransformProvider context) {
		Preconditions.checkNotNull(bytes);
		ClassReader cr = new ClassReader(bytes);
		ClassWriter cw = new ClassWriter(cr, flags);
		ClassVisitor mod = context.createVisitor(cw);

		try {
			cr.accept(mod, 0);
			return cw.toByteArray();
		} catch (StopTransforming e) {
			return bytes;
		}
	}

	@Override
	public byte[] transform(final String name, String transformedName, byte[] bytes) {
		if (bytes == null || name.startsWith("openmods.asm") || name.startsWith("net.minecraft.")) return bytes;
		// / no need for COMPUTE_FRAMES, we can handle simple stuff
		return applyVisitor(bytes, 0, INCLUDING_CV);
	}
}
