package openmods.calc.types.multi;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import openmods.calc.Frame;
import openmods.calc.symbol.ICallable;
import openmods.reflection.TypeVariableHolder;
import openmods.utils.AnnotationMap;
import openmods.utils.CachedFactory;
import openmods.utils.OptionalInt;
import openmods.utils.Stack;

public class TypedFunction {

	public static class DispatchException extends RuntimeException {
		private static final long serialVersionUID = 8096730015947971477L;

		public DispatchException(Collection<TypedValue> args) {
			super("Failed to found override for args " + args);
		}
	}

	public static class AmbiguousDispatchException extends RuntimeException {
		private static final long serialVersionUID = 4012320626013808859L;

		public AmbiguousDispatchException(Collection<Method> methods) {
			super("Cannot always select overload between methods " + methods.toString());
		}
	}

	public static class NonStaticMethodsPresent extends RuntimeException {
		private static final long serialVersionUID = -8854128456679001775L;

		public NonStaticMethodsPresent() {
			super("Non-static methods detected, but target is null");
		}
	}

	public static class NonCompatibleMethodsPresent extends RuntimeException {
		private static final long serialVersionUID = -3296321220525016125L;

		public NonCompatibleMethodsPresent(Class<?> required, Object target) {
			super("Target " + target.getClass() + " is not compatible with selected methods from class " + required);
		}
	}

	public static class MethodInvokeException extends RuntimeException {
		private static final long serialVersionUID = 4012320626013808859L;

		public MethodInvokeException(Method method, Throwable t) {
			super("Failed to invoke method " + method, t);
		}
	}

	public static class MultipleReturn {
		private final TypedValue[] rets;

		private MultipleReturn(TypedValue[] rets) {
			this.rets = rets;
		}

		public static MultipleReturn wrap(TypedValue... rets) {
			return new MultipleReturn(rets);
		}
	}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface RawReturn {}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface MultiReturn {}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Variant {}

	@Target(ElementType.PARAMETER)
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface RawArg {}

	@Target(ElementType.PARAMETER)
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface RawDispatchArg {
		public Class<?>[] value();
	}

	@Target(ElementType.PARAMETER)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface OptionalArgs {}

	@Target(ElementType.PARAMETER)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface DispatchArg {
		public Class<?>[] extra() default {};
	}

	private static final CachedFactory<Method, TypeVariant> variantsCache = new CachedFactory<Method, TypeVariant>() {
		@Override
		protected TypeVariant create(Method key) {
			return createVariant(key);
		}
	};

	public static class Builder {
		// we want variants with "longer" dispatch list to be matched first
		private static final Ordering<TypeVariant> VARIANT_ORDERING = Ordering.natural().reverse().onResultOf(new Function<TypeVariant, Integer>() {
			@Override
			public Integer apply(TypeVariant input) {
				return input.lastDispatchArg;
			}
		});

		private static final CachedFactory<Set<Method>, TypedFunctionBody> bodyCache = new CachedFactory<Set<Method>, TypedFunctionBody>() {

			@Override
			protected TypedFunctionBody create(Set<Method> methods) {
				final List<TypeVariant> variants = Lists.newArrayList();

				for (Method m : methods) {
					try {
						variants.add(variantsCache.getOrCreate(m));
					} catch (Exception e) {
						throw new RuntimeException("Failed to register method " + m, e);
					}
				}

				if (variants.size() == 1) {
					return createSingleFunction(variants.get(0));
				} else {
					verifyVariants(variants);
					Collections.sort(variants, VARIANT_ORDERING);
					final OptionalInt mandatoryArgNum = calculateMandatoryArgNum(variants);
					return createMultiFunction(variants, mandatoryArgNum);
				}
			}
		};

		private Set<Class<?>> allowedClasses = Sets.newHashSet();

		private final Set<Method> variants = Sets.newHashSet();

		private Builder() {}

		public Builder addVariant(Method method) {
			if (!Modifier.isStatic(method.getModifiers()))
				allowedClasses.add(method.getDeclaringClass());

			method.setAccessible(true);
			variants.add(method);
			return this;
		}

		public Builder addVariants(Class<?> cls) {
			for (Method m : cls.getMethods())
				if (m.isAnnotationPresent(Variant.class))
					addVariant(m);

			return this;
		}

