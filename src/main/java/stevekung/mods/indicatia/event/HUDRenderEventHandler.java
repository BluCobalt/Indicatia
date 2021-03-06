package stevekung.mods.indicatia.event;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.StringUtils;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import stevekung.mods.indicatia.config.*;
import stevekung.mods.indicatia.core.IndicatiaMod;
import stevekung.mods.indicatia.gui.config.GuiRenderPreview;
import stevekung.mods.indicatia.renderer.HUDInfo;
import stevekung.mods.indicatia.renderer.KeystrokeRenderer;
import stevekung.mods.indicatia.utils.InfoUtils;
import stevekung.mods.indicatia.utils.LoggerIN;
import stevekung.mods.indicatia.utils.RenderUtilsIN;
import stevekung.mods.stevekunglib.utils.ColorUtils;
import stevekung.mods.stevekunglib.utils.JsonUtils;

public class HUDRenderEventHandler
{
    private Minecraft mc;
    public static boolean recordEnable;
    private int recTick;
    private static int readFileTicks;
    public static String topDonator = "";
    public static String recentDonator = "";
    private static String topDonatorName = "";
    private static String topDonatorCount = "";
    private static String recentDonatorName = "";
    private static String recentDonatorCount = "";
    public static final DecimalFormat tpsFormat = new DecimalFormat("########0.00");
    public static String currentLiveViewCount = "";

    public HUDRenderEventHandler()
    {
        this.mc = Minecraft.getMinecraft();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
        {
            HUDRenderEventHandler.readFileTicks++;

            if (HUDRenderEventHandler.recordEnable)
            {
                this.recTick++;
            }
            else
            {
                this.recTick = 0;
            }

            if (!ExtendedConfig.instance.topDonatorFilePath.isEmpty())
            {
                HUDRenderEventHandler.readTopDonatorFile();
            }
            else
            {
                HUDRenderEventHandler.topDonator = "";
            }

            if (!ExtendedConfig.instance.recentDonatorFilePath.isEmpty())
            {
                HUDRenderEventHandler.readRecentDonatorFile();
            }
            else
            {
                HUDRenderEventHandler.recentDonator = "";
            }

            if (IndicatiaMod.isYoutubeChatLoaded)
            {
                try
                {
                    Class<?> clazz = Class.forName("stevekung.mods.ytchat.utils.YouTubeChatService");
                    HUDRenderEventHandler.currentLiveViewCount = (String)clazz.getField("currentLiveViewCount").get(null);
                }
                catch (Exception e)
                {
                    HUDRenderEventHandler.currentLiveViewCount = TextFormatting.RED + "unavailable";
                }
            }
        }
    }

