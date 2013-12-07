package openmods.structured;

public interface IStructureContainerFactory<C extends IStructureContainer<?>> {
	public C createContainer(int type);
}
