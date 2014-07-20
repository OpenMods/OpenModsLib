package openmods.gui.logic;

public interface IValueUpdateAction {

	public Iterable<?> getTriggers();

	public void execute();

}
