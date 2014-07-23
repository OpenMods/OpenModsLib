package openmods.gui.component;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.client.*;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import openmods.utils.TextureUtils;

import org.lwjgl.opengl.GL11;

public class GuiComponentItemStackSpinner extends BaseComponent {

	private ItemStack stack;
	private float rotationY = 0f;
	private static ItemRenderer itemRenderer;
	private static RenderBlocks blockRenderer = new RenderBlocks();

	public GuiComponentItemStackSpinner(int x, int y, ItemStack stack) {
		super(x, y);
		if (itemRenderer == null) {
			itemRenderer = new ItemRenderer(Minecraft.getMinecraft());
		}
		this.stack = stack;
	}

	@Override
	public int getWidth() {
		return 64;
	}

	@Override
	public int getHeight() {
		return 64;
	}

	@Override
	public void render(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {}

	@Override
	public void renderOverlay(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		GL11.glPushMatrix();
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		float scale = 30.0f;
		GL11.glTranslated(offsetX + x + (scale / 2), offsetY + y + (scale / 2), scale);
		GL11.glScaled(scale, -scale, scale);
		rotationY += 0.6f;
		GL11.glRotatef(20, 1, 0, 0);
		GL11.glRotatef(rotationY, 0, 1, 0);
		renderItem(stack);
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glPopMatrix();
	}

	public void renderItem(ItemStack itemStack) {
		GL11.glPushMatrix();
		Minecraft mc = Minecraft.getMinecraft();
		EntityLivingBase player = mc.thePlayer;

		Item item = itemStack.getItem();

		Block block = null;
		if (item instanceof ItemBlock) block = Block.getBlockFromItem(item);

		TextureUtils.bindItemStackTexture(itemStack);

		GL11.glDisable(GL11.GL_LIGHTING);
		IItemRenderer customRenderer = MinecraftForgeClient.getItemRenderer(itemStack, ItemRenderType.ENTITY);
		if (customRenderer != null) {
			ForgeHooksClient.renderEquippedItem(ItemRenderType.EQUIPPED, customRenderer, blockRenderer, player, itemStack);
		} else if (block != null && itemStack.getItemSpriteNumber() == 0 && RenderBlocks.renderItemIn3d(block.getRenderType())) {
			blockRenderer.renderBlockAsItem(block, itemStack.getItemDamage(), 1.0F);
		} else {
			renderItem(itemStack, player);
		}
		GL11.glEnable(GL11.GL_LIGHTING);
		GL11.glPopMatrix();
	}

	private static void renderItem(ItemStack itemStack, EntityLivingBase player) {
		IIcon icon = player.getItemIcon(itemStack, 0);
		if (icon != null) {
			Tessellator tessellator = Tessellator.instance;
			GL11.glTranslatef(-0.5f, -0.5f, 0);
			ItemRenderer.renderItemIn2D(
					tessellator,
					icon.getMaxU(),
					icon.getMinV(),
					icon.getMinU(),
					icon.getMaxV(),
					icon.getIconWidth(),
					icon.getIconHeight(),
					0.0625F
					);
		}
	}
}
