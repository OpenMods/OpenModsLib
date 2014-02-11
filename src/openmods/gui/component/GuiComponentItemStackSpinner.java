package openmods.gui.component;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.IItemRenderer;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.IItemRenderer.ItemRenderType;
import openmods.block.OpenBlock;
import openmods.tileentity.OpenTileEntity;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

public class GuiComponentItemStackSpinner extends BaseComponent {

	private OpenTileEntity tile;
	private OpenBlock block;
	private ItemStack stack;
	private Item item;
	private float rotationX = 0.1f;
	private float rotationY = 0f;
	private int meta = 0;
	private static ItemRenderer itemRenderer;
	private static RenderBlocks blockRenderer = new RenderBlocks();
	private static final ResourceLocation RES_ITEM_GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");

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
	public void renderOverlay(Minecraft minecraft, int offsetX, int offsetY, int mouseX, int mouseY) {
		super.renderOverlay(minecraft, offsetX, offsetY, mouseX, mouseY);
		GL11.glPushMatrix();
		GL11.glEnable(GL11.GL_DEPTH_TEST);
		float scale = 30.0f;
		Tessellator t = Tessellator.instance;
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
		TextureManager texturemanager = mc.getTextureManager();

		Block block = null;
		if (itemStack.getItem() instanceof ItemBlock && itemStack.itemID < Block.blocksList.length)
		{
			block = Block.blocksList[itemStack.itemID];
		}

		IItemRenderer customRenderer = MinecraftForgeClient.getItemRenderer(itemStack, ItemRenderType.ENTITY);
		if (customRenderer != null)
		{
			texturemanager.bindTexture(texturemanager.getResourceLocation(itemStack.getItemSpriteNumber()));
			ForgeHooksClient.renderEquippedItem(ItemRenderType.ENTITY, customRenderer, blockRenderer, player, itemStack);
		}
		else if (block != null && itemStack.getItemSpriteNumber() == 0 && RenderBlocks.renderItemIn3d(Block.blocksList[itemStack.itemID].getRenderType()))
		{
			texturemanager.bindTexture(texturemanager.getResourceLocation(0));
			blockRenderer.renderBlockAsItem(Block.blocksList[itemStack.itemID], itemStack.getItemDamage(), 1.0F);
		}
		else
		{
			Icon icon = player.getItemIcon(itemStack, 0);
			if (icon == null)
			{
				GL11.glPopMatrix();
				return;
			}
			texturemanager.bindTexture(texturemanager.getResourceLocation(itemStack.getItemSpriteNumber()));
			Tessellator tessellator = Tessellator.instance;
			GL11.glEnable(GL12.GL_RESCALE_NORMAL);
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
			
			GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		}
		GL11.glPopMatrix();
	}
}
