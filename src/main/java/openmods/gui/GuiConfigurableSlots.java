package openmods.gui;

import java.util.Set;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import openmods.api.IValueProvider;
import openmods.api.IValueReceiver;
import openmods.container.ContainerBase;
import openmods.gui.component.*;
import openmods.gui.component.GuiComponentSideSelector.ISideSelectedListener;
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

	protected abstract void addCustomizations(IComponentParent parent, BaseComposite root);

	protected abstract GuiComponentTab createTab(IComponentParent parent, E slot);

	protected GuiComponentSideSelector createSideSelector(IComponentParent parent, E slot, IBlockState state, T te) {
		return new GuiComponentSideSelector(parent, 15, 15, 40.0, state, te, true);
	}

	protected GuiComponentCheckbox createCheckbox(IComponentParent parent, E slot) {
		return new GuiComponentCheckbox(parent, 10, 82, false, 0xFFFFFF);
	}

	protected abstract GuiComponentLabel createLabel(IComponentParent parent, E slot);

	@Override
	protected final BaseComposite createRoot(IComponentParent parent) {
		T te = getContainer().getOwner();

		final IBlockState state = te.getWorld().getBlockState(te.getPos());

		BaseComposite main = super.createRoot(parent);
		addCustomizations(parent, main);

		GuiComponentTabWrapper tabs = new GuiComponentTabWrapper(parent, 0, 0, main);

		for (E slot : getSlots()) {
			GuiComponentTab tabTool = createTab(parent, slot);
			tabs.addComponent(tabTool);

			GuiComponentSideSelector sideSelector = createSideSelector(parent, slot, state, te);
			GuiComponentCheckbox checkbox = createCheckbox(parent, slot);

			setupCheckBox(checkbox, te.createAutoFlagProvider(slot), te.createAutoSlotReceiver(slot));
			setupSelector(sideSelector, te.createAllowedDirectionsProvider(slot), te.createAllowedDirectionsReceiver(slot));

			tabTool.addComponent(sideSelector);
			tabTool.addComponent(checkbox);
			tabTool.addComponent(createLabel(parent, slot));
		}

		return tabs;
	}

	private void setupSelector(GuiComponentSideSelector selector, IValueProvider<Set<EnumFacing>> source, final IWriteableBitMap<EnumFacing> updater) {
		selector.setListener(new ISideSelectedListener() {
			@Override
			public void onSideToggled(EnumFacing side, boolean currentState) {
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
