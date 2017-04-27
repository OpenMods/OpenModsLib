package openmods.calc;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import openmods.calc.executable.IExecutable;

public class Compilers<E, M> {
	public interface ICompiler<E> {
		public IExecutable<E> compile(String input);
	}

	private final Map<M, ICompiler<E>> compilers;

	public Compilers(Map<M, ICompiler<E>> compilers) {
		this.compilers = ImmutableMap.copyOf(compilers);
	}

	public IExecutable<E> compile(M type, String input) {
		final ICompiler<E> compiler = compilers.get(type);
		Preconditions.checkArgument(compiler != null, "Unknown compiler: " + type);
		return compiler.compile(input);
	}

}
