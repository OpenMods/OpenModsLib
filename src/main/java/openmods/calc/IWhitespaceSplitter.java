package openmods.calc;

public interface IWhitespaceSplitter {
	public String getNextPart();

	public String getTail();

	public boolean isFinished();
}
