package openmods.calc.types.multi;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import openmods.calc.ICalculatorFrame;
import openmods.calc.ISymbol;
import openmods.reflection.TypeVariableHolder;
import openmods.utils.AnnotationMap;
import openmods.utils.Stack;
import org.apache.commons.lang3.tuple.Pair;

public abstract class TypedFunction implements ISymbol<TypedValue> {

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
	public @interface MultiReturn {}

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface Variant {}

	@Target(ElementType.PARAMETER)
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface RawArg {}

	@Target(ElementType.PARAMETER)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface OptionalArgs {}

	@Target(ElementType.PARAMETER)
	@Retention(RetentionPolicy.RUNTIME)
	public @interface DispatchArg {
		public Class<?>[] extra() default {};
	}

	public static class Builder {
		private final TypeDomain domain;

		private final List<Pair<Object, Method>> variants = Lists.newArrayList();

		public Builder(TypeDomain domain) {
			this.domain = domain;
		}

		public Builder addVariant(Object target, Method method) {
			method.setAccessible(true);
			variants.add(Pair.of(target, method));
			return this;
		}

		public <T> Builder addVariants(T target, Class<? super T> cls) {
			for (Method m : cls.getMethods())
				if (m.isAnnotationPresent(Variant.class))
					addVariant(target, m);

			return this;
		}

		public TypedFunction build() {
			Preconditions.checkArgument(!this.variants.isEmpty(), "No variants defined");

			final List<TypeVariant> variants = Lists.newArrayList();

			for (Pair<Object, Method> p : this.variants) {
				try {
					variants.add(createVariant(p.getLeft(), p.getRight(), domain));
				} catch (Exception e) {
					throw new RuntimeException("Failed to register method " + p.getRight(), e);
				}
			}

			if (variants.size() == 1) {
				return createSingleFunction(variants.get(0));
			} else {
				verifyVariants(variants);
				final Optional<Integer> mandatoryArgNum = calculateMandatoryArgNum(variants);
				return createMultiFunction(variants, mandatoryArgNum);
			}
		}

		private static TypedFunction createSingleFunction(final TypeVariant variant) {
			return new TypedFunction(Optional.of(variant.mandatoryArgNum)) {
				@Override
				protected List<TypedValue> execute(List<TypedValue> args) {
					if (!variant.matchDispatchArgs(args)) throw new DispatchException(args);
					return variant.execute(args);
				}
			};
		}

