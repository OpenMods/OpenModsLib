package openmods.gui;

import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.ForgeDirection;
import openmods.api.IValueProvider;
import openmods.api.IValueReceiver;
import openmods.container.ContainerBase;
import openmods.gui.component.BaseComposite;
import openmods.gui.component.GuiComponentCheckbox;
import openmods.gui.component.GuiComponentLabel;
import openmods.gui.component.GuiComponentSideSelector;
import openmods.gui.component.GuiComponentSideSelector.ISideSelectedListener;
import openmods.gui.component.GuiComponentTab;
import openmods.gui.component.GuiComponentTabWrapper;
import openmods.gui.listener.IValueChangedListener;
import openmods.gui.logic.ValueCopyAction;
import openmods.gui.misc.IConfigurableGuiSlots;
import openmods.sync.ISyncMapProvider;
import openmods.utils.bitmap.IWriteableBitMap;

public abstract class GuiConfigurableSlots<T extends TileEntity & ISyncMapProvider & IConfigurableGuiSlots<E>, C extends ContainerBase<T>, E extends Enum<E>> extends SyncedGuiContainer<C> {

	public GuiConfigurableSlots(C container, int width, int height, String name) {
		super(container, width, height, name);
	}

	protected abstract Iterable<E> getSlots();

	protected abstract void addCustomizations(BaseComposite root);

	protected abstract GuiComponentTab createTab(E slot);

	protected GuiComponentSideSelector createSideSelector(E slot, Block block, int meta, T te) {
		return new GuiComponentSideSelector(15, 15, 40.0, block, meta, te, true);
	}

	protected GuiComponentCheckbox createCheckbox(E slot) {
		return new GuiComponentCheckbox(10, 82, false, 0xFFFFFF);
	}

	protected abstract GuiComponentLabel createLabel(E slot);

	@Override
	protected final BaseComposite createRoot() {
		T te = getContainer().getOwner();

		final int meta = te.getBlockMetadata();
		final Block block = te.getBlockType();

		BaseComposite main = super.createRoot();
		addCustomizations(main);

		GuiComponentTabWrapper tabs = new GuiComponentTabWrapper(0, 0, main);

		for (E slot : getSlots()) {
			GuiComponentTab tabTool = createTab(slot);
			tabs.addComponent(tabTool);

			GuiComponentSideSelector sideSelector = createSideSelector(slot, block, meta, te);
			GuiComponentCheckbox checkbox = createCheckbox(slot);

			setupCheckBox(checkbox, te.createAutoFlagProvider(slot), te.createAutoSlotReceiver(slot));
			setupSelector(sideSelector, te.createAllowedDirectionsProvider(slot), te.createAllowedDirectionsReceiver(slot));

			tabTool.addComponent(sideSelector);
			tabTool.addComponent(checkbox);
			tabTool.addComponent(createLabel(slot));
		}

		return tabs;
	}

	private void setupSelector(GuiComponentSideSelector selector, IValueProvider<Set<ForgeDirection>> source, final IWriteableBitMap<ForgeDirection> updater) {
		selector.setListener(new ISideSelectedListener() {
			@Override
			public void onSideToggled(ForgeDirection side, boolean currentState) {
				updater.set(side, currentState);
			}
		});

		addSyncUpdateListener(ValueCopyAction.create(source, selector));
	}

	private void setupCheckBox(final GuiComponentCheckbox checkbox, IValueProvider<Boolean> source, final IValueReceiver<Boolean> updater) {
		checkbox.setListener(new IValueChangedListener<Boolean>() {
			@Override
			public void valueChanged(Boolean value) {
				updater.setValue(value);
			}
		});

		addSyncUpdateListener(ValueCopyAction.create(source, checkbox));
	}
}
