package openmods.structured;

public interface IStructureContainerFactory<C extends IStructureContainer<?>> {
	C createContainer(int type);
}
