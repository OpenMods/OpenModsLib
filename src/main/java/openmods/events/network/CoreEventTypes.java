package openmods.events.network;

import openmods.network.event.*;

public enum CoreEventTypes implements IEventPacketType {
	TILE_ENTITY_NOTIFY {

		@Override
		public EventPacket createPacket() {
			return new TileEntityMessageEventPacket();
		}

		@Override
		public PacketDirectionValidator getDirection() {
			return PacketDirectionValidator.ANY;
		}
	};

	@Override
	public boolean isCompressed() {
		return false;
	}

	@Override
	public boolean isChunked() {
		return false;
	}

	@Override
	public int getId() {
		return ordinal();
	}

	public static void registerAll() {
		EventPacketManager.INSTANCE.registerEvent(TileEntityMessageEventPacket.class);
	}
}