		public IUnboundCallable build(Class<?> targetCls) {
			Preconditions.checkArgument(!this.variants.isEmpty(), "No variants defined");

			if (targetCls == null) {
				if (!this.allowedClasses.isEmpty()) throw new NonStaticMethodsPresent();
			} else {
				for (Class<?> cls : this.allowedClasses)
					if (!cls.isAssignableFrom(targetCls))
						throw new NonCompatibleMethodsPresent(cls, targetCls);
			}

			final TypedFunctionBody body = bodyCache.getOrCreate(ImmutableSet.copyOf(this.variants));
			return new TypedFunction.Unbound(targetCls, body);
		}

		public ICallable<TypedValue> build(TypeDomain domain, Object target) {
			Preconditions.checkArgument(!this.variants.isEmpty(), "No variants defined");

			if (target == null) {
				if (!this.allowedClasses.isEmpty()) throw new NonStaticMethodsPresent();
			} else {
				for (Class<?> cls : this.allowedClasses)
					if (!cls.isInstance(target))
						throw new NonCompatibleMethodsPresent(cls, target);
			}

			final TypedFunctionBody body = bodyCache.getOrCreate(ImmutableSet.copyOf(this.variants));
			return new TypedFunction.Bound(domain, target, body);
		}

		private static TypedFunctionBody createSingleFunction(final TypeVariant variant) {
			return new TypedFunctionBody(OptionalInt.of(variant.mandatoryArgNum)) {
				@Override
				protected List<TypedValue> execute(TypeDomain domain, Object target, List<TypedValue> args) {
					if (!variant.matchDispatchArgs(args)) throw new DispatchException(args);
					return variant.execute(domain, target, args);
				}
			};
		}

		private static TypedFunctionBody createMultiFunction(final List<TypeVariant> variants, final OptionalInt mandatoryArgNum) {
			return new TypedFunctionBody(mandatoryArgNum) {
				@Override
				protected List<TypedValue> execute(TypeDomain domain, Object target, List<TypedValue> args) {
					for (TypeVariant v : variants)
						if (v.matchDispatchArgs(args)) return v.execute(domain, target, args);

					throw new DispatchException(args);
				}
			};
		}

		private static void verifyVariants(final List<TypeVariant> variants) {
			// O(n^2) algorithm. Meh, not on critical path, don't bother

			final Set<Method> ambiguousMethods = Sets.newHashSet();
			for (int i = 0; i < variants.size(); i++) {
				final TypeVariant v1 = variants.get(i);
				for (int j = i + 1; j < variants.size(); j++) {
					final TypeVariant v2 = variants.get(j);
					if (v2.isMatchAmbigous(v1)) {
						ambiguousMethods.add(v1.method);
						ambiguousMethods.add(v2.method);
					}
				}
			}

			if (!ambiguousMethods.isEmpty()) throw new AmbiguousDispatchException(ambiguousMethods);
		}

		private static OptionalInt calculateMandatoryArgNum(List<TypeVariant> variants) {
			OptionalInt result = OptionalInt.absent();
			for (TypeVariant v : variants) {
				if (result.isPresent()) {
					if (result.get() != v.mandatoryArgNum) return OptionalInt.absent();
				} else {
					result = OptionalInt.of(v.mandatoryArgNum);
				}
			}

			return result;
		}
	}

	@TypeVariableHolder(Optional.class)
	private static class OptionalTypeHolder {
		private static TypeVariable<?> T;

		public static Class<?> resolve(TypeToken<?> t) {
			return t.resolveType(T).getRawType();
		}
	}

	@TypeVariableHolder(Iterable.class)
	private static class IterableTypeHolder {
		private static TypeVariable<?> T;

		public static Class<?> resolve(Type t) {
			return TypeToken.of(t).resolveType(T).getRawType();
		}
	}

	private static class MissingType {}

	private static class DispatchArgMatcher {
		public static final DispatchArgMatcher MISSING = new DispatchArgMatcher(MissingType.class);
		public final Set<Class<?>> expectedTypes;

		private DispatchArgMatcher(Set<Class<?>> expectedTypes) {
			this.expectedTypes = ImmutableSet.copyOf(expectedTypes);
		}

