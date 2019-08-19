package openmods.network.rpc;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import openmods.datastore.IDataVisitor;

public class TargetWrapperRegistry implements IDataVisitor<String, Integer> {

	private Map<String, Class<? extends IRpcTarget>> targets = ImmutableMap.of();

	private BiMap<Class<? extends IRpcTarget>, Integer> wrapperCls = HashBiMap.create();

	@Override
	public void begin(int size) {
		wrapperCls.clear();
	}

	@Override
	public void entry(String clsName, Integer clsId) {
		Class<? extends IRpcTarget> cls = targets.get(clsName);

		try {
			cls.getConstructor();
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException(String.format("Class %s has no parameterless constructor", clsName), e);
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}

		this.wrapperCls.put(cls, clsId);
	}

	@Override
	public void end() {}

	public int getWrapperId(Class<? extends IRpcTarget> cls) {
		Integer id = wrapperCls.get(cls);
		Preconditions.checkNotNull(id, "Wrapper class %s is not registered", cls);
		return id;
	}

	public IRpcTarget createWrapperFromId(int id) {
		Class<? extends IRpcTarget> cls = wrapperCls.inverse().get(id);
		Preconditions.checkNotNull(cls, "Can't find class for id %s", id);

		try {
			return cls.newInstance();
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	public void addTargets(final Map<String, Class<? extends IRpcTarget>> targets) {
		this.targets = ImmutableMap.copyOf(targets);
	}
}
