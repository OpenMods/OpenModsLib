package openmods.utils;

@FunctionalInterface
public interface ITester<T> {
	enum Result {
		ACCEPT,
		REJECT,
		CONTINUE
	}

	Result test(T o);
}