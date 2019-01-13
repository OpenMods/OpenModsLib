package openmods.include;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import openmods.Log;
import openmods.asm.StopTransforming;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;

public class IncludingClassVisitor extends ClassVisitor {

	private class AnnotatedFieldFinder extends FieldVisitor implements IIncludedMethodBuilder {
		public final String fieldName;
		public final Type fieldType;

		public AnnotatedFieldFinder(String fieldName, Type fieldType, FieldVisitor fv) {
			super(Opcodes.ASM5, fv);
			this.fieldName = fieldName;
			this.fieldType = fieldType;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			AnnotationVisitor av = super.visitAnnotation(desc, visible);
			Type t = Type.getType(desc);
			if (INCLUDE_INTERFACE.equals(t)) return new IncludeAnnotationVisitor(av, this);
			return av;
		}

		@Override
		public Type getInterfaceType(Type annotationHint) {
			return MoreObjects.firstNonNull(annotationHint, fieldType);
		}

		@Override
		public MethodAdder createMethod(Type wrappedInterface) {
			return new MethodAdder(wrappedInterface) {
				@Override
				public void visitInterfaceAccess(MethodVisitor target) {
					target.visitFieldInsn(Opcodes.GETFIELD, clsName, fieldName, fieldType.getDescriptor());
				}
			};
		}
	}

	private class AnnotatedMethodFinder extends MethodVisitor implements IIncludedMethodBuilder {
		private final Method method;

		public AnnotatedMethodFinder(Method method, MethodVisitor mv) {
			super(Opcodes.ASM5, mv);
			this.method = method;
		}

		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			AnnotationVisitor av = super.visitAnnotation(desc, visible);
			Type t = Type.getType(desc);
			if (INCLUDE_INTERFACE.equals(t)) return new IncludeAnnotationVisitor(av, this);
			if (INCLUDE_OVERRIDE.equals(t)) overrides.add(method);
			return av;
		}

		@Override
		public Type getInterfaceType(Type annotationHint) {
			return MoreObjects.firstNonNull(annotationHint, method.getReturnType());
		}

