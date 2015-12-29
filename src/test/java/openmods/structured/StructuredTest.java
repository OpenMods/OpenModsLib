package openmods.structured;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;

import java.util.List;
import java.util.Map;

import net.minecraft.network.PacketBuffer;
import openmods.structured.Command.Type;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InOrder;

import com.google.common.collect.Lists;

public class StructuredTest {

	public static class IdHolder {
		public int id;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}
	}

	public abstract static class TestElement implements IStructureElement {

	}

	public static class IntTestElement extends TestElement {
		public int value;

		@Override
		public void readFromStream(PacketBuffer input) {
			this.value = input.readInt();
		}

		@Override
		public void writeToStream(PacketBuffer output) {
			output.writeInt(value);
		}
	}

	public static class StringTestElement extends TestElement {
		public String value;

		@Override
		public void readFromStream(PacketBuffer input) {
			this.value = input.readStringFromBuffer(0xFFFF);
		}

		@Override
		public void writeToStream(PacketBuffer output) {
			output.writeString(value);
		}
	}

	public abstract static class TestContainer implements IStructureContainer<TestElement> {}

	public static class IntTestContainer extends TestContainer {

		public static final int TYPE = 0;

		public final IntTestElement element = new IntTestElement();

		@Override
		public int getType() {
			return TYPE;
		}

		@Override
		public void createElements(IElementAddCallback<TestElement> callback) {
			callback.addElement(element);
		}
	}

	public static class StringTestContainer extends TestContainer {
		public static final int TYPE = 1;

		public final StringTestElement element = new StringTestElement();

		@Override
		public int getType() {
			return TYPE;
		}

		@Override
		public void createElements(IElementAddCallback<TestElement> callback) {
			callback.addElement(element);
		}
	}

	public static class CustomDataTestContainer extends TestContainer implements ICustomCreateData {
		public static final int TYPE = 2;

		public final StringTestElement stringElement = new StringTestElement();

		public final IntTestElement intElement = new IntTestElement();

		public long customValue;

		@Override
		public int getType() {
			return TYPE;
		}

		@Override
		public void createElements(IElementAddCallback<TestElement> callback) {
			callback.addElement(stringElement);
			callback.addElement(intElement);
		}

		@Override
		public void readCustomDataFromStream(PacketBuffer input) {
			this.customValue = input.readLong();
		}

		@Override
		public void writeCustomDataFromStream(PacketBuffer output) {
			output.writeLong(customValue);
		}
	}

	public static class TestMaster extends StructuredDataMaster<TestContainer, TestElement> {
		public TestMaster(IStructureObserver<TestContainer, TestElement> observer) {
			super(observer);
		}

		public TestMaster() {
			super();
		}

	}

	public static class TestFactory implements IStructureContainerFactory<TestContainer> {

		@Override
		public TestContainer createContainer(int type) {
			switch (type) {
				case IntTestContainer.TYPE:
					return new IntTestContainer();
				case StringTestContainer.TYPE:
					return new StringTestContainer();
				case CustomDataTestContainer.TYPE:
					return new CustomDataTestContainer();
			}
			throw new IllegalArgumentException(String.format("%d", type));
		}

	}

	public static class TestSlave extends StructuredDataSlave<TestContainer, TestElement> {
		public TestSlave(IStructureObserver<TestContainer, TestElement> observer) {
			super(new TestFactory(), observer);
		}

		public TestSlave() {
			super(new TestFactory());
		}

		@Override
		protected void onConsistencyCheckFail() {
			Assert.fail("Consistency check!");
		}

		public Map<Integer, TestContainer> getContainers() {
			return containers;
		}

		public Map<Integer, TestElement> getElements() {
			return elements;
		}
	}

	private static StringTestContainer createStringContainer(final TestMaster master, String value) {
		final StringTestContainer stringContainer = new StringTestContainer();
		stringContainer.element.value = value;
		master.addContainer(stringContainer);
		return stringContainer;
	}

	private static IntTestContainer createIntContainer(final TestMaster master, int value) {
		final IntTestContainer intContainer = new IntTestContainer();
		intContainer.element.value = value;
		master.addContainer(intContainer);
		return intContainer;
	}

	private static void checkStringContainer(final Map<Integer, TestContainer> containers, int index, String value) {
		final TestContainer c = containers.get(index);
		Assert.assertTrue(c instanceof StringTestContainer);
		final StringTestContainer stringC = (StringTestContainer)c;
		Assert.assertEquals(value, stringC.element.value);
	}

	private static void checkIntContainer(final Map<Integer, TestContainer> containers, int index, int value) {
		final TestContainer c = containers.get(index);
		Assert.assertTrue(c instanceof IntTestContainer);
		final IntTestContainer intC = (IntTestContainer)c;
		Assert.assertEquals(value, intC.element.value);
	}

	private static void checkStringElement(final Map<Integer, TestElement> elements, int index, String value) {
		final TestElement e = elements.get(index);
		Assert.assertTrue(e instanceof StringTestElement);
		final StringTestElement stringE = (StringTestElement)e;
		Assert.assertEquals(value, stringE.value);
	}

	private static void checkIntElement(final Map<Integer, TestElement> elements, int index, int value) {
		final TestElement e = elements.get(index);
		Assert.assertTrue(e instanceof IntTestElement);
		final IntTestElement intE = (IntTestElement)e;
		Assert.assertEquals(value, intE.value);
	}

	private static void performUpdate(TestMaster master, TestSlave slave, Command.Type... types) {
		final List<Command> commands = Lists.newArrayList();
		master.appendUpdateCommands(commands);

		Assert.assertEquals(types.length, commands.size());

		for (int i = 0; i < types.length; i++)
			checkCommandType(commands.get(i), types[i]);

		slave.interpretCommandList(commands);
	}

	private static void checkCommandType(Command command, Type type) {
		Assert.assertEquals(type, command.type());
	}

	@Test
	public void testCreate() {
		final IStructureObserver<TestContainer, TestElement> masterMock = createObserverMock();
		final TestMaster master = new TestMaster(masterMock);

		final IStructureObserver<TestContainer, TestElement> slaveMock = createObserverMock();
		final TestSlave slave = new TestSlave(slaveMock);

		final IntTestContainer intContainer = createIntContainer(master, 5);
		final StringTestContainer stringContainer = createStringContainer(master, "world");

		{
			InOrder inOrder = inOrder(masterMock);

			inOrder.verify(masterMock).onElementAdded(0, intContainer, 0, intContainer.element);
			inOrder.verify(masterMock).onContainerAdded(0, intContainer);
			inOrder.verify(masterMock).onStructureUpdate();

			inOrder.verify(masterMock).onElementAdded(1, stringContainer, 1, stringContainer.element);
			inOrder.verify(masterMock).onContainerAdded(1, stringContainer);
			inOrder.verify(masterMock).onStructureUpdate();

			inOrder.verifyNoMoreInteractions();
		}

		Assert.assertTrue(slave.isEmpty());

		performUpdate(master, slave, Command.Type.CREATE, Command.Type.CONSISTENCY_CHECK);

		{
			InOrder inOrder = inOrder(slaveMock);
			inOrder.verify(slaveMock).onUpdateStarted();
			inOrder.verify(slaveMock).onElementAdded(eq(0), any(IntTestContainer.class), eq(0), any(IntTestElement.class));
			inOrder.verify(slaveMock).onContainerAdded(eq(0), any(IntTestContainer.class));

			inOrder.verify(slaveMock).onElementAdded(eq(1), any(StringTestContainer.class), eq(1), any(StringTestElement.class));
			inOrder.verify(slaveMock).onContainerAdded(eq(1), any(StringTestContainer.class));

			inOrder.verify(slaveMock).onStructureUpdate();

			inOrder.verify(slaveMock).onContainerUpdated(eq(0), any(IntTestContainer.class));
			inOrder.verify(slaveMock).onElementUpdated(eq(0), any(IntTestContainer.class), eq(0), any(IntTestElement.class));

			inOrder.verify(slaveMock).onContainerUpdated(eq(1), any(StringTestContainer.class));
			inOrder.verify(slaveMock).onElementUpdated(eq(1), any(StringTestContainer.class), eq(1), any(StringTestElement.class));

			inOrder.verify(slaveMock).onDataUpdate();

			inOrder.verify(slaveMock).onUpdateFinished();

			inOrder.verifyNoMoreInteractions();
		}

		Assert.assertFalse(slave.isEmpty());

		{
			final Map<Integer, TestContainer> containers = slave.getContainers();
			Assert.assertEquals(2, containers.size());
			checkIntContainer(containers, 0, 5);
			checkStringContainer(containers, 1, "world");
		}

		{
			final Map<Integer, TestElement> elements = slave.getElements();
			Assert.assertEquals(2, elements.size());
			checkIntElement(elements, 0, 5);
			checkStringElement(elements, 1, "world");
		}
	}

	@Test
	public void testUpdate() {
		final IStructureObserver<TestContainer, TestElement> masterMock = createObserverMock();
		final TestMaster master = new TestMaster(masterMock);

		final IStructureObserver<TestContainer, TestElement> slaveMock = createObserverMock();
		final TestSlave slave = new TestSlave(slaveMock);

		final IntTestContainer intContainer = createIntContainer(master, 5);
		createStringContainer(master, "world");

		performUpdate(master, slave, Command.Type.CREATE, Command.Type.CONSISTENCY_CHECK);

		reset((Object)masterMock, (Object)slaveMock);

		intContainer.element.value = 42;
		master.markElementModified(0);

		{
			InOrder inOrder = inOrder(masterMock);
			inOrder.verify(masterMock).onContainerUpdated(0, intContainer);
			inOrder.verify(masterMock).onElementUpdated(0, intContainer, 0, intContainer.element);
			inOrder.verify(masterMock).onDataUpdate();
			inOrder.verifyNoMoreInteractions();
		}

		performUpdate(master, slave, Command.Type.UPDATE_SINGLE);

		{
			InOrder inOrder = inOrder(slaveMock);
			inOrder.verify(slaveMock).onUpdateStarted();
			inOrder.verify(slaveMock).onContainerUpdated(eq(0), any(IntTestContainer.class));
			inOrder.verify(slaveMock).onElementUpdated(eq(0), any(IntTestContainer.class), eq(0), any(IntTestElement.class));
			inOrder.verify(slaveMock).onDataUpdate();
			inOrder.verify(slaveMock).onUpdateFinished();
			inOrder.verifyNoMoreInteractions();
		}
		{
			final Map<Integer, TestContainer> containers = slave.getContainers();
			Assert.assertEquals(2, containers.size());
			checkIntContainer(containers, 0, 42);
			checkStringContainer(containers, 1, "world");
		}

		{
			final Map<Integer, TestElement> elements = slave.getElements();
			Assert.assertEquals(2, elements.size());
			checkIntElement(elements, 0, 42);
			checkStringElement(elements, 1, "world");
		}
	}

	@Test
	public void testRemove() {
		final IStructureObserver<TestContainer, TestElement> masterMock = createObserverMock();
		final TestMaster master = new TestMaster(masterMock);

		final IStructureObserver<TestContainer, TestElement> slaveMock = createObserverMock();
		final TestSlave slave = new TestSlave(slaveMock);

		final IntTestContainer intContainer = createIntContainer(master, 5);
		createStringContainer(master, "world");

		performUpdate(master, slave, Command.Type.CREATE, Command.Type.CONSISTENCY_CHECK);

		intContainer.element.value = 42;
		master.markElementModified(0);

		performUpdate(master, slave, Command.Type.UPDATE_SINGLE);

		reset((Object)masterMock, (Object)slaveMock);

		master.removeContainer(0);

		{
			InOrder inOrder = inOrder(masterMock);
			inOrder.verify(masterMock).onContainerRemoved(0, intContainer);
			inOrder.verify(masterMock).onElementRemoved(0, intContainer, 0, intContainer.element);
			inOrder.verify(masterMock).onStructureUpdate();

			inOrder.verifyNoMoreInteractions();
		}

		performUpdate(master, slave, Command.Type.DELETE, Command.Type.CONSISTENCY_CHECK);

		{
			InOrder inOrder = inOrder(slaveMock);
			inOrder.verify(slaveMock).onUpdateStarted();
			inOrder.verify(slaveMock).onContainerRemoved(eq(0), any(IntTestContainer.class));
			inOrder.verify(slaveMock).onElementRemoved(eq(0), any(IntTestContainer.class), eq(0), any(IntTestElement.class));
			inOrder.verify(slaveMock).onStructureUpdate();
			inOrder.verify(slaveMock).onUpdateFinished();

			inOrder.verifyNoMoreInteractions();
		}

		{
			final Map<Integer, TestContainer> containers = slave.getContainers();
			Assert.assertEquals(1, containers.size());
			checkStringContainer(containers, 1, "world");
		}

		{
			final Map<Integer, TestElement> elements = slave.getElements();
			Assert.assertEquals(1, elements.size());
			checkStringElement(elements, 1, "world");
		}
	}

	@Test
	public void testRemoveAllAndSlaveReset() {
		final IStructureObserver<TestContainer, TestElement> masterMock = createObserverMock();
		final TestMaster master = new TestMaster(masterMock);

		final IStructureObserver<TestContainer, TestElement> slaveMock = createObserverMock();
		final TestSlave slave = new TestSlave(slaveMock);

		final IntTestContainer intContainer = createIntContainer(master, 5);
		final StringTestContainer stringContainer = createStringContainer(master, "world");

		performUpdate(master, slave, Command.Type.CREATE, Command.Type.CONSISTENCY_CHECK);

		reset((Object)masterMock, (Object)slaveMock);

		master.removeAll();

		{
			InOrder inOrder = inOrder(masterMock);
			inOrder.verify(masterMock).onContainerRemoved(0, intContainer);
			inOrder.verify(masterMock).onElementRemoved(0, intContainer, 0, intContainer.element);

			inOrder.verify(masterMock).onContainerRemoved(1, stringContainer);
			inOrder.verify(masterMock).onElementRemoved(1, stringContainer, 1, stringContainer.element);
			inOrder.verify(masterMock).onStructureUpdate();

			inOrder.verifyNoMoreInteractions();
		}

		performUpdate(master, slave, Command.Type.RESET);

		{
			InOrder inOrder = inOrder(slaveMock);
			inOrder.verify(slaveMock).onUpdateStarted();
			inOrder.verify(slaveMock).onContainerRemoved(eq(0), any(IntTestContainer.class));
			inOrder.verify(slaveMock).onElementRemoved(eq(0), any(IntTestContainer.class), eq(0), any(IntTestElement.class));

			inOrder.verify(slaveMock).onContainerRemoved(eq(1), any(IntTestContainer.class));
			inOrder.verify(slaveMock).onElementRemoved(eq(1), any(StringTestContainer.class), eq(1), any(StringTestElement.class));

			inOrder.verify(slaveMock).onStructureUpdate();
			inOrder.verify(slaveMock).onUpdateFinished();

			inOrder.verifyNoMoreInteractions();
		}

		Assert.assertTrue(slave.getContainers().isEmpty());
		Assert.assertTrue(slave.getElements().isEmpty());
	}

	@Test
	public void testCreateAfterRemove() {
		final IStructureObserver<TestContainer, TestElement> masterMock = createObserverMock();
		final TestMaster master = new TestMaster(masterMock);

		final IStructureObserver<TestContainer, TestElement> slaveMock = createObserverMock();
		final TestSlave slave = new TestSlave(slaveMock);

		final IntTestContainer intContainer = createIntContainer(master, 5);

		performUpdate(master, slave, Command.Type.CREATE, Command.Type.CONSISTENCY_CHECK);

		reset((Object)masterMock, (Object)slaveMock);

		master.removeAll();

		final StringTestContainer stringContainer = createStringContainer(master, "world");

		{
			InOrder inOrder = inOrder(masterMock);

			inOrder.verify(masterMock).onContainerRemoved(0, intContainer);
			inOrder.verify(masterMock).onElementRemoved(0, intContainer, 0, intContainer.element);
			inOrder.verify(masterMock).onStructureUpdate();

			inOrder.verify(masterMock).onElementAdded(0, stringContainer, 0, stringContainer.element);
			inOrder.verify(masterMock).onContainerAdded(0, stringContainer);
			inOrder.verify(masterMock).onStructureUpdate();

			inOrder.verifyNoMoreInteractions();
		}

		performUpdate(master, slave, Command.Type.RESET, Command.Type.CREATE, Command.Type.CONSISTENCY_CHECK);

		{
			InOrder inOrder = inOrder(slaveMock);
			inOrder.verify(slaveMock).onUpdateStarted();
			inOrder.verify(slaveMock).onContainerRemoved(eq(0), any(IntTestContainer.class));
			inOrder.verify(slaveMock).onElementRemoved(eq(0), any(IntTestContainer.class), eq(0), any(IntTestElement.class));

			inOrder.verify(slaveMock).onElementAdded(eq(0), any(StringTestContainer.class), eq(0), any(StringTestElement.class));
			inOrder.verify(slaveMock).onContainerAdded(eq(0), any(StringTestContainer.class));

			inOrder.verify(slaveMock).onStructureUpdate();

			inOrder.verify(slaveMock).onContainerUpdated(eq(0), any(StringTestContainer.class));
			inOrder.verify(slaveMock).onElementUpdated(eq(0), any(StringTestContainer.class), eq(0), any(StringTestElement.class));

			inOrder.verify(slaveMock).onDataUpdate();

			inOrder.verify(slaveMock).onUpdateFinished();

			inOrder.verifyNoMoreInteractions();
		}

		{
			final Map<Integer, TestContainer> containers = slave.getContainers();
			Assert.assertEquals(1, containers.size());
			checkStringContainer(containers, 0, "world");
		}

		{
			final Map<Integer, TestElement> elements = slave.getElements();
			Assert.assertEquals(1, elements.size());
			checkStringElement(elements, 0, "world");
		}
	}

	@SuppressWarnings("unchecked")
	protected IStructureObserver<TestContainer, TestElement> createObserverMock() {
		return mock(IStructureObserver.class);
	}

	@Test
	public void testUpdateAfterCreate() {
		final TestMaster master = new TestMaster();
		final TestSlave slave = new TestSlave();

		final IntTestContainer intContainer = createIntContainer(master, 4);

		intContainer.element.value = 9;
		master.markElementModified(0);

		performUpdate(master, slave, Command.Type.CREATE, Command.Type.CONSISTENCY_CHECK);

		checkIntContainer(slave.getContainers(), 0, 9);
	}

	@Test
	public void testDeleteAfterCreate() {
		final TestMaster master = new TestMaster();
		final TestSlave slave = new TestSlave();

		createIntContainer(master, 6);

		master.markElementModified(0);
		master.removeContainer(0);

		performUpdate(master, slave, Command.Type.CONSISTENCY_CHECK);
	}

	@Test
	public void testDeleteAfterCreateAndUpdate() {
		final TestMaster master = new TestMaster();
		final TestSlave slave = new TestSlave();

		createIntContainer(master, 7);

		master.removeContainer(0);

		performUpdate(master, slave, Command.Type.CONSISTENCY_CHECK);
	}

	@Test
	public void testCreateOnlyConsistency() {
		final TestMaster master = new TestMaster();
		final TestSlave slave = new TestSlave();

		int containerCount = 0;

		createIntContainer(master, containerCount + 5);

		performUpdate(master, slave, Command.Type.CREATE, Command.Type.CONSISTENCY_CHECK);
		checkIntContainer(slave.getContainers(), 0, containerCount + 5);
		checkIntElement(slave.getElements(), 0, containerCount + 5);
		containerCount++;

		while (containerCount < StructuredDataMaster.CONSISTENCY_CHECK_PERIOD + 2) {
			createIntContainer(master, containerCount + 5);

			performUpdate(master, slave, Command.Type.CREATE, Command.Type.CONSISTENCY_CHECK);
			containerCount++;

			{
				final Map<Integer, TestContainer> containers = slave.getContainers();
				final Map<Integer, TestElement> elements = slave.getElements();

				for (int j = 0; j < containerCount; j++) {
					checkIntContainer(containers, j, j + 5);
					checkIntElement(elements, j, j + 5);
				}
			}

		}
	}

	@Test
	public void testCreateAndDeleteConsistency() {
		final TestMaster master = new TestMaster();
		final TestSlave slave = new TestSlave();

		int containerCount = 0;

		createIntContainer(master, containerCount + 5);

		performUpdate(master, slave, Command.Type.CREATE, Command.Type.CONSISTENCY_CHECK);
		checkIntContainer(slave.getContainers(), 0, containerCount + 5);
		checkIntElement(slave.getElements(), 0, containerCount + 5);
		containerCount++;

		while (containerCount < StructuredDataMaster.CONSISTENCY_CHECK_PERIOD + 2) {
			createIntContainer(master, containerCount + 5);

			master.removeContainer(containerCount - 1);

			performUpdate(master, slave, Command.Type.DELETE, Command.Type.CREATE, Command.Type.CONSISTENCY_CHECK);

			{
				final Map<Integer, TestContainer> containers = slave.getContainers();
				Assert.assertEquals(1, containers.size());

				final Map<Integer, TestElement> elements = slave.getElements();
				Assert.assertEquals(1, elements.size());

				checkIntContainer(containers, containerCount, containerCount + 5);
				checkIntElement(elements, containerCount, containerCount + 5);
			}

			containerCount++;

		}
	}

	@Test
	public void testCustomCreateData() {
		final TestMaster master = new TestMaster();
		final TestSlave slave = new TestSlave();

		final int c1IntValue = 53;
		final String c1StringValue = "hello";
		final long c1CustomValue = 4342;

		{
			final CustomDataTestContainer container = new CustomDataTestContainer();
			container.intElement.value = c1IntValue;
			container.stringElement.value = c1StringValue;
			container.customValue = c1CustomValue;
			master.addContainer(container);
		}

		final int c2IntValue = 523;
		final String c2StringValue = "world";
		final long c2CustomValue = -432;
		{
			final CustomDataTestContainer container = new CustomDataTestContainer();
			container.intElement.value = c2IntValue;
			container.stringElement.value = c2StringValue;
			container.customValue = c2CustomValue;
			master.addContainer(container);
		}

		performUpdate(master, slave, Command.Type.CREATE, Command.Type.CONSISTENCY_CHECK);

		{
			final Map<Integer, TestContainer> containers = slave.getContainers();
			{
				final TestContainer container = containers.get(0);
				Assert.assertTrue(container instanceof CustomDataTestContainer);
				final CustomDataTestContainer customContainer = (CustomDataTestContainer)container;
				Assert.assertEquals(c1IntValue, customContainer.intElement.value);
				Assert.assertEquals(c1StringValue, customContainer.stringElement.value);
				Assert.assertEquals(c1CustomValue, customContainer.customValue);
			}

			{
				final TestContainer container = containers.get(1);
				Assert.assertTrue(container instanceof CustomDataTestContainer);
				final CustomDataTestContainer customContainer = (CustomDataTestContainer)container;
				Assert.assertEquals(c2IntValue, customContainer.intElement.value);
				Assert.assertEquals(c2StringValue, customContainer.stringElement.value);
				Assert.assertEquals(c2CustomValue, customContainer.customValue);
			}
		}

		{
			final Map<Integer, TestElement> elements = slave.getElements();
			checkStringElement(elements, 0, c1StringValue);
			checkIntElement(elements, 1, c1IntValue);
			checkStringElement(elements, 2, c2StringValue);
			checkIntElement(elements, 3, c2IntValue);
		}
	}
}
