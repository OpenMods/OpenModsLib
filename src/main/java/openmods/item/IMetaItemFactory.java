package openmods.item;

public interface IMetaItemFactory {

	public int getMeta();

	public boolean isEnabled();

	public IMetaItem createMetaItem();
}
