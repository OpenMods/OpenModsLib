package openmods.integration;

import com.google.common.base.Preconditions;
import openmods.conditions.ICondition;

public abstract class IntegrationModule implements IIntegrationModule {
	private final ICondition condition;

	public IntegrationModule(ICondition condition) {
		Preconditions.checkNotNull(condition, "Invalid use");
		this.condition = condition;
	}

	@Override
	public boolean canLoad() {
		return condition.check();
	}

}
