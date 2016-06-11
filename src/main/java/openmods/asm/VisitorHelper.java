package openmods.asm;

import com.google.common.base.Preconditions;
import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import net.minecraft.launchwrapper.Launch;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

public class VisitorHelper {

	public abstract static class TransformProvider {
		private final int flags;

		public TransformProvider(int flags) {
			this.flags = flags;
		}

		public abstract ClassVisitor createVisitor(String name, ClassVisitor cv);
	}

	public static byte[] apply(byte[] bytes, String name, TransformProvider context) {
		Preconditions.checkNotNull(bytes);
		ClassReader cr = new ClassReader(bytes);
		ClassWriter cw = new ClassWriter(cr, context.flags);
		ClassVisitor mod = context.createVisitor(name, cw);

		try {
			cr.accept(mod, 0);
			return cw.toByteArray();
		} catch (StopTransforming e) {
			return bytes;
		}
	}

	public static boolean useSrgNames() {
		Boolean deobfuscated = (Boolean)Launch.blackboard.get("fml.deobfuscatedEnvironment");
		return deobfuscated == null || !deobfuscated;
	}

	public static String getMappedName(String clsName) {
		return useSrgNames()? FMLDeobfuscatingRemapper.INSTANCE.unmap(clsName) : clsName;
	}
}
