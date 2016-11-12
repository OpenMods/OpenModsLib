package openmods.calc;

import openmods.utils.Stack;

public class FrameFactory {

	public static <E> Frame<E> newLocalFrameWithSubstack(Frame<E> enclosingFrame, int depth) {
		return new Frame<E>(new LocalSymbolMap<E>(enclosingFrame.symbols()), enclosingFrame.stack().substack(depth));
	}

	public static <E> Frame<E> newLocalFrame(SymbolMap<E> parentSymbols) {
		return new Frame<E>(new LocalSymbolMap<E>(parentSymbols), new Stack<E>());
	}

	public static <E> Frame<E> newLocalFrame(Frame<E> enclosingFrame) {
		return newLocalFrame(enclosingFrame.symbols());
	}

	public static <E> Frame<E> symbolsToFrame(SymbolMap<E> symbols) {
		return new Frame<E>(symbols, new Stack<E>());
	}

	public static <E> Frame<E> newProtectionFrameWithSubstack(Frame<E> enclosingFrame, int depth) {
		return new Frame<E>(new ProtectionSymbolMap<E>(enclosingFrame.symbols()), enclosingFrame.stack().substack(depth));
	}

	public static <E> Frame<E> createProtectionFrame(SymbolMap<E> symbols) {
		return new Frame<E>(new ProtectionSymbolMap<E>(symbols), new Stack<E>());
	}

	public static <E> Frame<E> newClosureFrame(SymbolMap<E> scopeSymbols, Frame<E> stackFrame, int depth) {
		return new Frame<E>(new LocalSymbolMap<E>(scopeSymbols), stackFrame.stack().substack(depth));
	}

	public static <E> Frame<E> createTopFrame(SymbolMap<E> topSymbolMap) {
		return new Frame<E>(topSymbolMap, new Stack<E>());
	}

	public static <E> Frame<E> createTopFrame() {
		return createTopFrame(new TopSymbolMap<E>());
	}

}
