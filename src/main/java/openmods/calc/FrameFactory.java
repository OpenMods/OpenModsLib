package openmods.calc;

import openmods.utils.Stack;

public class FrameFactory {

	public static <E> Frame<E> newLocalFrameWithSubstack(Frame<E> enclosingFrame, int depth) {
		return new Frame<E>(new LocalSymbolMap<E>(enclosingFrame.symbols()), enclosingFrame.stack().substack(depth));
	}

	public static <E> Frame<E> newLocalFrame(Frame<E> enclosingFrame) {
		return new Frame<E>(new LocalSymbolMap<E>(enclosingFrame.symbols()), new Stack<E>());
	}

	public static <E> Frame<E> newProtectionFrameWithSubstack(Frame<E> enclosingFrame, int depth) {
		return new Frame<E>(new ProtectionSymbolMap<E>(enclosingFrame.symbols()), enclosingFrame.stack().substack(depth));
	}

	public static <E> Frame<E> createTopFrame() {
		return new Frame<E>(new TopSymbolMap<E>(), new Stack<E>());
	}

}
