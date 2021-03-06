package openmods.calc;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public abstract class NestedCommandException extends RuntimeException {
	private static final long serialVersionUID = 601967437864511783L;

	private final Object[] args;
	private final List<String> path = Lists.newArrayList();

	public NestedCommandException(String message, Object... args) {
		super(message);
		this.args = args;
	}

	public NestedCommandException(Throwable cause, String message, Object... args) {
		super(message, cause);
		this.args = args;
	}

	public NestedCommandException pushCommandName(String name) {
		path.add(name);
		return this;
	}

	protected String getPath() {
		return Joiner.on("::").join(Lists.reverse(path));
	}

	protected abstract String contents();

	public ITextComponent getChatComponent() {
		return new TextComponentTranslation(contents(), getPath())
				.appendSibling(new TextComponentTranslation(getMessage(), args));
	}
}
