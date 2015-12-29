package openmods.network.event;

import net.minecraftforge.fml.relauncher.Side;

public enum EventDirection {
	C2S {
		@Override
		public boolean validateSend(Side side) {
			return side == Side.CLIENT;
		}

		@Override
		public boolean validateReceive(Side side) {
			return side == Side.SERVER;
		}
	},
	S2C {
		@Override
		public boolean validateSend(Side side) {
			return side == Side.SERVER;
		}

		@Override
		public boolean validateReceive(Side side) {
			return side == Side.CLIENT;
		}
	},
	ANY {
		@Override
		public boolean validateSend(Side side) {
			return true;
		}

		@Override
		public boolean validateReceive(Side side) {
			return true;
		}
	};

	public abstract boolean validateSend(Side side);

	public abstract boolean validateReceive(Side side);
}