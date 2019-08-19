package stevekung.mods.indicatia.mixin;

import java.util.Random;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraft.client.model.ModelBox;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.client.renderer.entity.layers.LayerArrow;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.util.MathHelper;
import stevekung.mods.indicatia.utils.RenderUtils;

@Mixin(LayerArrow.class)
public abstract class LayerArrowMixin implements LayerRenderer<EntityLivingBase>
{
    @Shadow
    @Final
    private RendererLivingEntity field_177168_a;

    @Override
    @Overwrite
    public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale)
    {
        int i = entity.getArrowCountInEntity();

        if (i > 0)
        {
            EntityArrow arrow = new EntityArrow(entity.worldObj, entity.posX, entity.posY, entity.posZ);
            Random rand = new Random(entity.getEntityId());

            for (int j = 0; j < i; ++j)
            {
                GlStateManager.pushMatrix();
                RenderUtils.disableLighting();
                ModelRenderer modelrenderer = this.field_177168_a.getMainModel().getRandomModelBox(rand);
                ModelBox modelbox = modelrenderer.cubeList.get(rand.nextInt(modelrenderer.cubeList.size()));
                modelrenderer.postRender(0.0625F);
                float f = rand.nextFloat();
                float f1 = rand.nextFloat();
                float f2 = rand.nextFloat();
                float f3 = (modelbox.posX1 + (modelbox.posX2 - modelbox.posX1) * f) / 16.0F;
                float f4 = (modelbox.posY1 + (modelbox.posY2 - modelbox.posY1) * f1) / 16.0F;
                float f5 = (modelbox.posZ1 + (modelbox.posZ2 - modelbox.posZ1) * f2) / 16.0F;
                GlStateManager.translate(f3, f4, f5);
                f = f * 2.0F - 1.0F;
                f1 = f1 * 2.0F - 1.0F;
                f2 = f2 * 2.0F - 1.0F;
                f = f * -1.0F;
                f1 = f1 * -1.0F;
                f2 = f2 * -1.0F;
                float f6 = MathHelper.sqrt_float(f * f + f2 * f2);
                arrow.prevRotationYaw = arrow.rotationYaw = (float)(Math.atan2(f, f2) * 180.0D / Math.PI);
                arrow.prevRotationPitch = arrow.rotationPitch = (float)(Math.atan2(f1, f6) * 180.0D / Math.PI);
                double d0 = 0.0D;
                double d1 = 0.0D;
                double d2 = 0.0D;
                this.field_177168_a.getRenderManager().renderEntityWithPosYaw(arrow, d0, d1, d2, 0.0F, partialTicks);
                GlStateManager.popMatrix();
                RenderUtils.enableLighting();
            }
        }
    }
}