		private static TypedFunction createMultiFunction(final List<TypeVariant> variants, final Optional<Integer> mandatoryArgNum) {
			return new TypedFunction(mandatoryArgNum) {
				@Override
				protected List<TypedValue> execute(List<TypedValue> args) {
					for (TypeVariant v : variants)
						if (v.matchDispatchArgs(args)) return v.execute(args);

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

		private static Optional<Integer> calculateMandatoryArgNum(List<TypeVariant> variants) {
			Optional<Integer> result = Optional.absent();
			for (TypeVariant v : variants) {
				if (result.isPresent()) {
					if (result.get() != v.mandatoryArgNum) return Optional.absent();
				} else {
					result = Optional.of(v.mandatoryArgNum);
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
		private final TypeDomain domain;

		private final Object target;

		private final Method method;

		private final Map<Integer, DispatchArgMatcher> dispatchArgMatchers;

		private final List<ArgConverter> argConverters;

		private final int mandatoryArgNum;

		public TypeVariant(TypeDomain domain, Object target, Method method, Map<Integer, DispatchArgMatcher> dispatchArgMatchers, List<ArgConverter> argConverters, int mandatoryArgNum) {
			this.domain = domain;
			this.target = target;
			this.method = method;
			this.dispatchArgMatchers = ImmutableMap.copyOf(dispatchArgMatchers);
			this.argConverters = argConverters;
			this.mandatoryArgNum = mandatoryArgNum;
		}

		public boolean isMatchAmbigous(TypeVariant other) {
			final int thisLength = this.method.getParameterTypes().length;
			final int otherLength = other.method.getParameterTypes().length;
			for (int i = 0; i < Math.max(thisLength, otherLength); i++) {
				final DispatchArgMatcher ownMatcher = i < thisLength? this.dispatchArgMatchers.get(i) : DispatchArgMatcher.MISSING;
				final DispatchArgMatcher otherMatcher = i < otherLength? other.dispatchArgMatchers.get(i) : DispatchArgMatcher.MISSING;
				if (ownMatcher != null && otherMatcher != null && !ownMatcher.isAmbiguous(otherMatcher)) return false;
			}

			return true;
		}

		public boolean matchDispatchArgs(List<TypedValue> args) {
			final int argCount = args.size();
			for (Map.Entry<Integer, DispatchArgMatcher> m : dispatchArgMatchers.entrySet()) {
				final int matchedArgIndex = m.getKey();
				final Class<?> matchedArgType = matchedArgIndex < argCount? args.get(matchedArgIndex).type : MissingType.class;
				if (!m.getValue().match(matchedArgType)) return false;
			}

			return true;
		}

		public List<Object> convertArgs(List<TypedValue> args) {
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

		public List<TypedValue> execute(List<TypedValue> args) {
			try {
				final List<Object> unwrappedArgs = convertArgs(args);
				Object result = method.invoke(target, unwrappedArgs.toArray());
				return convertResult(domain, result);
			} catch (Exception e) {
				throw new MethodInvokeException(method, e);
			}
		}
	}

	private static DispatchArgMatcher createMatcher(TypeDomain domain, Class<?> argType, DispatchArg annotation, Class<?>... extraTypes) {
		final Set<Class<?>> dispatchArgsTypes = Sets.newHashSet(annotation.extra());
		dispatchArgsTypes.addAll(Arrays.asList(extraTypes));

		for (Class<?> cls : dispatchArgsTypes)
			if (cls != MissingType.class) domain.checkConversion(cls, argType);

		dispatchArgsTypes.add(argType);
		return new DispatchArgMatcher(dispatchArgsTypes);
	}

	public static TypeVariant createVariant(final Object target, final Method method, final TypeDomain typeDomain) {
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
			final boolean isRaw = annotations.hasAnnotation(RawArg.class);
			Preconditions.checkArgument(!(dispatchAnn != null && isRaw), "Argument cannot be both dispatch and raw");

			final boolean isVariadicArg = isVariadic && (i == parameterCount - 1);
			Preconditions.checkArgument(!(dispatchAnn != null && isVariadicArg), "Variadic arguments cannot be used for dispatch");

			if (isVariadicArg) {
				final Class<?> componentType = type.getComponentType().getRawType();
				if (isRaw) {
					Preconditions.checkState(TypedValue.class.isAssignableFrom(componentType), "Raw argument must have TypedValue type");
					argConverters.add(new VariadicRawArgConverter());
				} else {
					Preconditions.checkState(typeDomain.isKnownType(componentType), "Argument %s is not valid in domain", componentType);
					argConverters.add(new VariadicArgConverter(componentType));

				}
			} else if (optionalArgsStart >= 0) {
				Preconditions.checkState(Optional.class.isAssignableFrom(type.getRawType()), "Optional argument must have Optional type");
				final Class<?> varType = OptionalTypeHolder.resolve(type);
				if (isRaw) {
					Preconditions.checkState(TypedValue.class.isAssignableFrom(varType), "Raw argument must have TypedValue type");
					argConverters.add(new OptionalRawArgConverter());
				} else {
					Preconditions.checkState(typeDomain.isKnownType(varType), "Argument %s is not valid in domain", varType);
					argConverters.add(new OptionalArgConverter(varType));
					if (dispatchAnn != null) argMatchers.put(i, createMatcher(typeDomain, varType, dispatchAnn, MissingType.class));
				}
			} else {
				final Class<?> rawType = type.getRawType();
				if (isRaw) {
					Preconditions.checkState(TypedValue.class.isAssignableFrom(rawType), "Raw argument must have TypedValue type");
					argConverters.add(new MandatoryRawArgConverter());
				} else {
					Preconditions.checkState(typeDomain.isKnownType(rawType), "Argument %s is not valid in domain", type);
					argConverters.add(new MandatoryArgConverter(rawType));
					if (dispatchAnn != null) argMatchers.put(i, createMatcher(typeDomain, rawType, dispatchAnn));
				}
			}
		}

		final int mandatoryArgCount = optionalArgsStart >= 0? optionalArgsStart : parameterCount;
		final boolean isCollectionReturn = method.isAnnotationPresent(MultiReturn.class);

		final Class<?> returnType = method.getReturnType();
		if (MultipleReturn.class.isAssignableFrom(returnType)) {
			class MultipleReturnVariant extends TypeVariant {
				public MultipleReturnVariant() {
					super(typeDomain, target, method, argMatchers, argConverters, mandatoryArgCount);
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
			if (returnType.isArray()) {
				final Class<?> componentType = returnType.getComponentType();
				typeDomain.checkIsKnownType(componentType);
				class ArrayReturnVariant extends TypeVariant {
					public ArrayReturnVariant() {
						super(typeDomain, target, method, argMatchers, argConverters, mandatoryArgCount);
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
				typeDomain.checkIsKnownType(componentType);
				class IterableReturnVariant extends TypeVariant {
					public IterableReturnVariant() {
						super(typeDomain, target, method, argMatchers, argConverters, mandatoryArgCount);
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
		} else {
			typeDomain.checkIsKnownType(returnType);
			class SingleReturnVariant extends TypeVariant {
				public SingleReturnVariant() {
					super(typeDomain, target, method, argMatchers, argConverters, mandatoryArgCount);
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

	private final Optional<Integer> mandatoryArgNum;

	public TypedFunction(Optional<Integer> mandatoryArgNum) {
		this.mandatoryArgNum = mandatoryArgNum;
	}

	@Override
	public void execute(ICalculatorFrame<TypedValue> frame, Optional<Integer> argumentsCount, Optional<Integer> returnsCount) {
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

		final List<TypedValue> returns = execute(args);

		if (returnsCount.isPresent()) {
			final Integer expectedReturns = returnsCount.get();
			final int actualReturns = returns.size();
			Preconditions.checkState(expectedReturns == actualReturns, "Invalid number of return values, requested %s, got %s", expectedReturns, actualReturns);
		}

		for (TypedValue v : returns)
			stack.push(v);
	}

	protected abstract List<TypedValue> execute(List<TypedValue> args);

}
