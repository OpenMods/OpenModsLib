package openmods.gui;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;
import openmods.container.ContainerBase;
import openmods.gui.logic.IValueUpdateAction;
import openmods.gui.logic.SyncObjectUpdateDispatcher;
import openmods.sync.ISyncMapProvider;

public class SyncedGuiContainer<T extends ContainerBase<? extends ISyncMapProvider>> extends BaseGuiContainer<T> {

	private SyncObjectUpdateDispatcher dispatcher;

	public SyncedGuiContainer(T container, PlayerInventory inv, ITextComponent name, int width, int height) {
		super(container, inv, name, width, height);

		if (dispatcher != null) dispatcher.triggerAll();
	}

	protected SyncObjectUpdateDispatcher dispatcher() {
		if (dispatcher == null) {
			dispatcher = new SyncObjectUpdateDispatcher();
			getContainer().getOwner().getSyncMap().addUpdateListener(dispatcher);
		}

		return dispatcher;
	}

	public void addSyncUpdateListener(IValueUpdateAction action) {
		dispatcher().addAction(action);
	}

	@Override
	public void init() {
		super.init();
		if (dispatcher != null) dispatcher.triggerAll();
	}

	@Override
	public void removed() {
		super.removed();

		if (dispatcher != null) getContainer().getOwner().getSyncMap().removeUpdateListener(dispatcher);
	}

}