		@Override
		public MethodAdder createMethod(Type wrappedInterface) {
			return new MethodAdder(wrappedInterface) {
				@Override
				public void visitInterfaceAccess(MethodVisitor target) {
					target.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clsName, method.getName(), method.getDescriptor(), false);
				}
			};
		}
	}

	private interface IIncludedMethodBuilder {
		Type getInterfaceType(Type annotationHint);

		MethodAdder createMethod(Type wrappedInterface);
	}

	private class IncludeAnnotationVisitor extends AnnotationVisitor {
		private Type classParam;
		private final IIncludedMethodBuilder builder;

		public IncludeAnnotationVisitor(AnnotationVisitor av, IIncludedMethodBuilder builder) {
			super(Opcodes.ASM5, av);
			this.builder = builder;
		}

		@Override
		public void visit(String name, Object value) {
			if ("value".equals(name) && value instanceof Type) classParam = (Type)value;
			super.visit(name, value);
		}

		@Override
		public void visitEnd() {
			addInterfaceImplementations(classParam, builder);
		}
	}

	private abstract static class MethodAdder {
		public final Type intf;

		private MethodAdder(Type intf) {
			this.intf = intf;
		}

		public abstract void visitInterfaceAccess(MethodVisitor target);

		public void addMethod(ClassVisitor target, Method method) {
			MethodVisitor mv = target.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
					method.getName(),
					method.getDescriptor(),
					null, null);

			mv.visitVarInsn(Opcodes.ALOAD, 0);
			visitInterfaceAccess(mv);
			// should have interface reference on stack
			// checkcast just to be safe
			mv.visitTypeInsn(Opcodes.CHECKCAST, intf.getInternalName());

			Type[] args = method.getArgumentTypes();
			for (int i = 0; i < args.length; i++)
				mv.visitVarInsn(args[i].getOpcode(Opcodes.ILOAD), i + 1);

			mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, intf.getInternalName(), method.getName(), method.getDescriptor(), true);
			Type returnType = method.getReturnType();
			mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
			mv.visitMaxs(args.length + 1, args.length + 1);
			mv.visitEnd();
		}
	}

	public static final Type INCLUDE_INTERFACE = Type.getObjectType("openmods/include/IncludeInterface");

	public static final Type INCLUDE_OVERRIDE = Type.getObjectType("openmods/include/IncludeOverride");

	private final Set<Method> existingMethods = Sets.newHashSet();
	private final Set<Method> overrides = Sets.newHashSet();
	private final Map<Method, MethodAdder> methodsToAdd = Maps.newHashMap();

	private String clsName;
	private int version;
	private int access;
	private String signature;
	private String superName;
	private final Set<String> interfaces = Sets.newHashSet();

	public IncludingClassVisitor(ClassVisitor cv) {
		super(Opcodes.ASM5, cv);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		FieldVisitor parent = super.visitField(access, name, desc, signature, value);
		return new AnnotatedFieldFinder(name, Type.getType(desc), parent);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		Method method = new Method(name, desc);
		existingMethods.add(method);

		MethodVisitor parent = super.visitMethod(access, name, desc, signature, exceptions);
		return new AnnotatedMethodFinder(method, parent);
	}

	@Override
	public void visitEnd() {
		if (methodsToAdd.isEmpty()) throw new StopTransforming();

		final Set<Method> conflicts = Sets.intersection(existingMethods, methodsToAdd.keySet());
		Set<Method> nonMarked = Sets.difference(conflicts, overrides);
		Preconditions.checkState(nonMarked.isEmpty(), "%s implements interface methods %s, but they are not marked with @IncludeOverride", clsName, nonMarked);

		Set<Method> markedButNotImplemented = Sets.difference(overrides, methodsToAdd.keySet());
		Preconditions.checkState(markedButNotImplemented.isEmpty(), "%s marks methods %s with @IncludeOverride, but no interface implements it", clsName, markedButNotImplemented);

		Map<Method, MethodAdder> filtered = Maps.filterKeys(methodsToAdd, m -> !conflicts.contains(m));

		for (Map.Entry<Method, MethodAdder> m : filtered.entrySet())
			m.getValue().addMethod(cv, m.getKey());

		// risky, but should work, since we are only replacing interfaces
		super.visit(version, access, clsName, signature, superName, interfaces.toArray(new String[0]));
		super.visitEnd();
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (Modifier.isInterface(access)) throw new StopTransforming();

		this.clsName = name;
		this.access = access;
		this.version = version;
		this.signature = signature;
		this.superName = superName;
		this.interfaces.addAll(Arrays.asList(interfaces));
		super.visit(version, access, clsName, signature, superName, interfaces);
	}

	private List<Method> getInterfaceMethods(Type intf) {
		Preconditions.checkState(intf.getSort() == Type.OBJECT, "%s is not interface (including class = %s)", intf, clsName);
		try {
			Class<?> loaded = Class.forName(intf.getClassName());
			Preconditions.checkArgument(loaded.isInterface(), "%s is not interface (including class = %s)", loaded, clsName);

			List<Method> result = Lists.newArrayList();
			for (java.lang.reflect.Method m : loaded.getMethods())
				result.add(Method.getMethod(m));
			return result;
		} catch (ClassNotFoundException e) {
			Log.severe(e, "Error while searching for interface '%s'", intf);
			throw new RuntimeException(e);
		}
	}

	private void addInterfaceImplementations(Type annotationHint, IIncludedMethodBuilder builder) {
		Type wrappedInterface = builder.getInterfaceType(annotationHint);
		boolean notYetDeclared = interfaces.add(wrappedInterface.getInternalName());
		Preconditions.checkState(notYetDeclared, "%s already implements interface %s", clsName, wrappedInterface);
		Log.debug("Adding interface %s to %s", wrappedInterface.getInternalName(), clsName);
		for (Method m : getInterfaceMethods(wrappedInterface)) {
			MethodAdder prev = methodsToAdd.put(m, builder.createMethod(wrappedInterface));
			if (prev != null) Preconditions.checkState(overrides.contains(m), "Included method '%s' conflict, interfaces = %s,%s", m, wrappedInterface, prev.intf);
		}
	}

}