		private DispatchArgMatcher(Class<?>... expectedTypes) {
			this.expectedTypes = ImmutableSet.copyOf(expectedTypes);
		}

		public boolean match(Class<?> type) {
			return expectedTypes.contains(type);
		}

		public boolean isAmbiguous(DispatchArgMatcher other) {
			return !Sets.intersection(this.expectedTypes, other.expectedTypes).isEmpty();
		}

	}

	private interface ArgConverter {
		public Object convert(Iterator<TypedValue> value);
	}

	private static class MandatoryArgConverter implements ArgConverter {
		private final Class<?> cls;

		public MandatoryArgConverter(Class<?> cls) {
			Preconditions.checkNotNull(cls);
			this.cls = cls;
		}

		@Override
		public Object convert(Iterator<TypedValue> value) {
			Preconditions.checkArgument(value.hasNext(), "Missing mandatory argument");
			final TypedValue result = value.next();
			return result.unwrap(cls);
		}
	}

	private static class MandatoryRawArgConverter implements ArgConverter {
		@Override
		public Object convert(Iterator<TypedValue> value) {
			Preconditions.checkArgument(value.hasNext(), "Missing mandatory argument");
			return value.next();
		}
	}

	private static class OptionalArgConverter implements ArgConverter {
		private final Class<?> cls;

		public OptionalArgConverter(Class<?> cls) {
			Preconditions.checkNotNull(cls);
			this.cls = cls;
		}

		@Override
		public Object convert(Iterator<TypedValue> value) {
			if (value.hasNext()) {
				final TypedValue result = value.next();
				return Optional.of(result.unwrap(cls));
			} else {
				return Optional.absent();
			}
		}
	}

	private static class OptionalRawArgConverter implements ArgConverter {
		@Override
		public Object convert(Iterator<TypedValue> value) {
			if (value.hasNext()) {
				final TypedValue result = value.next();
				return Optional.of(result);
			} else {
				return Optional.absent();
			}
		}
	}

	private static class VariadicArgConverter implements ArgConverter {
		private final Class<?> cls;

		public VariadicArgConverter(Class<?> cls) {
			Preconditions.checkNotNull(cls);
			this.cls = cls;
		}

		@Override
		public Object convert(Iterator<TypedValue> value) {
			final List<TypedValue> values = Lists.newArrayList(value);

			final Object result = Array.newInstance(cls, values.size());
			for (int i = 0; i < values.size(); i++) {
				final TypedValue v = values.get(i);
				final Object c = v.unwrap(cls);
				Array.set(result, i, c);
			}

			return result;
		}
	}

	private static class VariadicRawArgConverter implements ArgConverter {
		@Override
		public Object convert(Iterator<TypedValue> value) {
			final List<TypedValue> values = Lists.newArrayList(value);

			final Object result = Array.newInstance(TypedValue.class, values.size());
			for (int i = 0; i < values.size(); i++) {
				final TypedValue v = values.get(i);
				Array.set(result, i, v);
			}

			return result;
		}
	}

	private abstract static class TypeVariant {
		private final Method method;

		private final Map<Integer, DispatchArgMatcher> dispatchArgMatchers;

		private final List<ArgConverter> argConverters;

		private final int mandatoryArgNum;

		private final int lastDispatchArg;

		public TypeVariant(Method method, Map<Integer, DispatchArgMatcher> dispatchArgMatchers, List<ArgConverter> argConverters, int mandatoryArgNum) {
			this.method = method;
			this.dispatchArgMatchers = ImmutableMap.copyOf(dispatchArgMatchers);
			this.argConverters = argConverters;
			this.mandatoryArgNum = mandatoryArgNum;
			this.lastDispatchArg = dispatchArgMatchers.isEmpty()? -1 : Ordering.natural().max(dispatchArgMatchers.keySet());
		}

		private boolean isMatchAmbigous(TypeVariant other) {
			final int thisLength = this.method.getParameterTypes().length;
			final int otherLength = other.method.getParameterTypes().length;
			for (int i = 0; i < Math.max(thisLength, otherLength); i++) {
				final DispatchArgMatcher ownMatcher = i < thisLength? this.dispatchArgMatchers.get(i) : DispatchArgMatcher.MISSING;
				final DispatchArgMatcher otherMatcher = i < otherLength? other.dispatchArgMatchers.get(i) : DispatchArgMatcher.MISSING;
				if (ownMatcher != null && otherMatcher != null && !ownMatcher.isAmbiguous(otherMatcher)) return false;
			}

			return true;
		}

