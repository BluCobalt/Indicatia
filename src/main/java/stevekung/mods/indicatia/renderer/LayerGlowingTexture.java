package stevekung.mods.indicatia.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelHumanoidHead;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class LayerGlowingTexture implements LayerRenderer<EntityLivingBase>
{
    private final ModelRenderer render;
    private final ModelHumanoidHead head = new ModelHumanoidHead();

    public LayerGlowingTexture(ModelRenderer bipedHead)
    {
        this.render = bipedHead;
    }

    @Override
    public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale)
    {
        ItemStack itemstack = entity.getCurrentArmor(3);

        if (itemstack != null && itemstack.getItem() == Items.skull)
        {
            String texture = "";

            /*if (entity instanceof EntityPlayerSP)
            {
                System.out.println(itemstack.getTagCompound());
            }*/

            if (!itemstack.hasTagCompound())
            {
                return;
            }

            String id = itemstack.getTagCompound().getCompoundTag("ExtraAttributes").getString("id");

            if (id.equals("SUPERIOR_DRAGON_HELMET"))
            {
                texture = "superior";
            }
            else if (id.equals("WISE_DRAGON_HELMET"))
            {
                texture = "wise";
            }

            if (texture.isEmpty())
            {
                return;
            }

            ResourceLocation resource = new ResourceLocation("indicatia:textures/entity/" + texture + ".png");

            GlStateManager.enableBlend();
            GlStateManager.disableAlpha();
            GlStateManager.blendFunc(1, 1);
            GlStateManager.disableLighting();
            GlStateManager.depthMask(!entity.isInvisible());
            float light = 128.0F;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light, light);
            GlStateManager.enableLighting();

            if (entity.isSneaking())
            {
                GlStateManager.translate(0.0F, 0.2F, 0.0F);
            }

            this.render.postRender(0.0625F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            float f3 = 1.1875F;
            GlStateManager.scale(f3, -f3, -f3);
            GlStateManager.enableRescaleNormal();
            GlStateManager.scale(-1.0F, -1.0F, 1.0F);
            GlStateManager.enableAlpha();
            Minecraft.getMinecraft().getTextureManager().bindTexture(resource);
            this.head.render(null, 0.0F, 0.0F, 0.0F, 180.0F, 0.0F, 0.0625F);

            int i = entity.getBrightnessForRender(partialTicks);
            int j = i % 65536;
            int k = i / 65536;
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, j / 1.0F, k / 1.0F);

            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
        }
    }

    @Override
    public boolean shouldCombineTextures()
    {
        return false;
    }
}