package openmods.gui.component;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.text.StringTextComponent;
import openmods.api.IValueReceiver;
import openmods.gui.IComponentParent;
import openmods.gui.listener.IValueChangedListener;

public class GuiComponentTextbox extends BaseComponent implements IValueReceiver<String> {

	private final int width;
	private final int height;

	private TextFieldWidget textfield;

	private IValueChangedListener<String> listener;

	public GuiComponentTextbox(int x, int y, int width, int height) {
		super(x, y);
		this.width = width;
		this.height = height;

	}

	@Override
	public void init(IComponentParent parent) {
		super.init(parent);
		textfield = new TextFieldWidget( parent.getFontRenderer(), x, y, width, height, StringTextComponent.EMPTY);
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void render(MatrixStack matrixStack, int offsetX, int offsetY, int mouseX, int mouseY) {
		textfield.renderButton(matrixStack, mouseX, mouseY, 0);
	}

	// TODO 1.14 keyPressed != charTyped

	@Override
	public void keyTyped(char par1, int par2) {
		if (textfield.charTyped(par1, par2)) notifyListeners();
	}

	@Override
	public void mouseDown(int mouseX, int mouseY, int button) {
		super.mouseDown(mouseX, mouseY, button);
		textfield.mouseClicked(mouseX + x, mouseY + y, button);
	}

	public String getText() {
		return textfield.getText();
	}

	public GuiComponentTextbox setText(String text) {
		textfield.setText(text);
		return this;
	}

	@Override
	public void setValue(String value) {
		textfield.setText(value);
	}

	private void notifyListeners() {
		if (listener != null) listener.valueChanged(textfield.getText());
	}

	public void setListener(IValueChangedListener<String> listener) {
		this.listener = listener;
	}
}
