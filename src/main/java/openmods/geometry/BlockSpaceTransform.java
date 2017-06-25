package openmods.geometry;

import com.google.common.base.Preconditions;
import javax.vecmath.Matrix3f;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public abstract class BlockSpaceTransform {
	// Ok, so it's needlessly complicated, but I was trying to relax after trying to remember how to build transformation matrices from my linear algebra course

	private static final int ARG_Z = 6;
	private static final int ARG_Y = 4;
	private static final int ARG_X = 2;

	private static class BytecodeClassLoader extends ClassLoader {
		private BytecodeClassLoader() {
			super(BytecodeClassLoader.class.getClassLoader());
		}

		public Class<?> define(byte[] data) {
			return defineClass(null, data, 0, data.length);
		}
	}

	private static void createVarAccess(MethodVisitor mv, double value, int var) {
		if (value == 1) {
			mv.visitVarInsn(Opcodes.DLOAD, var);
		} else if (value == -1) {
			mv.visitInsn(Opcodes.DCONST_1);
			mv.visitVarInsn(Opcodes.DLOAD, var);
			mv.visitInsn(Opcodes.DSUB);
		} else {
			throw new IllegalArgumentException();
		}
	}

	private static void createRowVectorMultiplication(MethodVisitor mv, double x, double y, double z) {
		// there should be exactly one non-zero value in row, so we don't do addition
		if (x != 0) {
			Preconditions.checkArgument(y == 0 && z == 0);
			createVarAccess(mv, x, ARG_X);
		} else if (y != 0) {
			Preconditions.checkArgument(x == 0 && z == 0);
			createVarAccess(mv, y, ARG_Y);
		} else if (z != 0) {
			Preconditions.checkArgument(x == 0 && y == 0);
			createVarAccess(mv, z, ARG_Z);
		} else {
			throw new IllegalArgumentException();
		}
	}

	private static void createTransformMethod(MethodVisitor mv, boolean invert) {
		// 0 - this (unused)
		// 1 - orientation
		// 2,3 - x
		// 4,5 - y
		// 6,7 - z
		mv.visitCode();

		mv.visitVarInsn(Opcodes.ALOAD, 1);

		final String enumType = Type.getInternalName(Enum.class);
		mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, enumType, "ordinal", Type.getMethodDescriptor(Type.INT_TYPE), false);

		final Orientation[] orientations = Orientation.values();

		final Label defaultLabel = new Label();
		final Label[] targets = new Label[orientations.length];
		for (int i = 0; i < orientations.length; i++)
			targets[i] = new Label();

		mv.visitTableSwitchInsn(0, orientations.length - 1, defaultLabel, targets);

		{
			mv.visitLabel(defaultLabel);
			final String excType = Type.getInternalName(IllegalArgumentException.class);
			final Type stringType = Type.getType(String.class);

			mv.visitTypeInsn(Opcodes.NEW, excType);
			mv.visitInsn(Opcodes.DUP);
			mv.visitVarInsn(Opcodes.ALOAD, 1);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, Type.getInternalName(Object.class), "toString", Type.getMethodDescriptor(stringType), false);
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, excType, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, stringType), false);
			mv.visitInsn(Opcodes.ATHROW);
		}

		{
			final String createVectorDesc = Type.getMethodDescriptor(Type.getType(Vec3d.class), Type.DOUBLE_TYPE, Type.DOUBLE_TYPE, Type.DOUBLE_TYPE);
			final String selfType = Type.getInternalName(BlockSpaceTransform.class);
			for (int i = 0; i < orientations.length; i++) {
				mv.visitLabel(targets[i]);
				final Orientation orientation = orientations[i];
				final Matrix3f mat = orientation.getLocalToWorldMatrix();
				if (invert) mat.invert();

				createRowVectorMultiplication(mv, mat.m00, mat.m01, mat.m02);
				createRowVectorMultiplication(mv, mat.m10, mat.m11, mat.m12);
				createRowVectorMultiplication(mv, mat.m20, mat.m21, mat.m22);

				mv.visitMethodInsn(Opcodes.INVOKESTATIC, selfType, "createVector", createVectorDesc, false);
				mv.visitInsn(Opcodes.ARETURN);
			}
		}

		mv.visitMaxs(0, 0);
		mv.visitEnd();
	}

	private static Class<? extends BlockSpaceTransform> createTransformClass() {
		final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

		final String parentCls = Type.getInternalName(BlockSpaceTransform.class);

		final String name = parentCls + "$GeneratedImplementation$";

		cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER | Opcodes.ACC_SYNTHETIC, name, null, parentCls, new String[] {});

		cw.visitSource(".dynamic", null);

		{
			MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(Opcodes.ALOAD, 0);
			mv.visitMethodInsn(Opcodes.INVOKESPECIAL, parentCls, "<init>", "()V", false);
			mv.visitInsn(Opcodes.RETURN);
			mv.visitMaxs(0, 0);
			mv.visitEnd();
		}

		final String transformMethod = Type.getMethodDescriptor(Type.getType(Vec3d.class), Type.getType(Orientation.class), Type.DOUBLE_TYPE, Type.DOUBLE_TYPE, Type.DOUBLE_TYPE);

		{
			final MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "mapWorldToBlock", transformMethod, null, null);
			createTransformMethod(mv, true);
		}

		{
			final MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "mapBlockToWorld", transformMethod, null, null);
			createTransformMethod(mv, false);
		}

		cw.visitEnd();

		final byte[] clsBytes = cw.toByteArray();
		final BytecodeClassLoader loader = new BytecodeClassLoader();
		@SuppressWarnings("unchecked")
		final Class<? extends BlockSpaceTransform> cls = (Class<? extends BlockSpaceTransform>)loader.define(clsBytes);
		return cls;
	}

	private static BlockSpaceTransform createTransformInstance() {
		try {
			final Class<? extends BlockSpaceTransform> transformClass = createTransformClass();
			return transformClass.newInstance();
		} catch (Throwable t) {
			throw new Error("Failed to create block space transformer", t);
		}
	}

	public static final BlockSpaceTransform instance = createTransformInstance();

	public abstract Vec3d mapWorldToBlock(Orientation orientation, double x, double y, double z);

	public abstract Vec3d mapBlockToWorld(Orientation orientation, double x, double y, double z);

	// wrapper over obfuscated method
	protected static Vec3d createVector(double x, double y, double z) {
		return new Vec3d(x, y, z);
	}

	public AxisAlignedBB mapWorldToBlock(Orientation orientation, AxisAlignedBB aabb) {
		final Vec3d min = mapWorldToBlock(orientation, aabb.minX, aabb.minY, aabb.minZ);
		final Vec3d max = mapWorldToBlock(orientation, aabb.maxX, aabb.maxY, aabb.maxZ);
		return new AxisAlignedBB(min.xCoord, min.yCoord, min.zCoord, max.xCoord, max.yCoord, max.zCoord);
	}

	public AxisAlignedBB mapBlockToWorld(Orientation orientation, AxisAlignedBB aabb) {
		final Vec3d min = mapBlockToWorld(orientation, aabb.minX, aabb.minY, aabb.minZ);
		final Vec3d max = mapBlockToWorld(orientation, aabb.maxX, aabb.maxY, aabb.maxZ);
		return new AxisAlignedBB(min.xCoord, min.yCoord, min.zCoord, max.xCoord, max.yCoord, max.zCoord);
	}
}
