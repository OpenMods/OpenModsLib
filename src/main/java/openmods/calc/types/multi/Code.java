package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import openmods.calc.ICalculatorFrame;
import openmods.calc.IExecutable;

public class Code {
	private final IExecutable<TypedValue> code;

	public Code(IExecutable<TypedValue> code) {
		Preconditions.checkNotNull(code);
		this.code = code;
	}

	public void execute(ICalculatorFrame<TypedValue> frame) {
		this.code.execute(frame);
	}

	@Override
	public int hashCode() {
		return code.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return (this == obj)
				|| (obj instanceof Code && ((Code)obj).equals(this.code));
	}

}