    @SubscribeEvent
    public void onPreInfoRender(RenderGameOverlayEvent.Pre event)
    {
        if (event.getType() == RenderGameOverlayEvent.ElementType.BOSSHEALTH)
        {
            event.setCanceled(!ConfigManagerIN.indicatia_general.enableRenderBossHealthStatus);
            GlStateManager.enableDepth();
            GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        }
        if (event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR || event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS)
        {
            if (this.mc.currentScreen instanceof GuiRenderPreview)
            {
                event.setCanceled(true);
            }
        }
        if (event.getType() == RenderGameOverlayEvent.ElementType.TEXT)
        {
            if (ConfigManagerIN.indicatia_general.enableRenderInfo && !this.mc.gameSettings.hideGUI && !this.mc.gameSettings.showDebugInfo && this.mc.player != null && this.mc.world != null && !(this.mc.currentScreen instanceof GuiRenderPreview))
            {
                List<String> leftInfo = new LinkedList<>();
                List<String> rightInfo = new LinkedList<>();
                MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();

                // left info
                if (ExtendedConfig.instance.ping && !this.mc.isSingleplayer())
                {
                    leftInfo.add(HUDInfo.getPing());

                    if (ExtendedConfig.instance.pingToSecond)
                    {
                        leftInfo.add(HUDInfo.getPingToSecond());
                    }
                }
                if (ExtendedConfig.instance.serverIP && !this.mc.isSingleplayer())
                {
                    if (this.mc.isConnectedToRealms())
                    {
                        leftInfo.add(HUDInfo.getRealmName(this.mc));
                    }
                    if (this.mc.getCurrentServerData() != null)
                    {
                        leftInfo.add(HUDInfo.getServerIP(this.mc));
                    }
                }
                if (ExtendedConfig.instance.fps)
                {
                    leftInfo.add(HUDInfo.getFPS());
                }
                if (ExtendedConfig.instance.xyz)
                {
                    leftInfo.add(HUDInfo.getXYZ(this.mc));

                    if (this.mc.player.dimension == -1)
                    {
                        leftInfo.add(HUDInfo.getOverworldXYZFromNether(this.mc));
                    }
                }
                if (ExtendedConfig.instance.direction)
                {
                    leftInfo.add(HUDInfo.renderDirection(this.mc));
                }
                if (ExtendedConfig.instance.biome)
                {
                    leftInfo.add(HUDInfo.getBiome(this.mc));
                }
                if (ExtendedConfig.instance.slimeChunkFinder && this.mc.player.dimension == 0)
                {
                    String isSlimeChunk = InfoUtils.INSTANCE.isSlimeChunk(this.mc.player.getPosition()) ? "Yes" : "No";
                    leftInfo.add(ColorUtils.stringToRGB(ExtendedConfig.instance.slimeChunkColor).toColoredFont() + "Slime Chunk: " + ColorUtils.stringToRGB(ExtendedConfig.instance.slimeChunkValueColor).toColoredFont() + isSlimeChunk);
                }
                if (CPSPosition.getById(ExtendedConfig.instance.cpsPosition).equalsIgnoreCase("left"))
                {
                    if (ExtendedConfig.instance.cps)
                    {
                        leftInfo.add(HUDInfo.getCPS());
                    }
                    if (ExtendedConfig.instance.rcps)
                    {
                        leftInfo.add(HUDInfo.getRCPS());
                    }
                }
                if (ConfigManagerIN.indicatia_donation.donatorMessagePosition == ConfigManagerIN.Donation.DonatorMessagePos.LEFT)
                {
                    if (!HUDRenderEventHandler.topDonator.isEmpty())
                    {
                        String text = ExtendedConfig.instance.topDonatorText.isEmpty() ? "" : ExtendedConfig.instance.topDonatorText + TextFormatting.RESET + " ";
                        leftInfo.add(text + HUDRenderEventHandler.topDonator);
                    }
                    if (!HUDRenderEventHandler.recentDonator.isEmpty())
                    {
                        String text = ExtendedConfig.instance.recentDonatorText.isEmpty() ? "" : ExtendedConfig.instance.recentDonatorText + TextFormatting.RESET + " ";
                        leftInfo.add(text + HUDRenderEventHandler.recentDonator);
                    }
                }
                // server tps
                if (ExtendedConfig.instance.tps && server != null)
                {
                    int dimension = 0;
                    double overallTPS = HUDRenderEventHandler.mean(server.tickTimeArray) * 1.0E-6D;
                    double overworldTPS = HUDRenderEventHandler.mean(server.worldTickTimes.get(dimension)) * 1.0E-6D;
                    double tps = Math.min(1000.0D / overallTPS, 20);

                    leftInfo.add(ColorUtils.stringToRGB(ExtendedConfig.instance.tpsColor).toColoredFont() + "Overall TPS: " + ColorUtils.stringToRGB(ExtendedConfig.instance.tpsValueColor).toColoredFont() + HUDRenderEventHandler.tpsFormat.format(overallTPS));

                    if (ExtendedConfig.instance.tpsAllDims)
                    {
                        for (Integer dimensionIds : DimensionManager.getIDs())
                        {
                            long[] values = server.worldTickTimes.get(dimensionIds);

                            if (values == null)
                            {
                                LoggerIN.error("Got null Dimension ID {}! Skipped TPS from dimension", values);
                                return;
                            }
                            double dimensionTPS = HUDRenderEventHandler.mean(values) * 1.0E-6D;
                            leftInfo.add(ColorUtils.stringToRGB(ExtendedConfig.instance.tpsColor).toColoredFont() + "Dimension " + server.getWorld(dimensionIds).provider.getDimensionType().getName() + " " + dimensionIds + ": " + ColorUtils.stringToRGB(ExtendedConfig.instance.tpsValueColor).toColoredFont() + HUDRenderEventHandler.tpsFormat.format(dimensionTPS));
                        }
                    }
                    else
                    {
                        leftInfo.add(ColorUtils.stringToRGB(ExtendedConfig.instance.tpsColor).toColoredFont() + "Overworld TPS: " + ColorUtils.stringToRGB(ExtendedConfig.instance.tpsValueColor).toColoredFont() + HUDRenderEventHandler.tpsFormat.format(overworldTPS));
                    }
                    leftInfo.add(ColorUtils.stringToRGB(ExtendedConfig.instance.tpsColor).toColoredFont() + "TPS: " + ColorUtils.stringToRGB(ExtendedConfig.instance.tpsValueColor).toColoredFont() + HUDRenderEventHandler.tpsFormat.format(tps));
                }

                // right info
                if (ExtendedConfig.instance.realTime)
                {
                    rightInfo.add(HUDInfo.getCurrentTime());
                }
                if (ExtendedConfig.instance.gameTime)
                {
                    rightInfo.add(HUDInfo.getCurrentGameTime(this.mc));
                }
                if (ExtendedConfig.instance.gameWeather && this.mc.world.isRaining())
                {
                    rightInfo.add(HUDInfo.getGameWeather(this.mc));
                }
                if (ExtendedConfig.instance.moonPhase)
                {
                    rightInfo.add(InfoUtils.INSTANCE.getMoonPhase(this.mc));
                }
                if (CPSPosition.getById(ExtendedConfig.instance.cpsPosition).equalsIgnoreCase("right"))
                {
                    if (ExtendedConfig.instance.cps)
                    {
                        rightInfo.add(HUDInfo.getCPS());
                    }
                    if (ExtendedConfig.instance.rcps)
                    {
                        rightInfo.add(HUDInfo.getRCPS());
                    }
                }
                if (ConfigManagerIN.indicatia_donation.donatorMessagePosition == ConfigManagerIN.Donation.DonatorMessagePos.RIGHT)
                {
                    if (!HUDRenderEventHandler.topDonator.isEmpty())
                    {
                        String text = ExtendedConfig.instance.topDonatorText.isEmpty() ? "" : ExtendedConfig.instance.topDonatorText + TextFormatting.RESET + " ";
                        rightInfo.add(text + HUDRenderEventHandler.topDonator);
                    }
                    if (!HUDRenderEventHandler.recentDonator.isEmpty())
                    {
                        String text = ExtendedConfig.instance.recentDonatorText.isEmpty() ? "" : ExtendedConfig.instance.recentDonatorText + TextFormatting.RESET + " ";
                        rightInfo.add(text + HUDRenderEventHandler.recentDonator);
                    }
                }
                if (IndicatiaMod.isYoutubeChatLoaded && !HUDRenderEventHandler.currentLiveViewCount.isEmpty())
                {
                    rightInfo.add(ColorUtils.stringToRGB(ExtendedConfig.instance.ytChatViewCountColor).toColoredFont() + "Current watched: " + ColorUtils.stringToRGB(ExtendedConfig.instance.ytChatViewCountValueColor).toColoredFont() + HUDRenderEventHandler.currentLiveViewCount);
                }

                // equipments
                if (!this.mc.player.isSpectator() && ExtendedConfig.instance.equipmentHUD)
                {
                    if (EnumEquipment.Position.getById(ExtendedConfig.instance.equipmentPosition).equalsIgnoreCase("hotbar"))
                    {
                        HUDInfo.renderHotbarEquippedItems(this.mc);
                    }
                    else
                    {
                        if (EnumEquipment.Direction.getById(ExtendedConfig.instance.equipmentDirection).equalsIgnoreCase("vertical"))
                        {
                            HUDInfo.renderVerticalEquippedItems(this.mc);
                        }
                        else
                        {
                            HUDInfo.renderHorizontalEquippedItems(this.mc);
                        }
                    }
                }

                if (ExtendedConfig.instance.potionHUD)
                {
                    HUDInfo.renderPotionHUD(this.mc);
                }

                // left info
                for (int i = 0; i < leftInfo.size(); ++i)
                {
                    ScaledResolution res = new ScaledResolution(this.mc);
                    String string = leftInfo.get(i);
                    float fontHeight = this.mc.fontRenderer.FONT_HEIGHT + 1;
                    float yOffset = 3 + fontHeight * i;
                    float xOffset = res.getScaledWidth() - 2 - this.mc.fontRenderer.getStringWidth(string);

                    if (!StringUtils.isNullOrEmpty(string))
                    {
                        this.mc.fontRenderer.drawString(string, ExtendedConfig.instance.swapRenderInfo ? xOffset : 3.0625F, yOffset, 16777215, true);
                    }
                }

                // right info
                for (int i = 0; i < rightInfo.size(); ++i)
                {
                    ScaledResolution res = new ScaledResolution(this.mc);
                    String string = rightInfo.get(i);
                    float fontHeight = this.mc.fontRenderer.FONT_HEIGHT + 1;
                    float yOffset = 3 + fontHeight * i;
                    float xOffset = res.getScaledWidth() - 2 - this.mc.fontRenderer.getStringWidth(string);

                    if (!StringUtils.isNullOrEmpty(string))
                    {
                        this.mc.fontRenderer.drawString(string, ExtendedConfig.instance.swapRenderInfo ? 3.0625F : xOffset, yOffset, 16777215, true);
                    }
                }
            }

            if (HUDRenderEventHandler.recordEnable)
            {
                ScaledResolution res = new ScaledResolution(this.mc);
                int color = 16777215;

                if (this.recTick % 24 >= 0 && this.recTick % 24 <= 12)
                {
                    color = 16733525;
                }
                this.mc.fontRenderer.drawString("REC: " + StringUtils.ticksToElapsedTime(this.recTick), res.getScaledWidth() - this.mc.fontRenderer.getStringWidth("REC: " + StringUtils.ticksToElapsedTime(this.recTick)) - 2, res.getScaledHeight() - 10, color, true);
            }

            if (!this.mc.gameSettings.hideGUI && !this.mc.gameSettings.showDebugInfo)
            {
                if (ExtendedConfig.instance.keystroke)
                {
                    if (this.mc.currentScreen == null || this.mc.currentScreen instanceof GuiChat)
                    {
                        KeystrokeRenderer.render(this.mc);
                    }
                }
                if (ConfigManagerIN.indicatia_general.enableRenderInfo && ExtendedConfig.instance.cps && CPSPosition.getById(ExtendedConfig.instance.cpsPosition).equalsIgnoreCase("custom") && (this.mc.currentScreen == null || this.mc.currentScreen instanceof GuiChat))
                {
                    String rcps = ExtendedConfig.instance.rcps ? " " + HUDInfo.getRCPS() : "";
                    RenderUtilsIN.drawRect(ExtendedConfig.instance.cpsCustomXOffset, ExtendedConfig.instance.cpsCustomYOffset, ExtendedConfig.instance.cpsCustomXOffset + this.mc.fontRenderer.getStringWidth(HUDInfo.getCPS() + rcps) + 4, ExtendedConfig.instance.cpsCustomYOffset + 11, 16777216, ExtendedConfig.instance.cpsOpacity / 100.0F);
                    this.mc.fontRenderer.drawString(HUDInfo.getCPS() + rcps, ExtendedConfig.instance.cpsCustomXOffset + 2, ExtendedConfig.instance.cpsCustomYOffset + 2, 16777215, true);
                }
            }
        }
        if (event.getType() == RenderGameOverlayEvent.ElementType.CHAT)
        {
            if (this.mc.currentScreen instanceof GuiRenderPreview)
            {
                event.setCanceled(true);
                return;
            }
        }
        if (event.getType() == RenderGameOverlayEvent.ElementType.POTION_ICONS)
        {
            if (!ConfigManagerIN.indicatia_general.enableVanillaPotionHUD || this.mc.currentScreen instanceof GuiRenderPreview)
            {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onRenderHealthStatus(RenderLivingEvent.Specials.Post event)
    {
        EntityLivingBase entity = event.getEntity();
        float health = entity.getHealth();
        boolean halfHealth = health <= entity.getMaxHealth() / 2F;
        boolean halfHealth1 = health <= entity.getMaxHealth() / 4F;
        float range = entity.isSneaking() ? RenderLivingBase.NAME_TAG_RANGE_SNEAK : RenderLivingBase.NAME_TAG_RANGE;
        double distance = entity.getDistanceSq(this.mc.getRenderManager().renderViewEntity);
        String mode = HealthStatusMode.getById(ExtendedConfig.instance.healthStatusMode);
        boolean flag = mode.equalsIgnoreCase("disabled") ? false : mode.equalsIgnoreCase("pointed") ? entity == InfoUtils.INSTANCE.extendedPointedEntity : true;
        Style color = halfHealth ? JsonUtils.red() : halfHealth1 ? JsonUtils.darkRed() : JsonUtils.green();

        if (distance < range * range)
        {
            if (!this.mc.gameSettings.hideGUI && !entity.isInvisible() && flag && !(entity instanceof EntityPlayerSP || entity instanceof EntityArmorStand) && !InfoUtils.INSTANCE.isHypixel())
            {
                String heart = JsonUtils.create("\u2764 ").setStyle(color).getFormattedText();
                GlStateManager.alphaFunc(516, 0.1F);
                RenderUtilsIN.renderEntityHealth(entity, heart + String.format("%.1f", health), event.getX(), event.getY(), event.getZ());
            }
        }
    }

    private static void readTopDonatorFile()
    {
        File file = new File("/" + ExtendedConfig.instance.topDonatorFilePath);
        String text = "";

        if (HUDRenderEventHandler.readFileTicks % ConfigManagerIN.indicatia_donation.readFileInterval == 0)
        {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)))
            {
                String line;

                while ((line = reader.readLine()) != null)
                {
                    if (!line.trim().equals(""))
                    {
                        text = line.replace("\r", "");
                    }
                }
                String[] textSplit = text.split(" ");
                HUDRenderEventHandler.topDonatorName = textSplit[0];
                HUDRenderEventHandler.topDonatorCount = textSplit[1];
            }
            catch (Exception e)
            {
                LoggerIN.error("Couldn't read text file from path {}", file.getPath());
                e.printStackTrace();
                HUDRenderEventHandler.topDonator = TextFormatting.RED + "Cannot read text file!";
            }
        }
        HUDRenderEventHandler.topDonator = ColorUtils.stringToRGB(ExtendedConfig.instance.topDonatorNameColor).toColoredFont() + HUDRenderEventHandler.topDonatorName + ColorUtils.stringToRGB(ExtendedConfig.instance.topDonatorValueColor).toColoredFont() + " " + HUDRenderEventHandler.topDonatorCount.replace("THB", "") + "THB";
    }

