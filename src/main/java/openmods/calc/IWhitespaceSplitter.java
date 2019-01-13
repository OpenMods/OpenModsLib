package openmods.calc;

public interface IWhitespaceSplitter {
	String getNextPart();

	String getTail();

	boolean isFinished();
}