		private boolean matchDispatchArgs(List<TypedValue> args) {
			final int argCount = args.size();
			for (Map.Entry<Integer, DispatchArgMatcher> m : dispatchArgMatchers.entrySet()) {
				final int matchedArgIndex = m.getKey();
				final Class<?> matchedArgType = matchedArgIndex < argCount? args.get(matchedArgIndex).type : MissingType.class;
				if (!m.getValue().match(matchedArgType)) return false;
			}

			return true;
		}

		private List<Object> convertArgs(TypeDomain domain, List<TypedValue> args) {
			final List<Object> results = Lists.newArrayList();

			for (TypedValue v : args)
				Preconditions.checkArgument(v.domain == domain, "Mixed domain on arg %s", v);

			final Iterator<TypedValue> argsIterator = args.iterator();
			for (ArgConverter converter : argConverters)
				results.add(converter.convert(argsIterator));

			Preconditions.checkState(!argsIterator.hasNext(), "Unconverted args!");

			return results;
		}

		protected abstract List<TypedValue> convertResult(TypeDomain domain, Object result);

		public List<TypedValue> execute(TypeDomain domain, Object target, List<TypedValue> args) {
			try {
				final List<Object> unwrappedArgs = convertArgs(domain, args);
				Object result = method.invoke(target, unwrappedArgs.toArray());
				return convertResult(domain, result);
			} catch (Exception e) {
				throw new MethodInvokeException(method, e);
			}
		}
	}

	private static DispatchArgMatcher createMatcher(Class<?> argType, DispatchArg annotation, Class<?>... extraTypes) {
		final Set<Class<?>> dispatchArgsTypes = Sets.newHashSet(annotation.extra());
		dispatchArgsTypes.addAll(Arrays.asList(extraTypes));

		dispatchArgsTypes.add(argType);
		return new DispatchArgMatcher(dispatchArgsTypes);
	}

	private static DispatchArgMatcher createMatcher(RawDispatchArg annotation, Class<?>... extraTypes) {
		final Set<Class<?>> dispatchArgsTypes = Sets.newHashSet(annotation.value());
		Preconditions.checkArgument(!dispatchArgsTypes.isEmpty(), "Raw dispatch arg must specify dispatch types");

		dispatchArgsTypes.addAll(Arrays.asList(extraTypes));

		return new DispatchArgMatcher(dispatchArgsTypes);
	}

