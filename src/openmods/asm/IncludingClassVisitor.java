package openmods.asm;

import java.lang.reflect.Modifier;
import java.util.*;

import openmods.Log;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.Method;

import com.google.common.base.*;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class IncludingClassVisitor extends ClassVisitor {

	private void addInterfaceImplementations(Type annotationHint, IIncludedMethodBuilder builder) {
		Type wrappedInterface = builder.getInterfaceType(annotationHint);
		boolean notYetDeclared = interfaces.add(wrappedInterface.getInternalName());
		Preconditions.checkState(notYetDeclared, "%s already implements interface %s", clsName, wrappedInterface);

		for (Method m : getInterfaceMethods(wrappedInterface)) {
			MethodAdder prev = methodsToAdd.put(m, builder.createMethod(wrappedInterface));
			if (prev != null) Preconditions.checkState(overrides.contains(m), "Included method '%s' conflict, interfaces = %s,%s", m, wrappedInterface, prev.intf);
		}
	}

	private class AnnotatedFieldFinder extends FieldVisitor implements IIncludedMethodBuilder {
		public final String fieldName;
		public final Type fieldType;

		public AnnotatedFieldFinder(String fieldName, Type fieldType, FieldVisitor fv) {
			super(Opcodes.ASM4, fv);
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
			return Objects.firstNonNull(annotationHint, fieldType);
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
			super(Opcodes.ASM4, mv);
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
			return Objects.firstNonNull(annotationHint, method.getReturnType());
		}

		@Override
		public MethodAdder createMethod(Type wrappedInterface) {
			return new MethodAdder(wrappedInterface) {
				@Override
				public void visitInterfaceAccess(MethodVisitor target) {
					target.visitMethodInsn(Opcodes.INVOKEVIRTUAL, clsName, method.getName(), method.getDescriptor());
				}
			};
		}
	}

	private interface IIncludedMethodBuilder {
		public Type getInterfaceType(Type annotationHint);

		public MethodAdder createMethod(Type wrappedInterface);
	}

	private class IncludeAnnotationVisitor extends AnnotationVisitor {
		private Type classParam;
		private final IIncludedMethodBuilder builder;

		public IncludeAnnotationVisitor(AnnotationVisitor av, IIncludedMethodBuilder builder) {
			super(Opcodes.ASM4, av);
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

			mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, intf.getInternalName(), method.getName(), method.getDescriptor());
			Type returnType = method.getReturnType();
			mv.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
	}

	public static final String EXTENDABLE_MARKER = "openmods/include/IExtendable";

	public static final Type INCLUDE_INTERFACE = Type.getObjectType("openmods/include/IncludeInterface");

	public static final Type INCLUDE_OVERRIDE = Type.getObjectType("openmods/include/IncludeOverride");

	private List<Method> getInterfaceMethods(Type intf) {
		Preconditions.checkState(intf.getSort() == Type.OBJECT, "%s is not interface (including class = %s)", intf, clsName);
		try {
			Class<?> loaded = Class.forName(intf.getClassName());
			Preconditions.checkArgument(loaded.isInterface(), "%s is not interface (including class = %s)", loaded, clsName);

			List<Method> result = Lists.newArrayList();
			for (java.lang.reflect.Method m : loaded.getMethods())
				result.add(Method.getMethod(m));
			return result;
		} catch (Throwable t) {
			Log.severe(t, "Error while searching for interface '%s'", intf);
			throw Throwables.propagate(t);
		}
	}

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
		super(Opcodes.ASM4, cv);
	}

	private static boolean findMarker(String[] interfaces) {
		for (String intf : interfaces)
			if (EXTENDABLE_MARKER.equals(intf)) return true;
		return false;
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
		final Set<Method> conflicts = Sets.intersection(existingMethods, methodsToAdd.keySet());
		Set<Method> nonMarked = Sets.difference(conflicts, overrides);
		Preconditions.checkState(nonMarked.isEmpty(), "%s implements interface methods %s, but they are not marked with @IncludeOverride", clsName, nonMarked);

		Set<Method> markedButNotImplemented = Sets.difference(overrides, methodsToAdd.keySet());
		Preconditions.checkState(markedButNotImplemented.isEmpty(), "%s marks methods %s with @IncludeOverride, but no interface implements it", clsName, markedButNotImplemented);

		Map<Method, MethodAdder> filtered = Maps.filterKeys(methodsToAdd, new Predicate<Method>() {
			@Override
			public boolean apply(Method m) {
				return !conflicts.contains(m);
			}
		});

		for (Map.Entry<Method, MethodAdder> m : filtered.entrySet())
			m.getValue().addMethod(cv, m.getKey());

		// risky, but should work, since we are only replacing interfaces
		super.visit(version, access, clsName, signature, superName, interfaces.toArray(new String[interfaces.size()]));
		super.visitEnd();
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		if (Modifier.isInterface(access) || Modifier.isInterface(access) || !findMarker(interfaces)) throw new StopTransforming();

		this.clsName = name;
		this.access = access;
		this.version = version;
		this.signature = signature;
		this.superName = superName;
		this.interfaces.addAll(Arrays.asList(interfaces));
		super.visit(version, access, clsName, signature, superName, interfaces);
	}

}
