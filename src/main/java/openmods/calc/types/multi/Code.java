package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import openmods.calc.Frame;
import openmods.calc.IExecutable;
import openmods.calc.parsing.ExprUtils;
import openmods.calc.parsing.IExprNode;

public class Code {
	private final IExecutable<TypedValue> code;

	public Code(IExecutable<TypedValue> code) {
		Preconditions.checkNotNull(code);
		this.code = code;
	}

	public void execute(Frame<TypedValue> frame) {
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

	public static TypedValue flattenAndWrap(TypeDomain domain, IExprNode<TypedValue> expr) {
		return domain.create(Code.class, new Code(ExprUtils.flattenNode(expr)));
	}
}
