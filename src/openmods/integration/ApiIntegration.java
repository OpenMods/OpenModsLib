package openmods.integration;

import java.util.List;

import openmods.Log;
import openmods.utils.SafeClassLoad;

import com.google.common.collect.ImmutableList;

public abstract class ApiIntegration implements IIntegrationModule {

	private final List<SafeClassLoad> requiredClasses;

	public ApiIntegration(String... requiredClassesNames) {
		ImmutableList.Builder<SafeClassLoad> builder = ImmutableList.builder();

		for (String clsName : requiredClassesNames)
			builder.add(new SafeClassLoad(clsName));

		requiredClasses = builder.build();
	}

	@Override
	public boolean canLoad() {
		for (SafeClassLoad cls : requiredClasses)
			if (!cls.tryLoad()) {
				Log.info("Can't load integration module '%s', because class %s is missing", name(), cls.clsName);
				return false;
			}

		return true;
	}

}
