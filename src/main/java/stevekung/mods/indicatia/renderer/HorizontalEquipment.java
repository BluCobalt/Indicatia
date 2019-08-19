package stevekung.mods.indicatia.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.ItemBow;
import net.minecraft.item.ItemStack;
import stevekung.mods.indicatia.config.EnumEquipment;
import stevekung.mods.indicatia.config.ExtendedConfig;
import stevekung.mods.indicatia.utils.ColorUtils;

public class HorizontalEquipment
{
    private final ItemStack itemStack;
    private final boolean isArmor;
    private int width;
    private int itemDamageWidth;
    private String itemDamage = "";

    public HorizontalEquipment(ItemStack itemStack, boolean isArmor)
    {
        this.itemStack = itemStack;
        this.isArmor = isArmor;
        this.initSize();
    }

    public int getWidth()
    {
        return this.width;
    }

    public void render(int x, int y)
    {
        boolean isRightSide = EnumEquipment.Position.getById(ExtendedConfig.instance.equipmentPosition).equalsIgnoreCase("right");
        HUDInfo.renderItem(this.itemStack, isRightSide ? x - 18 : x, y);
        Minecraft.getMinecraft().fontRendererObj.drawString(ColorUtils.stringToRGB(ExtendedConfig.instance.equipmentStatusColor).toColoredFont() + this.itemDamage, isRightSide ? x - 20 - this.itemDamageWidth : x + 18, y + 4, 16777215, true);

        if (this.itemStack.getItem() instanceof ItemBow)
        {
            int arrowCount = HUDInfo.getInventoryArrowCount(Minecraft.getMinecraft().thePlayer.inventory);
            GlStateManager.disableDepth();
            ColorUtils.unicodeFontRenderer.drawString(ColorUtils.stringToRGB(ExtendedConfig.instance.arrowCountColor).toColoredFont() + HUDInfo.getArrowStackCount(arrowCount), isRightSide ? x - 10 : x + 8, y + 8, 16777215, true);
            GlStateManager.enableDepth();
        }
    }

    private void initSize()
    {
        String itemCount = HUDInfo.getInventoryItemCount(Minecraft.getMinecraft().thePlayer.inventory, this.itemStack);

        if (this.isArmor)
        {
            this.itemDamage = this.itemStack.isItemStackDamageable() ? HUDInfo.getArmorDurabilityStatus(this.itemStack) : HUDInfo.getItemStackCount(this.itemStack, Integer.parseInt(itemCount));
        }
        else
        {
            String status = EnumEquipment.Status.getById(ExtendedConfig.instance.equipmentStatus);
            this.itemDamage = this.itemStack.isItemStackDamageable() ? HUDInfo.getArmorDurabilityStatus(this.itemStack) : status.equalsIgnoreCase("none") ? "" : HUDInfo.getItemStackCount(this.itemStack, Integer.parseInt(itemCount));
        }
        this.itemDamageWidth = Minecraft.getMinecraft().fontRendererObj.getStringWidth(this.itemDamage);
        this.width = 20 + this.itemDamageWidth;
    }
}