package openmods.calc.types.multi;

import com.google.common.base.Preconditions;
import java.util.List;
import openmods.calc.ExecutableList;
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
				|| (obj instanceof Code && ((Code)obj).code.equals(this.code));
	}

	public static TypedValue flattenAndWrap(TypeDomain domain, IExprNode<TypedValue> expr) {
		return domain.create(Code.class, new Code(ExprUtils.flattenNode(expr)));
	}

	public static TypedValue wrap(TypeDomain domain, IExecutable<TypedValue> executable) {
		return domain.create(Code.class, new Code(executable));
	}

	public static TypedValue wrap(TypeDomain domain, List<IExecutable<TypedValue>> executables) {
		return wrap(domain, ExecutableList.wrap(executables));
	}

	@Override
	public String toString() {
		return "Code{" + code + "}";
	}

}
