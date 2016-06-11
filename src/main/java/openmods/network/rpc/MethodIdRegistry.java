package openmods.network.rpc;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Sets;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import openmods.datastore.IDataVisitor;
import org.apache.commons.lang3.ClassUtils;
import org.objectweb.asm.Type;

public class MethodIdRegistry implements IDataVisitor<String, Integer> {
	private final Set<Class<?>> registeredInterfaces = Sets.newHashSet();

	private final BiMap<Method, Integer> methodIds = HashBiMap.create();

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

	private static Method identifyMethod(String methodDesc) throws Exception {
		String[] parts = methodDesc.split(RpcSetup.ID_FIELDS_SEPARATOR, 3);
		Preconditions.checkArgument(parts.length == 3, "Method descriptor has %d fields", parts.length);
		Class<?> declaringCls = Class.forName(parts[0]);

		org.objectweb.asm.commons.Method method = new org.objectweb.asm.commons.Method(parts[1], parts[2]);

		Type[] argTypes = method.getArgumentTypes();
		Class<?>[] argCls = convertTypesToClasses(argTypes, declaringCls.getClassLoader());

		try {
			return declaringCls.getMethod(method.getName(), argCls);
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(
					String.format("Can't find method, class %s has methods %s",
							declaringCls, Arrays.toString(declaringCls.getMethods())),
					e);
		}
	}

	private static Class<?>[] convertTypesToClasses(Type[] argTypes, ClassLoader loader) throws ClassNotFoundException {
		Class<?>[] argCls = new Class<?>[argTypes.length];
		for (int i = 0; i < argCls.length; i++)
			argCls[i] = ClassUtils.getClass(loader, argTypes[i].getClassName(), false);
		return argCls;
	}

	@Override
	public void entry(String methodDesc, Integer id) {
		final Method method;
		try {
			method = identifyMethod(methodDesc);
		} catch (Throwable e) {
			throw new IllegalArgumentException(String.format("Malformed entry '%s' in method id %d", methodDesc, id), e);
		}

		MethodParamsCodec.create(method).validate();

		methodIds.put(method, id);

		final Class<?> declaringClass = method.getDeclaringClass();
		registeredInterfaces.add(declaringClass);
	}

	@Override
	public void end() {}

}
