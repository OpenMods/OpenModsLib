package openmods.network.event;

import cpw.mods.fml.relauncher.Side;

public abstract class PacketDirectionValidator {
	public static final PacketDirectionValidator C2S = new PacketDirectionValidator() {
		@Override
		public boolean validateSend(Side side) {
			return side == Side.CLIENT;
		}

		@Override
		public boolean validateReceive(Side side) {
			return side == Side.SERVER;
		}
	};

	public static final PacketDirectionValidator S2C = new PacketDirectionValidator() {
		@Override
		public boolean validateSend(Side side) {
			return side == Side.SERVER;
		}

		@Override
		public boolean validateReceive(Side side) {
			return side == Side.CLIENT;
		}
	};

	public static final PacketDirectionValidator ANY = new PacketDirectionValidator() {
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