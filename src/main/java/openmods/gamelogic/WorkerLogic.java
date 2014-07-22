package openmods.gamelogic;

import openmods.sync.SyncableInt;

public class WorkerLogic {
	private final SyncableInt progress;
	private final int maxProgress;
	private boolean isWorking;

	public WorkerLogic(SyncableInt progress, int maxProgress) {
		this.progress = progress;
		this.maxProgress = maxProgress;
	}

	public void start() {
		isWorking = true;
	}

	public void pause() {
		isWorking = false;
	}

	public void reset() {
		isWorking = false;
		progress.set(0);
	}

	public void checkWorkCondition(boolean canWork) {
		if (isWorking && !canWork) {
			reset();
		} else if (!isWorking && canWork) {
			start();
		}
	}

	public boolean update() {
		if (isWorking) {
			if (progress.get() >= maxProgress) {
				reset();
				return true;
			}

			progress.modify(+1);
		}

		return false;
	}

	public boolean isWorking() {
		return isWorking;
	}
}
