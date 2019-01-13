package openmods.gui.logic;

public interface IValueUpdateAction {

	Iterable<?> getTriggers();

	void execute();

}
