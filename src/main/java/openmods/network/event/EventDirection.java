package openmods.network.event;

import net.minecraftforge.fml.LogicalSide;

public enum EventDirection {
	C2S {
		@Override
		public boolean validateSend(LogicalSide side) {
			return side == LogicalSide.CLIENT;
		}

		@Override
		public boolean validateReceive(LogicalSide side) {
			return side == LogicalSide.SERVER;
		}
	},
	S2C {
		@Override
		public boolean validateSend(LogicalSide side) {
			return side == LogicalSide.SERVER;
		}

		@Override
		public boolean validateReceive(LogicalSide side) {
			return side == LogicalSide.CLIENT;
		}
	},
	ANY {
		@Override
		public boolean validateSend(LogicalSide side) {
			return true;
		}

		@Override
		public boolean validateReceive(LogicalSide side) {
			return true;
		}
	};

	public abstract boolean validateSend(LogicalSide side);

	public abstract boolean validateReceive(LogicalSide side);
}