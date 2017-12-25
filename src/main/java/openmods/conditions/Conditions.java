package openmods.conditions;

public class Conditions {
	public static ICondition any(final ICondition... conditions) {
		return new ICondition() {
			@Override
			public boolean check() {
				for (ICondition c : conditions)
					if (c.check()) return true;

				return false;
			}
		};
	}

	public static ICondition all(final ICondition... conditions) {
		return () -> {
			for (ICondition c : conditions)
				if (!c.check()) return false;

			return true;
		};
	}

	public static ICondition not(final ICondition condition) {
		return () -> !condition.check();
	}

	public static ICondition alwaysTrue() {
		return () -> true;
	}

	public static ICondition alwaysFalse() {
		return () -> false;
	}
}
