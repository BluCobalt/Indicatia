package stevekung.mods.indicatia.mixin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

@Mixin(GuiContainer.class)
public abstract class GuiContainerMixin extends GuiScreen
{
    private final GuiContainer that = (GuiContainer) (Object) this;
    private static final List<String> IGNORE_ITEMS = new ArrayList<>(Arrays.asList(" ", "Recipe Required"));
    private static final List<String> IGNORE_TOOLTIPS = new ArrayList<>(Arrays.asList(" "));

    @Inject(method = "handleMouseClick(Lnet/minecraft/inventory/Slot;III)V", cancellable = true, at = @At("HEAD"))
    private void handleMouseClick(Slot slot, int slotId, int clickedButton, int clickType, CallbackInfo info)
    {
        if (slotId != -999)
        {
            ItemStack itemStack = this.that.inventorySlots.getSlot(slotId).getStack();

            if (itemStack != null)
            {
                if (this.ignoreNullItem(itemStack, IGNORE_ITEMS))
                {
                    info.cancel();
                }
            }
        }
    }

    @Override
    protected void renderToolTip(ItemStack itemStack, int x, int y)
    {
        if (itemStack != null)
        {
            if (this.ignoreNullItem(itemStack, IGNORE_TOOLTIPS))
            {
                return;
            }
        }
        super.renderToolTip(itemStack, x, y);
    }

    private boolean ignoreNullItem(ItemStack itemStack, List<String> ignores)
    {
        String displayName = EnumChatFormatting.getTextWithoutFormattingCodes(itemStack.getDisplayName());
        return ignores.stream().anyMatch(name -> displayName.equals(name));
    }
}