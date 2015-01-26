package openmods.utils.io;

public interface INBTSerializer<T> extends INbtReader<T>, INbtWriter<T>, INbtChecker<T> {}