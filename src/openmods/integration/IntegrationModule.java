package openmods.integration;

import openmods.conditions.ICondition;

import com.google.common.base.Preconditions;

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