	private static TypeVariant createVariant(final Method method) {
		final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		final Type[] parameterTypes = method.getGenericParameterTypes();
		final int parameterCount = parameterTypes.length;

		int optionalArgsStart = -1;

		final boolean isVariadic = method.isVarArgs();

		final List<ArgConverter> argConverters = Lists.newArrayList();
		final Map<Integer, DispatchArgMatcher> argMatchers = Maps.newHashMap();

		for (int i = 0; i < parameterCount; i++) {
			final TypeToken<?> type = TypeToken.of(parameterTypes[i]);
			final AnnotationMap annotations = new AnnotationMap(parameterAnnotations[i]);

			if (annotations.hasAnnotation(OptionalArgs.class)) optionalArgsStart = i;

			final DispatchArg dispatchAnn = annotations.get(DispatchArg.class);
			final boolean isTypedDispatch = dispatchAnn != null;

			final RawDispatchArg dispatchRawAnn = annotations.get(RawDispatchArg.class);
			final boolean isRawDispatch = dispatchRawAnn != null;

			final boolean isRawNonDispatch = annotations.hasAnnotation(RawArg.class);

			final boolean isRawArg = isRawNonDispatch || isRawDispatch;
			final boolean isDispatchArg = isTypedDispatch || isRawDispatch;

			final boolean isVariadicArg = isVariadic && (i == parameterCount - 1);

			Preconditions.checkArgument(!(isTypedDispatch && isRawNonDispatch), "Argument cannot be both dispatch and raw");
			Preconditions.checkArgument(!(isRawDispatch && isRawNonDispatch), "Argument cannot be both raw and raw dispatch");
			Preconditions.checkArgument(!(isRawDispatch && isTypedDispatch), "Argument cannot be both raw and typed dispatch");
			Preconditions.checkArgument(!(isVariadicArg && isDispatchArg), "Variadic arguments cannot be used for dispatch");

			if (isVariadicArg) {
				final Class<?> componentType = type.getComponentType().getRawType();
				if (isRawArg) {
					Preconditions.checkState(TypedValue.class.isAssignableFrom(componentType), "Raw argument must have TypedValue type");
					argConverters.add(new VariadicRawArgConverter());
				} else {
					argConverters.add(new VariadicArgConverter(componentType));

				}
			} else if (optionalArgsStart >= 0) {
				Preconditions.checkState(Optional.class.isAssignableFrom(type.getRawType()), "Optional argument must have Optional type");
				final Class<?> varType = OptionalTypeHolder.resolve(type);
				if (isRawArg) {
					Preconditions.checkState(TypedValue.class.isAssignableFrom(varType), "Raw argument must have TypedValue type");
					argConverters.add(new OptionalRawArgConverter());
					if (isDispatchArg) argMatchers.put(i, createMatcher(dispatchRawAnn, MissingType.class));
				} else {
					argConverters.add(new OptionalArgConverter(varType));
					if (isDispatchArg) argMatchers.put(i, createMatcher(varType, dispatchAnn, MissingType.class));
				}
			} else {
				final Class<?> rawType = type.getRawType();
				if (isRawArg) {
					Preconditions.checkState(TypedValue.class.isAssignableFrom(rawType), "Raw argument must have TypedValue type");
					argConverters.add(new MandatoryRawArgConverter());
					if (isDispatchArg) argMatchers.put(i, createMatcher(dispatchRawAnn));
				} else {
					argConverters.add(new MandatoryArgConverter(rawType));
					if (isDispatchArg) argMatchers.put(i, createMatcher(rawType, dispatchAnn));
				}
			}
		}

		final int mandatoryArgCount = optionalArgsStart >= 0? optionalArgsStart : parameterCount;
		final boolean isCollectionReturn = method.isAnnotationPresent(MultiReturn.class);
		final boolean isRawReturn = method.isAnnotationPresent(RawReturn.class);

		final Class<?> returnType = method.getReturnType();
		if (MultipleReturn.class.isAssignableFrom(returnType)) {
			Preconditions.checkState(!isRawReturn, "Method returning MultipleReturn cannot be marked as @RawReturn");
			Preconditions.checkState(!isCollectionReturn, "Method returning MultipleReturn cannot be marked as @MultiReturn");
			class MultipleReturnVariant extends TypeVariant {
				public MultipleReturnVariant() {
					super(method, argMatchers, argConverters, mandatoryArgCount);
				}

				@Override
				protected List<TypedValue> convertResult(TypeDomain domain, Object result) {
					final MultipleReturn results = (MultipleReturn)result;

					for (TypedValue v : results.rets)
						Preconditions.checkArgument(v.domain == domain, "Mixed domain on result %s", v);

					return Arrays.asList(results.rets);
				}
			}

			return new MultipleReturnVariant();
		} else if (isCollectionReturn) {
			Preconditions.checkState(!isRawReturn, "Method marked as @MultiReturn cannot be marked as @RawReturn");
			if (returnType.isArray()) {
				final Class<?> componentType = returnType.getComponentType();
				class ArrayReturnVariant extends TypeVariant {
					public ArrayReturnVariant() {
						super(method, argMatchers, argConverters, mandatoryArgCount);
					}

					@Override
					protected List<TypedValue> convertResult(TypeDomain domain, Object result) {
						final List<TypedValue> ret = Lists.newArrayList();

						final int returnSize = Array.getLength(result);
						for (int i = 0; i < returnSize; i++) {
							final Object v = Array.get(result, i);
							ret.add(domain.castAndCreate(componentType, v));
						}

						return ret;
					}
				}

				return new ArrayReturnVariant();
			} else if (Iterable.class.isAssignableFrom(returnType)) {
				final Class<?> componentType = IterableTypeHolder.resolve(method.getGenericReturnType());
				class IterableReturnVariant extends TypeVariant {
					public IterableReturnVariant() {
						super(method, argMatchers, argConverters, mandatoryArgCount);
					}

					@Override
					protected List<TypedValue> convertResult(TypeDomain domain, Object result) {
						final List<TypedValue> ret = Lists.newArrayList();
						final Iterable<?> results = (Iterable<?>)result;

						for (Object v : results) {
							ret.add(domain.castAndCreate(componentType, v));
						}

						return ret;
					}
				}

				return new IterableReturnVariant();
			} else {
				throw new IllegalArgumentException("Method " + method + " is marked with @MultiReturn, but does not return array or Iterable");
			}
		} else if (isRawReturn) {
			Preconditions.checkState(TypedValue.class.isAssignableFrom(method.getReturnType()), "Method marked with @RawReturn must return TypedValue");
			class RawSingleReturnVariant extends TypeVariant {

				public RawSingleReturnVariant() {
					super(method, argMatchers, argConverters, mandatoryArgCount);
				}

				@Override
				protected List<TypedValue> convertResult(TypeDomain domain, Object result) {
					return Lists.newArrayList((TypedValue)result);
				}
			}
			return new RawSingleReturnVariant();
		} else {
			class SingleReturnVariant extends TypeVariant {
				public SingleReturnVariant() {
					super(method, argMatchers, argConverters, mandatoryArgCount);
				}

				@Override
				public List<TypedValue> convertResult(TypeDomain domain, Object result) {
					return wrapArg(domain, result, returnType);
				}

				private <T> List<TypedValue> wrapArg(TypeDomain domain, Object result, Class<T> returnType) {
					final T castResult = returnType.cast(result);
					return Lists.newArrayList(domain.create(returnType, castResult));
				}
			}

			return new SingleReturnVariant();
		}
	}

