package openmods.structured;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import openmods.structured.Command.Type;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;

public class StructuredTest {

	public abstract static class TestElement implements IStructureElement {
		public int id;

		@Override
		public int getId() {
			return id;
		}

		@Override
		public void setId(int id) {
			this.id = id;
		}
	}

	public static class IntTestElement extends TestElement {
		public int value;

		@Override
		public void readFromStream(DataInput input) throws IOException {
			this.value = input.readInt();
		}

		@Override
		public void writeToStream(DataOutput output) throws IOException {
			output.writeInt(value);
		}
	}

	public static class StringTestElement extends TestElement {
		public String value;

		@Override
		public void readFromStream(DataInput input) throws IOException {
			this.value = input.readUTF();
		}

		@Override
		public void writeToStream(DataOutput output) throws IOException {
			output.writeUTF(value);
		}
	}

	public abstract static class TestContainer implements IStructureContainer<TestElement> {
		public int id;

		@Override
		public int getId() {
			return id;
		}

		@Override
		public void setId(int id) {
			this.id = id;
		}

		@Override
		public void onElementAdded(TestElement element) {}

		@Override
		public void onUpdate() {}

		@Override
		public void onElementUpdated(TestElement element) {}
	}

	public static class IntTestContainer extends TestContainer {

		public static final int TYPE = 0;

		public final IntTestElement element = new IntTestElement();

		@Override
		public int getType() {
			return TYPE;
		}

