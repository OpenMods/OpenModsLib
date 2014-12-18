package openmods.inventory.transfer;

public interface IRevertable {
	public boolean commit();

	public boolean hasChanges();

	public void abort();
}
