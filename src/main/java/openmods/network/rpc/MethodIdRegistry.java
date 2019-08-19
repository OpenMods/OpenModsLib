package openmods.network.rpc;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import openmods.datastore.IDataVisitor;

public class MethodIdRegistry implements IDataVisitor<String, Integer> {
	private final Set<Class<?>> registeredInterfaces = Sets.newHashSet();

	private final BiMap<Method, Integer> methodIds = HashBiMap.create();

	private Map<String, Method> methods = ImmutableMap.of();

	public boolean isClassRegistered(Class<?> cls) {
		return registeredInterfaces.contains(cls);
	}

	public int methodToId(Method method) {
		Integer id = methodIds.get(method);
		Preconditions.checkNotNull(id, "Method %s is ignored or not registered", method);
		return id;
	}

	public Method idToMethod(int id) {
		Method method = methodIds.inverse().get(id);
		Preconditions.checkNotNull(method, "Unregistered method id %s", id);
		return method;
	}

	@Override
	public void begin(int size) {
		registeredInterfaces.clear();
		methodIds.clear();
	}

	@Override
	public void entry(String methodDesc, Integer id) {
		final Method method = methods.get(methodDesc);
		if (method == null) {
			throw new IllegalArgumentException("Can't find method " + methodDesc);
		}
		MethodParamsCodec.create(method).validate();

		methodIds.put(method, id);

		final Class<?> declaringClass = method.getDeclaringClass();
		registeredInterfaces.add(declaringClass);
	}

	@Override
	public void end() {}

	public void addMethods(final Map<String, Method> methods) {
		this.methods = ImmutableMap.copyOf(methods);
	}

}