		@Override
		public List<TestElement> createElements() {
			return Lists.<TestElement> newArrayList(element);
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
		public List<TestElement> createElements() {
			return Lists.<TestElement> newArrayList(element);
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
		public List<TestElement> createElements() {
			return Lists.<TestElement> newArrayList(stringElement, intElement);
		}

		@Override
		public void readCustomDataFromStream(DataInput input) throws IOException {
			this.customValue = input.readLong();
		}

		@Override
		public void writeCustomDataFromStream(DataOutput output) throws IOException {
			output.writeLong(customValue);
		}
	}

	public static class TestMaster extends StructuredDataMaster<TestContainer, TestElement> {}

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
		protected TestSlave() {
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

		for (int i = 0; i < types.length; i++)
			checkCommandType(commands, types[i], i);

		slave.interpretCommandList(commands);
	}

	private static void checkCommandType(final List<Command> commands, final Type type, final int index) {
		Assert.assertEquals(type, commands.get(index).type());
	}

	@Test
	public void umbrellaTest() {
		final TestMaster master = new TestMaster();
		final TestSlave slave = new TestSlave();

		final IntTestContainer intContainer = new IntTestContainer();
		intContainer.element.value = 5;
		master.addContainer(intContainer);

		final StringTestContainer stringContainer = new StringTestContainer();
		stringContainer.element.value = "world";
		master.addContainer(stringContainer);

		Assert.assertTrue(slave.isEmpty());

		performUpdate(master, slave, Command.Type.CREATE, Command.Type.CONSISTENCY_CHECK);
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

		intContainer.element.value = 42;
		master.markElementModified(intContainer.element);

		performUpdate(master, slave, Command.Type.UPDATE_SINGLE);

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

		master.removeContainer(intContainer);
		performUpdate(master, slave, Command.Type.DELETE);

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
	public void testUpdateAfterCreate() {
		final TestMaster master = new TestMaster();
		final TestSlave slave = new TestSlave();

		final IntTestContainer intContainer = new IntTestContainer();
		intContainer.element.value = 5;
		master.addContainer(intContainer);

		intContainer.element.value = 9;
		master.markElementModified(intContainer.element);

		performUpdate(master, slave, Command.Type.CREATE, Command.Type.CONSISTENCY_CHECK);

		checkIntContainer(slave.getContainers(), 0, 9);
	}

	@Test
	public void testDeleteAfterCreate() {
		final TestMaster master = new TestMaster();
		final TestSlave slave = new TestSlave();

		final IntTestContainer intContainer = new IntTestContainer();
		intContainer.element.value = 5;
		master.addContainer(intContainer);

		master.markElementModified(intContainer.element);
		master.removeContainer(intContainer);

		performUpdate(master, slave, Command.Type.CONSISTENCY_CHECK);
	}

	@Test
	public void testDeleteAfterCreateAndUpdate() {
		final TestMaster master = new TestMaster();
		final TestSlave slave = new TestSlave();

		final IntTestContainer intContainer = new IntTestContainer();
		intContainer.element.value = 5;
		master.addContainer(intContainer);

		master.removeContainer(intContainer);

		performUpdate(master, slave, Command.Type.CONSISTENCY_CHECK);
	}

	@Test
	public void testCreateOnlyConsistency() {
		final TestMaster master = new TestMaster();
		final TestSlave slave = new TestSlave();

		int containerCount = 0;

		{
			final IntTestContainer intContainer = new IntTestContainer();
			intContainer.element.value = containerCount + 5;
			master.addContainer(intContainer);
		}

		performUpdate(master, slave, Command.Type.CREATE, Command.Type.CONSISTENCY_CHECK);
		checkIntContainer(slave.getContainers(), 0, containerCount + 5);
		checkIntElement(slave.getElements(), 0, containerCount + 5);
		containerCount++;

		while (containerCount < StructuredDataMaster.CONSISTENCY_CHECK_PERIOD - 2) {
			{
				final IntTestContainer newContainer = new IntTestContainer();
				newContainer.element.value = containerCount + 5;
				master.addContainer(newContainer);
			}

			performUpdate(master, slave, Command.Type.CREATE);
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

		{
			final IntTestContainer lastContainer = new IntTestContainer();
			lastContainer.element.value = containerCount + 5;
			master.addContainer(lastContainer);
		}

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

	@Test
	public void testCreateAndDeleteOnlyConsistency() {
		final TestMaster master = new TestMaster();
		final TestSlave slave = new TestSlave();

		int containerCount = 0;
		int lastElement;

		{
			final IntTestContainer intContainer = new IntTestContainer();
			intContainer.element.value = containerCount + 5;
			master.addContainer(intContainer);
			lastElement = intContainer.id;
		}

		performUpdate(master, slave, Command.Type.CREATE, Command.Type.CONSISTENCY_CHECK);
		checkIntContainer(slave.getContainers(), 0, containerCount + 5);
		checkIntElement(slave.getElements(), 0, containerCount + 5);
		containerCount++;

		while (containerCount < StructuredDataMaster.CONSISTENCY_CHECK_PERIOD - 2) {
			master.removeContainer(lastElement);

			{
				final IntTestContainer newContainer = new IntTestContainer();
				newContainer.element.value = containerCount + 5;
				master.addContainer(newContainer);
				lastElement = newContainer.id;
			}

			performUpdate(master, slave, Command.Type.DELETE, Command.Type.CREATE);
			containerCount++;

			{
				final Map<Integer, TestContainer> containers = slave.getContainers();
				Assert.assertEquals(1, containers.size());

				final Map<Integer, TestElement> elements = slave.getElements();
				Assert.assertEquals(1, elements.size());

				checkIntContainer(containers, lastElement, lastElement + 5);
				checkIntElement(elements, lastElement, lastElement + 5);
			}

		}

		master.removeContainer(lastElement);

		{
			final IntTestContainer lastContainer = new IntTestContainer();
			lastContainer.element.value = containerCount + 5;
			master.addContainer(lastContainer);
			lastElement = lastContainer.id;
		}

		performUpdate(master, slave, Command.Type.DELETE, Command.Type.CREATE, Command.Type.CONSISTENCY_CHECK);
		containerCount++;

		{
			final Map<Integer, TestContainer> containers = slave.getContainers();
			Assert.assertEquals(1, containers.size());

			final Map<Integer, TestElement> elements = slave.getElements();
			Assert.assertEquals(1, elements.size());

			checkIntContainer(containers, lastElement, lastElement + 5);
			checkIntElement(elements, lastElement, lastElement + 5);
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