	private abstract static class TypedFunctionBody {
		private final OptionalInt mandatoryArgNum;

		private TypedFunctionBody(OptionalInt mandatoryArgNum) {
			this.mandatoryArgNum = mandatoryArgNum;
		}

		public void call(TypeDomain domain, Object target, Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			final int argCount;

			if (argumentsCount.isPresent()) {
				argCount = argumentsCount.get();
			} else {
				Preconditions.checkState(mandatoryArgNum.isPresent(), "Number of arguments not given and function is not fixed");
				argCount = mandatoryArgNum.get();
			}

			final List<TypedValue> reversedArgs = Lists.newArrayList();
			final Stack<TypedValue> stack = frame.stack();
			for (int i = 0; i < argCount; i++)
				reversedArgs.add(stack.pop());
			final List<TypedValue> args = Lists.reverse(reversedArgs);

			final List<TypedValue> returns = execute(domain, target, args);

			if (returnsCount.isPresent()) {
				final Integer expectedReturns = returnsCount.get();
				final int actualReturns = returns.size();
				Preconditions.checkState(expectedReturns == actualReturns, "Invalid number of return values, requested %s, got %s", expectedReturns, actualReturns);
			}

			stack.pushAll(returns);
		}

		protected abstract List<TypedValue> execute(TypeDomain domain, Object target, List<TypedValue> args);
	}

	protected final TypedFunctionBody body;

	private TypedFunction(TypedFunctionBody body) {
		this.body = body;
	}

	public interface IUnboundCallable {
		public void call(TypeDomain domain, Object target, Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount);
	}

	public static class Unbound extends TypedFunction implements IUnboundCallable {
		private final Class<?> targetCls;

		public Unbound(Class<?> targetCls, TypedFunctionBody body) {
			super(body);
			this.targetCls = targetCls;
		}

		@Override
		public void call(TypeDomain domain, Object target, Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			Preconditions.checkState(targetCls.isInstance(target));
			body.call(domain, target, frame, argumentsCount, returnsCount);
		}
	}

	public static class Bound extends TypedFunction implements ICallable<TypedValue> {
		private final TypeDomain domain;
		private final Object target;

		public Bound(TypeDomain domain, Object target, TypedFunctionBody body) {
			super(body);
			this.domain = domain;
			this.target = target;
		}

		@Override
		public void call(Frame<TypedValue> frame, OptionalInt argumentsCount, OptionalInt returnsCount) {
			body.call(domain, target, frame, argumentsCount, returnsCount);
		}
	}

	public static Builder builder() {
		return new Builder();
	}

}
