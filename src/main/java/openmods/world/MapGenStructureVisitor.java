package openmods.world;

import openmods.Log;
import openmods.api.IResultListener;
import openmods.asm.MappedType;
import openmods.asm.MethodMatcher;
import openmods.asm.VisitorHelper;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class MapGenStructureVisitor extends ClassVisitor {

	private final MethodMatcher modifiedMethod;
	private final MethodMatcher markerMethod;
	private final MappedType structureStartCls;
	private final IResultListener listener;

	private class FixerMethodVisitor extends MethodVisitor {
		public FixerMethodVisitor(MethodVisitor mv) {
			super(Opcodes.ASM5, mv);
		}

		private boolean checkcastFound;
		private Integer localVarId;
		private boolean markerMethodFound;

		/*
		 * Default compilator usually creates:
		 * checkcast class net/minecraft/world/gen/structure/StructureStart
		 * astore X
		 * aload X
		 *
		 * We use that to get id of local variable that stores 'structurestart'
		 */

		@Override
		public void visitTypeInsn(int opcode, String type) {
			super.visitTypeInsn(opcode, type);
			if (opcode == Opcodes.CHECKCAST && type.equals(structureStartCls.name())) {
				checkcastFound = true;
				Log.debug("Found checkcast to '%s'", type);
			}
		}

		@Override
		public void visitVarInsn(int opcode, int var) {
			super.visitVarInsn(opcode, var);

			if (checkcastFound && opcode == Opcodes.ASTORE) {
				localVarId = var;
				checkcastFound = false;
				Log.debug("Found var: %d", localVarId);
			}
		}

		/*
		 * Here we are transforming condition
		 * if (structurestart.isSizeableStructure())
		 * to
		 * if (structurestart.isSizeableStructure() &&
		 * !structurestart.getComponents().isEmpty())
		 *
		 * Again, we assume that compilator places IFEQ jump just after calling
		 * isSizeableStructure from first expression. We can then reuse label
		 * for second part
		 */

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean intf) {
			super.visitMethodInsn(opcode, owner, name, desc, intf);
			if (opcode == Opcodes.INVOKEVIRTUAL && owner.equals(structureStartCls.name()) && markerMethod.match(name, desc)) {
				markerMethodFound = true;
				Log.debug("Found 'StructureStart.isSizeableStructure' (%s.%s) call", owner, name);
			}
		}

		@Override
		public void visitJumpInsn(int opcode, Label label) {
			super.visitJumpInsn(opcode, label);

			if (markerMethodFound && localVarId != null && opcode == Opcodes.IFEQ) {
				Log.debug("All conditions matched, inserting extra condition");
				super.visitVarInsn(Opcodes.ALOAD, localVarId); // hopefully
																// 'structurestart'
				String getComponentsMethodName = VisitorHelper.useSrgNames()? "func_75073_b" : "getComponents";
				super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, structureStartCls.name(), getComponentsMethodName, "()Ljava/util/LinkedList;", false);
				super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/util/LinkedList", "isEmpty", "()Z", false);
				super.visitJumpInsn(Opcodes.IFNE, label);
				listener.onSuccess();
				markerMethodFound = false;
			}
		}
	}

	public MapGenStructureVisitor(String obfClassName, ClassVisitor cv, IResultListener listener) {
		super(Opcodes.ASM5, cv);

		this.listener = listener;

		structureStartCls = MappedType.of("net/minecraft/world/gen/structure/StructureStart");
		MappedType chunkPositionCls = MappedType.of("net/minecraft/world/ChunkPosition");
		MappedType worldCls = MappedType.of("net/minecraft/world/World");

		String descriptor = Type.getMethodDescriptor(
				chunkPositionCls.type(),
				worldCls.type(),
				Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE
				);

		modifiedMethod = new MethodMatcher(obfClassName, descriptor, "func_151545_a", "func_151545_a");
		markerMethod = new MethodMatcher(structureStartCls, "()Z", "isSizeableStructure", "func_75069_d");
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor parent = super.visitMethod(access, name, desc, signature, exceptions);
		return modifiedMethod.match(name, desc)? new FixerMethodVisitor(parent) : parent;
	}

}