    private static void readRecentDonatorFile()
    {
        File file = new File("/" + ExtendedConfig.instance.recentDonatorFilePath);
        String text = "";

        if (HUDRenderEventHandler.readFileTicks % ConfigManagerIN.indicatia_donation.readFileInterval == 0)
        {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)))
            {
                String line;

                while ((line = reader.readLine()) != null)
                {
                    if (!line.trim().equals(""))
                    {
                        text = line.replace("\r", "");
                    }
                }
                String[] textSplit = text.split(" ");
                HUDRenderEventHandler.recentDonatorName = textSplit[0];
                HUDRenderEventHandler.recentDonatorCount = textSplit[1];
            }
            catch (Exception e)
            {
                LoggerIN.error("Couldn't read text file from path {}", file.getPath());
                e.printStackTrace();
                HUDRenderEventHandler.recentDonator = TextFormatting.RED + "Cannot read text file!";
            }
        }
        HUDRenderEventHandler.recentDonator = ColorUtils.stringToRGB(ExtendedConfig.instance.recentDonatorNameColor).toColoredFont() + HUDRenderEventHandler.recentDonatorName + ColorUtils.stringToRGB(ExtendedConfig.instance.recentDonatorValueColor).toColoredFont() + " " + HUDRenderEventHandler.recentDonatorCount.replace("THB", "") + "THB";
    }

    public static long mean(long[] values)
    {
        long sum = 0L;

        for (long value : values)
        {
            sum += value;
        }
        return sum / values.length;
    }
}