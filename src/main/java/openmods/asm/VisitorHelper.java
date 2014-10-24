package openmods.asm;

import net.minecraft.launchwrapper.Launch;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;

import com.google.common.base.Preconditions;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

public class VisitorHelper {

	public static interface TransformProvider {
		public ClassVisitor createVisitor(ClassVisitor cv);
	}

	public static byte[] apply(byte[] bytes, int flags, TransformProvider context) {
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

	public static boolean useSrgNames() {
		Boolean deobfuscated = (Boolean)Launch.blackboard.get("fml.deobfuscatedEnvironment");
		return deobfuscated == null || !deobfuscated;
	}

	public static String getMappedName(String clsName) {
		return useSrgNames()? FMLDeobfuscatingRemapper.INSTANCE.unmap(clsName) : clsName;
	}
}
