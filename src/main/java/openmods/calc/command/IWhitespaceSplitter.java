package openmods.calc.command;

public interface IWhitespaceSplitter {
	public String getNextPart();

	public String getTail();

	public boolean isFinished();
}
