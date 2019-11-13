package stevekung.mods.indicatia.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import stevekung.mods.indicatia.config.ConfigManagerIN;
import stevekung.mods.indicatia.renderer.LayerGlowingTexture;

@Mixin(RenderPlayer.class)
public abstract class RenderPlayerMixin
{
    private final RenderPlayer that = (RenderPlayer) (Object) this;

    @Shadow
    public abstract ModelPlayer getMainModel();

    @Inject(method = "<init>(Lnet/minecraft/client/renderer/entity/RenderManager;Z)V", cancellable = true, at = @At("RETURN"))
    private void init(RenderManager renderManager, boolean useSmallArms, CallbackInfo ci)
    {
        this.that.addLayer(new LayerGlowingTexture(this.getMainModel().bipedHead));
    }

    @Inject(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V", cancellable = true, at = @At(value = "INVOKE", target = "net/minecraft/client/renderer/entity/RenderPlayer.setModelVisibilities(Lnet/minecraft/client/entity/AbstractClientPlayer;)V", shift = At.Shift.AFTER))
    private void renderPre(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci)
    {
        if (ConfigManagerIN.enableAlternatePlayerModel)
        {
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        }
    }

    @Inject(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V", cancellable = true, at = @At(value = "INVOKE", target = "net/minecraft/client/renderer/entity/RendererLivingEntity.doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", shift = At.Shift.AFTER))
    private void renderPost(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci)
    {
        if (ConfigManagerIN.enableAlternatePlayerModel)
        {
            GlStateManager.disableBlend();
        }
    }
}