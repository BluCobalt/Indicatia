package stevekung.mods.indicatia.gui.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import stevekung.mods.indicatia.config.ExtendedConfig;
import stevekung.mods.stevekunglib.utils.LangUtils;

@SideOnly(Side.CLIENT)
public class GuiRenderInfoSettings extends GuiScreen
{
    private final GuiScreen parent;
    private GuiConfigButtonRowList optionsRowList;
    private static final List<ExtendedConfig.Options> OPTIONS = new ArrayList<>();

    static
    {
        OPTIONS.add(ExtendedConfig.Options.FPS);
        OPTIONS.add(ExtendedConfig.Options.XYZ);
        OPTIONS.add(ExtendedConfig.Options.DIRECTION);
        OPTIONS.add(ExtendedConfig.Options.BIOME);
        OPTIONS.add(ExtendedConfig.Options.PING);
        OPTIONS.add(ExtendedConfig.Options.PING_TO_SECOND);
        OPTIONS.add(ExtendedConfig.Options.SERVER_IP);
        OPTIONS.add(ExtendedConfig.Options.SERVER_IP_MC);
        OPTIONS.add(ExtendedConfig.Options.EQUIPMENT_HUD);
        OPTIONS.add(ExtendedConfig.Options.POTION_HUD);
        OPTIONS.add(ExtendedConfig.Options.KEYSTROKE);
        OPTIONS.add(ExtendedConfig.Options.KEYSTROKE_LRMB);
        OPTIONS.add(ExtendedConfig.Options.KEYSTROKE_SS);
        OPTIONS.add(ExtendedConfig.Options.KEYSTROKE_BLOCKING);
        OPTIONS.add(ExtendedConfig.Options.CPS);
        OPTIONS.add(ExtendedConfig.Options.RCPS);
        OPTIONS.add(ExtendedConfig.Options.SLIME_CHUNK);
        OPTIONS.add(ExtendedConfig.Options.REAL_TIME);
        OPTIONS.add(ExtendedConfig.Options.GAME_TIME);
        OPTIONS.add(ExtendedConfig.Options.GAME_WEATHER);
        OPTIONS.add(ExtendedConfig.Options.MOON_PHASE);
        OPTIONS.add(ExtendedConfig.Options.POTION_ICON);
        OPTIONS.add(ExtendedConfig.Options.TPS);
        OPTIONS.add(ExtendedConfig.Options.TPS_ALL_DIMS);
        OPTIONS.add(ExtendedConfig.Options.ALTERNATE_POTION_COLOR);
    }

    public GuiRenderInfoSettings(GuiScreen parent)
    {
        this.parent = parent;
    }

    @Override
    public void initGui()
    {
        this.buttonList.clear();
        this.buttonList.add(new GuiButton(200, this.width / 2 - 100, this.height - 27, LangUtils.translate("gui.done")));

        ExtendedConfig.Options[] options = new ExtendedConfig.Options[OPTIONS.size()];
        options = OPTIONS.toArray(options);
        this.optionsRowList = new GuiConfigButtonRowList(this.width, this.height, 32, this.height - 32, 25, options);
    }

    @Override
    public void handleMouseInput() throws IOException
    {
        super.handleMouseInput();
        this.optionsRowList.handleMouseInput();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException
    {
        if (keyCode == 1)
        {
            ExtendedConfig.instance.save();
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
    {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        this.optionsRowList.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state)
    {
        super.mouseReleased(mouseX, mouseY, state);
        this.optionsRowList.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException
    {
        if (button.enabled)
        {
            if (button.id == 200)
            {
                ExtendedConfig.instance.save();
                this.mc.displayGuiScreen(this.parent);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks)
    {
        this.drawDefaultBackground();
        this.optionsRowList.drawScreen(mouseX, mouseY, partialTicks);
        this.drawCenteredString(this.fontRenderer, LangUtils.translate("extended_config.render_info.title"), this.width / 2, 5, 16777215);

        if (GuiConfigButtonRowList.comment != null)
        {
            List<String> wrappedLine = this.fontRenderer.listFormattedStringToWidth(GuiConfigButtonRowList.comment, 250);
            int y = 15;

            for (String text : wrappedLine)
            {
                this.drawCenteredString(this.fontRenderer, TextFormatting.GREEN + text, this.width / 2, y, 16777215);
                y += this.fontRenderer.FONT_HEIGHT;
            }
        }
        else
        {
            this.drawCenteredString(this.fontRenderer, TextFormatting.YELLOW + LangUtils.translate("extended_config.render_info.rclick.info"), this.width / 2, 15, 16777215);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}