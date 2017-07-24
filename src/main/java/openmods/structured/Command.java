package openmods.structured;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import net.minecraft.network.PacketBuffer;
import openmods.utils.CollectionUtils;

public abstract class Command {

	public static final Comparator<Command> COMPARATOR = new Comparator<Command>() {

		@Override
		public int compare(Command o1, Command o2) {
			return o1.type().compareTo(o2.type());
		}
	};

	public static class CommandList extends ArrayList<Command> {
		private static final long serialVersionUID = 8317603452787461684L;

		public void readFromStream(PacketBuffer input) throws IOException {
			while (true) {
				Command command = Command.createFromStream(input);
				if (command.isEnd()) return;
				add(command);
			}
		}

		public void writeToStream(PacketBuffer output) throws IOException {
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
		protected void readDataFromStream(PacketBuffer input) {
			elementCount = input.readVarIntFromBuffer();
			minElementId = input.readVarIntFromBuffer();
			maxElementId = input.readVarIntFromBuffer();
			containerCount = input.readVarIntFromBuffer();
			minContainerId = input.readVarIntFromBuffer();
			maxContainerId = input.readVarIntFromBuffer();
		}

		@Override
		protected void writeDataToStream(PacketBuffer output) {
			output.writeVarIntToBuffer(elementCount);
			output.writeVarIntToBuffer(minElementId);
			output.writeVarIntToBuffer(maxElementId);
			output.writeVarIntToBuffer(containerCount);
			output.writeVarIntToBuffer(minContainerId);
			output.writeVarIntToBuffer(maxContainerId);
		}

		@Override
		public String dumpContents() {
			return "[elementCount=" + elementCount + ", minElementId=" + minElementId + ", maxElementId=" + maxElementId + ", containerCount=" + containerCount + ", minContainerId=" + minContainerId + ", maxContainerId=" + maxContainerId + "]";
		}

	}

	public abstract static class EmptyCommand extends Command {
		@Override
		protected void readDataFromStream(PacketBuffer input) {}

		@Override
		protected void writeDataToStream(PacketBuffer output) {}
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
		protected void readDataFromStream(PacketBuffer input) {
			CollectionUtils.readSortedIdList(input, idList);
		}

		@Override
		protected void writeDataToStream(PacketBuffer output) {
			CollectionUtils.writeSortedIdList(output, idList);
		}

		@Override
		public String dumpContents() {
			return String.valueOf(idList);
		}
	}

	public static class Create extends Command {
		public final List<ContainerInfo> containers = Lists.newArrayList();

		PacketBuffer containerPayload;
		PacketBuffer elementPayload;

		@Override
		public Type type() {
			return Type.CREATE;
		}

		@Override
		protected void readDataFromStream(PacketBuffer input) {
			final int elemCount = input.readVarIntFromBuffer();

			int currentContainerId = 0;
			int currentElementId = 0;

			for (int i = 0; i < elemCount; i++) {
				currentContainerId += input.readVarIntFromBuffer();
				final int type = input.readVarIntFromBuffer();
				currentElementId += input.readVarIntFromBuffer();

				containers.add(new ContainerInfo(currentContainerId, type, currentElementId));
			}

			containerPayload = readChunk(input);
			elementPayload = readChunk(input);
		}

		@Override
		protected void writeDataToStream(PacketBuffer output) {
			output.writeVarIntToBuffer(containers.size());

			int prevContainerId = 0;
			int prevElementId = 0;
			for (ContainerInfo info : containers) {
				int deltaContainerId = info.id - prevContainerId;
				Preconditions.checkArgument(deltaContainerId >= 0, "Container ids must be sorted in ascending order");

				int deltaElementId = info.start - prevElementId;
				Preconditions.checkArgument(deltaElementId >= 0, "Element ids must be sorted in ascending order");

				output.writeVarIntToBuffer(deltaContainerId);
				output.writeVarIntToBuffer(info.type);
				output.writeVarIntToBuffer(deltaElementId);

				prevContainerId = info.id;
				prevElementId = info.start;
			}

			writeChunk(output, containerPayload);
			writeChunk(output, elementPayload);
		}

		@Override
		public String dumpContents() {
			return String.format("%s -> %s", containers,
					(elementPayload == null? "<null>" : Integer.toString(elementPayload.writerIndex())));
		}
	}

	public abstract static class Update extends Command {
		public final SortedSet<Integer> idList = Sets.newTreeSet();
		PacketBuffer elementPayload;

		@Override
		protected void readDataFromStream(PacketBuffer input) {
			elementPayload = readChunk(input);
		}

		@Override
		protected void writeDataToStream(PacketBuffer output) {
			writeChunk(output, elementPayload);
		}
	}

	public static class UpdateSingle extends Update {

		@Override
		public Type type() {
			return Type.UPDATE_SINGLE;
		}

		@Override
		protected void readDataFromStream(PacketBuffer input) {
			CollectionUtils.readSortedIdList(input, idList);
			super.readDataFromStream(input);
		}

		@Override
		protected void writeDataToStream(PacketBuffer output) {
			CollectionUtils.writeSortedIdList(output, idList);
			super.writeDataToStream(output);
		}

		@Override
		public String dumpContents() {
			return String.format("%s -> %s", idList,
					(elementPayload == null? "<null>" : Integer.toString(elementPayload.writerIndex())));
		}
	}

	// TODO Implement
	public static class UpdateBulk extends Update {

		@Override
		public Type type() {
			return Type.UPDATE_BULK;
		}

		@Override
		protected void readDataFromStream(PacketBuffer input) {
			super.readDataFromStream(input);
		}

		@Override
		protected void writeDataToStream(PacketBuffer output) {
			super.writeDataToStream(output);
		}
	}

	public abstract Type type();

	protected abstract void readDataFromStream(PacketBuffer input) throws IOException;

	protected abstract void writeDataToStream(PacketBuffer output) throws IOException;

	public static Command createFromStream(PacketBuffer input) throws IOException {
		Type type = input.readEnumValue(Type.class);
		Command command = type.create();
		command.readDataFromStream(input);
		return command;
	}

	public void writeToStream(PacketBuffer output) throws IOException {
		output.writeEnumValue(type());
		writeDataToStream(output);
	}

	protected static PacketBuffer readChunk(PacketBuffer input) {
		final int size = input.readVarIntFromBuffer();
		return new PacketBuffer(input.readBytes(size));
	}

	protected static void writeChunk(PacketBuffer output, ByteBuf chunk) {
		output.writeVarIntToBuffer(chunk.readableBytes());
		output.writeBytes(chunk);
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
