package openmods.network;

public enum PacketDirection {
	TO_CLIENT(false, true),
	FROM_CLIENT(true, false),
	ANY(true, true);

	public final boolean toServer;
	public final boolean toClient;

	private PacketDirection(boolean toServer, boolean toClient) {
		this.toServer = toServer;
		this.toClient = toClient;
	}

	public boolean validateSend(boolean isRemote) {
		return (isRemote && toServer) || (!isRemote && toClient);
	}

	public boolean validateReceive(boolean isRemote) {
		return (isRemote && toClient) || (!isRemote && toServer);
	}
}