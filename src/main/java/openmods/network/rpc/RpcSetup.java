package openmods.network.rpc;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.LoaderState;
import java.lang.reflect.Method;
import java.util.Map;
import openmods.datastore.DataStoreBuilder;
import openmods.network.IdSyncManager;
import openmods.utils.io.TypeRW;
import org.objectweb.asm.Type;

public class RpcSetup {

	public static final String ID_FIELDS_SEPARATOR = ";";

	private int currentMethodId = 0;
	private int currentWrapperId = 0;

	private final DataStoreBuilder<String, Integer> methodsStoreBuilder;

	private final Map<String, Method> methods = Maps.newHashMap();

	private final DataStoreBuilder<String, Integer> targetsStoreBuilder;

	private final Map<String, Class<? extends IRpcTarget>> targets = Maps.newHashMap();

	RpcSetup() {
		this.methodsStoreBuilder = IdSyncManager.INSTANCE.createDataStore("rpc_methods", String.class, Integer.class);

		this.methodsStoreBuilder.setDefaultKeyReaderWriter();
		this.methodsStoreBuilder.setValueReaderWriter(TypeRW.VLI_SERIALIZABLE);

		this.targetsStoreBuilder = IdSyncManager.INSTANCE.createDataStore("rpc_targets", String.class, Integer.class);

		this.targetsStoreBuilder.setDefaultKeyReaderWriter();
		this.targetsStoreBuilder.setValueReaderWriter(TypeRW.VLI_SERIALIZABLE);
	}

	public RpcSetup registerInterface(Class<?> intf) {
		Preconditions.checkState(Loader.instance().isInState(LoaderState.PREINITIALIZATION), "This method can only be called in pre-initialization state");

		Preconditions.checkArgument(intf.isInterface(), "Class %s is not interface", intf);

		for (Method m : intf.getMethods()) {
			if (m.isAnnotationPresent(RpcIgnore.class)) continue;
			Preconditions.checkArgument(m.getReturnType() == void.class, "RPC methods cannot have return type (method = %s)", m);
			MethodParamsCodec.create(m).validate();

			String desc = Type.getMethodDescriptor(m);
			String entry = m.getDeclaringClass().getName() + ID_FIELDS_SEPARATOR + m.getName() + ID_FIELDS_SEPARATOR + desc;

			if (!methodsStoreBuilder.isRegistered(entry)) {
				methodsStoreBuilder.addEntry(entry, currentMethodId++);
				methods.put(entry, m);
			}
		}
		return this;
	}

	public RpcSetup registerTargetWrapper(Class<? extends IRpcTarget> wrapperCls) {
		Preconditions.checkState(Loader.instance().isInState(LoaderState.PREINITIALIZATION), "This method can only be called in pre-initialization state");
		final String key = wrapperCls.getName();
		targetsStoreBuilder.addEntry(key, currentWrapperId++);
		targets.put(key, wrapperCls);
		return this;
	}

	void finish(MethodIdRegistry methodRegistry, TargetWrapperRegistry wrapperRegistry) {
		methodRegistry.addMethods(methods);
		methodsStoreBuilder.addVisitor(methodRegistry);
		methodsStoreBuilder.register();

		wrapperRegistry.addTargets(targets);
		targetsStoreBuilder.addVisitor(wrapperRegistry);
		targetsStoreBuilder.register();
	}

}
