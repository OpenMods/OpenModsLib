package openmods.structured;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.*;

import openmods.utils.ByteUtils;
import openmods.utils.CollectionUtils;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public abstract class Command {

	public static final Comparator<Command> COMPARATOR = new Comparator<Command>() {

		@Override
		public int compare(Command o1, Command o2) {
			return o1.type().compareTo(o2.type());
		}
	};

	public static class CommandList extends ArrayList<Command> {
		private static final long serialVersionUID = 8317603452787461684L;

		public void readFromStream(DataInput input) throws IOException {
			while (true) {
				Command command = Command.createFromStream(input);
				if (command.isEnd()) return;
				add(command);
			}
		}

		public void writeToStream(DataOutput output) throws IOException {
			Collections.sort(this, Command.COMPARATOR);

			for (Command c : this) {
				c.writeToStream(output);
				if (c.isEnd()) return;
			}

			END_INST.writeToStream(output);
		}

	}

	public enum Type { // DO NOT REORDER
		RESET {
			@Override
			public Reset create() {
				return RESET_INST;
			}
		},
		DELETE {
			@Override
			public Delete create() {
				return new Delete();
			}
		},
		CREATE {
			@Override
			public Create create() {
				return new Create();
			}
		},
		UPDATE_SINGLE {
			@Override
			public UpdateSingle create() {
				return new UpdateSingle();
			}
		},
		UPDATE_BULK {
			@Override
			public UpdateBulk create() {
				return new UpdateBulk();
			}
		},
		CONSISTENCY_CHECK {
			@Override
			public ConsistencyCheck create() {
				return new ConsistencyCheck();
			}
		},
		END { // must be last!
			@Override
			public EmptyCommand create() {
				return END_INST;
			}
		};

		public abstract Command create();

		public static final Type[] TYPES = values();
	}

	public static class ConsistencyCheck extends Command {
		public int elementCount;
		public int minElementId;
		public int maxElementId;

		public int containerCount;
		public int minContainerId;
		public int maxContainerId;

		@Override
		public Type type() {
			return Type.CONSISTENCY_CHECK;
		}

		@Override
		protected void readDataFromStream(DataInput input) {
			elementCount = ByteUtils.readVLI(input);
			minElementId = ByteUtils.readVLI(input);
			maxElementId = ByteUtils.readVLI(input);
			containerCount = ByteUtils.readVLI(input);
			minContainerId = ByteUtils.readVLI(input);
			maxContainerId = ByteUtils.readVLI(input);
		}

		@Override
		protected void writeDataToStream(DataOutput output) {
			ByteUtils.writeVLI(output, elementCount);
			ByteUtils.writeVLI(output, minElementId);
			ByteUtils.writeVLI(output, maxElementId);
			ByteUtils.writeVLI(output, containerCount);
			ByteUtils.writeVLI(output, minContainerId);
			ByteUtils.writeVLI(output, maxContainerId);
		}

		@Override
		public String dumpContents() {
			return "[elementCount=" + elementCount + ", minElementId=" + minElementId + ", maxElementId=" + maxElementId + ", containerCount=" + containerCount + ", minContainerId=" + minContainerId + ", maxContainerId=" + maxContainerId + "]";
		}

	}

	public abstract static class EmptyCommand extends Command {
		@Override
		protected void readDataFromStream(DataInput input) {}

		@Override
		protected void writeDataToStream(DataOutput output) {}
	}

	public static final class Reset extends EmptyCommand {
		@Override
		public Type type() {
			return Type.RESET;
		}
	}

	static final Reset RESET_INST = new Reset();

	private static final EmptyCommand END_INST = new EmptyCommand() {
		@Override
		public Type type() {
			return Type.END;
		}

		@Override
		public boolean isEnd() {
			return true;
		}
	};

	public static class ContainerInfo {
		public final int id;
		public final int type;
		public final int start;

		public ContainerInfo(int id, int type, int start) {
			this.id = id;
			this.type = type;
			this.start = start;
		}

		@Override
		public String toString() {
			return "[id=" + id + ", type=" + type + ", start=" + start + "]";
		}
	}

	public static class Delete extends Command {
		public final SortedSet<Integer> idList = Sets.newTreeSet();

		@Override
		public Type type() {
			return Type.DELETE;
		}

		@Override
		protected void readDataFromStream(DataInput input) {
			CollectionUtils.readSortedIdList(input, idList);
		}

		@Override
		protected void writeDataToStream(DataOutput output) {
			CollectionUtils.writeSortedIdList(output, idList);
		}

		@Override
		public String dumpContents() {
			return String.valueOf(idList);
		}
	}

	public static class Create extends Command {
		public final List<ContainerInfo> containers = Lists.newArrayList();

		byte[] containerPayload;
		byte[] elementPayload;

		@Override
		public Type type() {
			return Type.CREATE;
		}

		@Override
		protected void readDataFromStream(DataInput input) throws IOException {
			int elemCount = ByteUtils.readVLI(input);

			int currentContainerId = 0;
			int currentElementId = 0;

			for (int i = 0; i < elemCount; i++) {
				currentContainerId += ByteUtils.readVLI(input);
				int type = ByteUtils.readVLI(input);
				currentElementId += ByteUtils.readVLI(input);

				containers.add(new ContainerInfo(currentContainerId, type, currentElementId));
			}

			containerPayload = readChunk(input);
			elementPayload = readChunk(input);
		}

		@Override
		protected void writeDataToStream(DataOutput output) throws IOException {
			ByteUtils.writeVLI(output, containers.size());

			int prevContainerId = 0;
			int prevElementId = 0;
			for (ContainerInfo info : containers) {
				int deltaContainerId = info.id - prevContainerId;
				Preconditions.checkArgument(deltaContainerId >= 0, "Container ids must be sorted in ascending order");

				int deltaElementId = info.start - prevElementId;
				Preconditions.checkArgument(deltaElementId >= 0, "Element ids must be sorted in ascending order");

				ByteUtils.writeVLI(output, deltaContainerId);
				ByteUtils.writeVLI(output, info.type);
				ByteUtils.writeVLI(output, deltaElementId);

				prevContainerId = info.id;
				prevElementId = info.start;
			}

			writeChunk(output, containerPayload);
			writeChunk(output, elementPayload);
		}

		@Override
		public String dumpContents() {
			return String.format("%s -> %s", containers,
					(elementPayload == null? "<null>" : Integer.toString(elementPayload.length)));
		}
	}

	public abstract static class Update extends Command {
		public final SortedSet<Integer> idList = Sets.newTreeSet();
		byte[] elementPayload;

		@Override
		protected void readDataFromStream(DataInput input) throws IOException {
			elementPayload = readChunk(input);
		}

		@Override
		protected void writeDataToStream(DataOutput output) throws IOException {
			writeChunk(output, elementPayload);
		}
	}

	public static class UpdateSingle extends Update {

		@Override
		public Type type() {
			return Type.UPDATE_SINGLE;
		}

		@Override
		protected void readDataFromStream(DataInput input) throws IOException {
			CollectionUtils.readSortedIdList(input, idList);
			super.readDataFromStream(input);
		}

		@Override
		protected void writeDataToStream(DataOutput output) throws IOException {
			CollectionUtils.writeSortedIdList(output, idList);
			super.writeDataToStream(output);
		}

		@Override
		public String dumpContents() {
			return String.format("%s -> %s", idList,
					(elementPayload == null? "<null>" : Integer.toString(elementPayload.length)));
		}
	}

	// TODO Implement
	public static class UpdateBulk extends Update {

		@Override
		public Type type() {
			return Type.UPDATE_BULK;
		}

		@Override
		protected void readDataFromStream(DataInput input) throws IOException {

			super.readDataFromStream(input);
		}

		@Override
		protected void writeDataToStream(DataOutput output) throws IOException {

			super.writeDataToStream(output);
		}
	}

	public abstract Type type();

	protected abstract void readDataFromStream(DataInput input) throws IOException;

	protected abstract void writeDataToStream(DataOutput output) throws IOException;

	public static Command createFromStream(DataInput input) throws IOException {
		int id = ByteUtils.readVLI(input);
		Type type = Type.TYPES[id];
		Command command = type.create();
		command.readDataFromStream(input);
		return command;
	}

	public void writeToStream(DataOutput output) throws IOException {
		ByteUtils.writeVLI(output, type().ordinal());
		writeDataToStream(output);
	}

	protected static byte[] readChunk(DataInput input) throws IOException {
		int size = ByteUtils.readVLI(input);
		byte[] chunk = new byte[size];
		input.readFully(chunk);
		return chunk;
	}

	protected static void writeChunk(DataOutput output, byte[] chunk) throws IOException {
		ByteUtils.writeVLI(output, chunk.length);
		output.write(chunk);
	}

	protected boolean isEnd() {
		return false;
	}

	public String dumpContents() {
		return "";
	}

	@Override
	public String toString() {
		return type() + ": " + dumpContents();
	}
}
