package openmods.gui.logic;

import java.util.Set;

import net.minecraftforge.common.util.ForgeDirection;
import openmods.gui.component.GuiComponentSideSelector;
import openmods.gui.component.GuiComponentSideSelector.ISideSelectedListener;
import openmods.sync.IValueProvider;
import openmods.utils.bitmap.IRpcDirectionBitMap;

import com.google.common.collect.ImmutableList;

public class SideSelectorUpdater implements IValueUpdateAction, ISideSelectedListener {

	private final IValueProvider<Set<ForgeDirection>> source;

	private final IRpcDirectionBitMap updater;

	private final GuiComponentSideSelector selector;

	public SideSelectorUpdater(IValueProvider<Set<ForgeDirection>> source, IRpcDirectionBitMap updater, GuiComponentSideSelector selector) {
		this.source = source;
		this.updater = updater;
		this.selector = selector;
	}

	@Override
	public Iterable<?> getTriggers() {
		return ImmutableList.of(source);
	}

	@Override
	public void execute() {
		selector.setSides(source.getValue());
	}

	@Override
	public void onSideToggled(ForgeDirection side, boolean currentState) {
		updater.toggle(side);
	}
}
