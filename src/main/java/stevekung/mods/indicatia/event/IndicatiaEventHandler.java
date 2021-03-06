package stevekung.mods.indicatia.event;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemFishingRod;
import net.minecraft.item.ItemStack;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.status.INetHandlerStatusClient;
import net.minecraft.network.status.client.CPacketPing;
import net.minecraft.network.status.client.CPacketServerQuery;
import net.minecraft.network.status.server.SPacketPong;
import net.minecraft.network.status.server.SPacketServerInfo;
import net.minecraft.realms.RealmsBridge;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.MovementInput;
import net.minecraft.util.StringUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import stevekung.mods.indicatia.config.ConfigManagerIN;
import stevekung.mods.indicatia.config.ExtendedConfig;
import stevekung.mods.indicatia.core.IndicatiaMod;
import stevekung.mods.indicatia.gui.*;
import stevekung.mods.indicatia.gui.config.GuiExtendedConfig;
import stevekung.mods.indicatia.gui.config.GuiRenderPreview;
import stevekung.mods.indicatia.handler.KeyBindingHandler;
import stevekung.mods.indicatia.utils.AutoLoginFunction;
import stevekung.mods.indicatia.utils.CapeUtils;
import stevekung.mods.indicatia.utils.InfoUtils;
import stevekung.mods.indicatia.utils.LoggerIN;
import stevekung.mods.stevekunglib.utils.JsonUtils;
import stevekung.mods.stevekunglib.utils.LangUtils;
import stevekung.mods.stevekunglib.utils.client.ClientUtils;
import stevekung.mods.stevekunglib.utils.enums.CachedEnum;

public class IndicatiaEventHandler
{
    private Minecraft mc;
    public static final List<Long> LEFT_CLICK = new ArrayList<>();
    public static final List<Long> RIGHT_CLICK = new ArrayList<>();
    public static int currentServerPing;
    private static final ThreadPoolExecutor serverPinger = new ScheduledThreadPoolExecutor(5, new ThreadFactoryBuilder().setNameFormat("Real Time Server Pinger #%d").setDaemon(true).build());
    private static int pendingPingTicks = 100;
    private int disconnectClickCount;
    private int disconnectClickCooldown;
    private boolean initVersionCheck;

    public static boolean isAFK;
    public static String afkMode = "idle";
    public static String afkReason;
    public static int afkMoveTicks;
    public static int afkTicks;

    static boolean printAutoGG;
    static int printAutoGGTicks;

    public static boolean autoFish;
    public static int autoFishTick;

    private static long sneakTimeOld = 0L;
    private static boolean sneakingOld = false;

    public IndicatiaEventHandler()
    {
        this.mc = Minecraft.getMinecraft();
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (this.mc.player != null)
        {
            if (!this.initVersionCheck)
            {
                IndicatiaMod.CHECKER.startCheckIfFailed();

                if (ConfigManagerIN.indicatia_general.enableVersionChecker)
                {
                    IndicatiaMod.CHECKER.printInfo(this.mc.player);
                }
                this.initVersionCheck = true;
            }
            if (event.phase == TickEvent.Phase.START)
            {
                IndicatiaEventHandler.runAFK(this.mc.player);
                IndicatiaEventHandler.processAutoFish(this.mc);
                AutoLoginFunction.runAutoLoginFunction();
                CapeUtils.loadCapeTexture();

                if (IndicatiaEventHandler.printAutoGG && IndicatiaEventHandler.printAutoGGTicks < ConfigManagerIN.indicatia_general.autoGGDelay)
                {
                    IndicatiaEventHandler.printAutoGGTicks++;

                    if (IndicatiaEventHandler.printAutoGGTicks == ConfigManagerIN.indicatia_general.autoGGDelay)
                    {
                        this.mc.player.sendChatMessage(ConfigManagerIN.indicatia_general.autoGGMessage);
                        IndicatiaEventHandler.printAutoGG = false;
                        IndicatiaEventHandler.printAutoGGTicks = 0;
                    }
                }
                if (AutoLoginFunction.functionDelay > 0)
                {
                    AutoLoginFunction.functionDelay--;
                }
                if (AutoLoginFunction.functionDelay == 0)
                {
                    AutoLoginFunction.runAutoLoginFunctionTicks(this.mc);
                }
                if (this.disconnectClickCooldown > 0)
                {
                    this.disconnectClickCooldown--;
                }
                if (this.mc.currentScreen != null && this.mc.currentScreen instanceof GuiIngameMenu)
                {
                    if (ConfigManagerIN.indicatia_general.enableConfirmDisconnectButton && !this.mc.isSingleplayer())
                    {
                        this.mc.currentScreen.buttonList.forEach(button ->
                        {
                            if (button.id == 1 && ConfigManagerIN.indicatia_general.confirmDisconnectMode == ConfigManagerIN.General.DisconnectMode.CLICK)
                            {
                                if (this.disconnectClickCooldown < 60)
                                {
                                    int cooldownSec = 1 + this.disconnectClickCooldown / 20;
                                    button.displayString = TextFormatting.RED + LangUtils.translate("message.confirm_disconnect") + " in " + cooldownSec + "...";
                                }
                                if (this.disconnectClickCooldown == 0)
                                {
                                    button.displayString = LangUtils.translate("menu.disconnect");
                                    this.disconnectClickCount = 0;
                                }
                            }
                        });
                    }
                }
                else
                {
                    this.disconnectClickCount = 0;
                    this.disconnectClickCooldown = 0;
                }

                if (IndicatiaEventHandler.pendingPingTicks > 0 && this.mc.getCurrentServerData() != null)
                {
                    IndicatiaEventHandler.pendingPingTicks--;

                    if (IndicatiaEventHandler.pendingPingTicks == 0)
                    {
                        IndicatiaEventHandler.getRealTimeServerPing(this.mc.getCurrentServerData());
                        IndicatiaEventHandler.pendingPingTicks = 100;
                    }
                }

                for (EnumAction action : CachedEnum.actionValues)
                {
                    if (action != EnumAction.NONE)
                    {
                        if (ConfigManagerIN.indicatia_general.enableAdditionalBlockhitAnimation && this.mc.gameSettings.keyBindAttack.isKeyDown() && this.mc.objectMouseOver != null && this.mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK && !this.mc.player.getHeldItemMainhand().isEmpty() && this.mc.player.getHeldItemMainhand().getItemUseAction() == action)
                        {
                            this.mc.player.swingArm(EnumHand.MAIN_HAND);
                        }
                    }
                }
            }
        }
        GuiIngameForge.renderObjective = ConfigManagerIN.indicatia_general.enableRenderScoreboard;
    }

    @SubscribeEvent
    public void onInputUpdate(InputUpdateEvent event)
    {
        MovementInput movement = event.getMovementInput();
        EntityPlayer player = event.getEntityPlayer();

        if (ConfigManagerIN.indicatia_general.enableCustomMovementHandler)
        {
            // canceled turn back
            if (ExtendedConfig.instance.toggleSprintUseMode.equalsIgnoreCase("key_binding") && KeyBindingHandler.KEY_TOGGLE_SPRINT.isKeyDown())
            {
                ++movement.moveForward;
            }

            // toggle sneak
            movement.sneak = this.mc.gameSettings.keyBindSneak.isKeyDown() || ExtendedConfig.instance.toggleSneak && !event.getEntityPlayer().isSpectator();

            if (ExtendedConfig.instance.toggleSneak && !player.isSpectator() && !player.isCreative())
            {
                movement.moveStrafe = (float)(movement.moveStrafe * 0.3D);
                movement.moveForward = (float)(movement.moveForward * 0.3D);
            }

            // toggle sprint
            if (ExtendedConfig.instance.toggleSprint && !player.isPotionActive(MobEffects.BLINDNESS) && !ExtendedConfig.instance.toggleSneak)
            {
                player.setSprinting(true);
            }

            // afk stuff
            if (IndicatiaEventHandler.afkMode.equals("360_move"))
            {
                int afkMoveTick = IndicatiaEventHandler.afkMoveTicks;

                if (afkMoveTick > 0 && afkMoveTick < 2)
                {
                    movement.moveForward += Math.random();
                    movement.forwardKeyDown = true;
                }
                else if (afkMoveTick > 2 && afkMoveTick < 4)
                {
                    movement.moveStrafe += Math.random();
                    movement.leftKeyDown = true;
                }
                else if (afkMoveTick > 4 && afkMoveTick < 6)
                {
                    movement.moveForward -= Math.random();
                    movement.backKeyDown = true;
                }
                else if (afkMoveTick > 6 && afkMoveTick < 8)
                {
                    movement.moveStrafe -= Math.random();
                    movement.rightKeyDown = true;
                }
            }

            // auto login function
            if (AutoLoginFunction.functionDelay == 0)
            {
                if (AutoLoginFunction.forwardTicks > 0 || AutoLoginFunction.forwardAfterCommandTicks > 0)
                {
                    movement.moveForward++;
                }
            }
        }
    }

    @SubscribeEvent
    public void onMouseClick(MouseEvent event)
    {
        if (event.getButton() == 0 && event.isButtonstate())
        {
            IndicatiaEventHandler.LEFT_CLICK.add(System.currentTimeMillis());
        }
        if (event.getButton() == 1 && event.isButtonstate())
        {
            IndicatiaEventHandler.RIGHT_CLICK.add(System.currentTimeMillis());
        }
    }

    @SubscribeEvent
    public void onDisconnectedFromServerEvent(FMLNetworkEvent.ClientDisconnectionFromServerEvent event)
    {
        IndicatiaEventHandler.stopCommandTicks();
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        IndicatiaEventHandler.stopCommandTicks();
    }

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event)
    {
        IndicatiaEventHandler.stopCommandTicks();
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event)
    {
        if (event.phase == TickEvent.Phase.START)
        {
            InfoUtils.INSTANCE.processMouseOverEntity(this.mc);

            if (ConfigManagerIN.indicatia_general.enableSmoothSneakingView)
            {
                if (this.mc.player != null)
                {
                    this.mc.player.eyeHeight = IndicatiaEventHandler.getSmoothEyeHeight(this.mc.player);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPressKey(InputEvent.KeyInputEvent event)
    {
        if (KeyBindingHandler.KEY_QUICK_CONFIG.isKeyDown())
        {
            GuiExtendedConfig config = new GuiExtendedConfig();
            this.mc.displayGuiScreen(config);
        }
        if (KeyBindingHandler.KEY_REC_OVERLAY.isKeyDown())
        {
            HUDRenderEventHandler.recordEnable = !HUDRenderEventHandler.recordEnable;
        }
        if (ConfigManagerIN.indicatia_general.enableCustomCape && KeyBindingHandler.KEY_CUSTOM_CAPE_GUI.isKeyDown())
        {
            GuiCustomCape customCapeGui = new GuiCustomCape();
            this.mc.displayGuiScreen(customCapeGui);
        }
        if (ExtendedConfig.instance.toggleSprintUseMode.equals("key_binding") && KeyBindingHandler.KEY_TOGGLE_SPRINT.isKeyDown())
        {
            ExtendedConfig.instance.toggleSprint = !ExtendedConfig.instance.toggleSprint;
            ClientUtils.setOverlayMessage(JsonUtils.create(ExtendedConfig.instance.toggleSprint ? LangUtils.translate("message.toggle_sprint_enabled") : LangUtils.translate("message.toggle_sprint_disabled")).getFormattedText());
            ExtendedConfig.instance.save();
        }
        if (ExtendedConfig.instance.toggleSneakUseMode.equals("key_binding") && KeyBindingHandler.KEY_TOGGLE_SNEAK.isKeyDown())
        {
            ExtendedConfig.instance.toggleSneak = !ExtendedConfig.instance.toggleSneak;
            ClientUtils.setOverlayMessage(JsonUtils.create(ExtendedConfig.instance.toggleSneak ? LangUtils.translate("message.toggle_sneak_enabled") : LangUtils.translate("message.toggle_sneak_disabled")).getFormattedText());
            ExtendedConfig.instance.save();
        }
        if (KeyBindingHandler.KEY_DONATOR_GUI.isKeyDown())
        {
            GuiDonator donatorGui = new GuiDonator();
            this.mc.displayGuiScreen(donatorGui);
        }
    }

    @SubscribeEvent
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event)
    {
        if (event.getGui() instanceof GuiMainMenu)
        {
            int height = event.getGui().height / 4 + 48;
            event.getButtonList().add(new GuiButtonMojangStatus(200, event.getGui().width / 2 + 104, height + 84));
        }
    }

    @SubscribeEvent
    public void onPreActionPerformedGui(GuiScreenEvent.ActionPerformedEvent.Pre event)
    {
        if (ConfigManagerIN.indicatia_general.enableConfirmDisconnectButton && event.getGui() instanceof GuiIngameMenu && !this.mc.isSingleplayer())
        {
            if (event.getButton().id == 1)
            {
                event.setCanceled(true);
                event.getButton().playPressSound(this.mc.getSoundHandler());

                if (ConfigManagerIN.indicatia_general.confirmDisconnectMode == ConfigManagerIN.General.DisconnectMode.GUI)
                {
                    this.mc.displayGuiScreen(new GuiConfirmDisconnect());
                }
                else
                {
                    this.disconnectClickCount++;
                    event.getButton().displayString = TextFormatting.RED + LangUtils.translate("message.confirm_disconnect");

                    if (this.disconnectClickCount == 1)
                    {
                        this.disconnectClickCooldown = 100;
                    }
                    if (this.disconnectClickCount == 2)
                    {
                        if (this.mc.isConnectedToRealms())
                        {
                            this.mc.world.sendQuittingDisconnectingPacket();
                            this.mc.loadWorld(null);
                            RealmsBridge bridge = new RealmsBridge();
                            bridge.switchToRealms(new GuiMainMenu());
                        }
                        else
                        {
                            this.mc.world.sendQuittingDisconnectingPacket();
                            this.mc.loadWorld(null);
                            this.mc.displayGuiScreen(new GuiMultiplayer(new GuiMainMenu()));
                        }
                        this.disconnectClickCount = 0;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onPostActionPerformedGui(GuiScreenEvent.ActionPerformedEvent.Post event)
    {
        if (event.getGui() instanceof GuiMainMenu)
        {
            if (event.getButton().id == 200)
            {
                this.mc.displayGuiScreen(new GuiMojangStatusChecker(event.getGui()));
            }
        }
    }

    @SubscribeEvent
    public void onRenderHand(RenderHandEvent event)
    {
        if (this.mc.currentScreen instanceof GuiRenderPreview)
        {
            event.setCanceled(true);
            return;
        }
    }

    private static void getRealTimeServerPing(ServerData server)
    {
        IndicatiaEventHandler.serverPinger.submit(() ->
        {
            try
            {
                ServerAddress address = ServerAddress.fromString(server.serverIP);
                NetworkManager manager = NetworkManager.createNetworkManagerAndConnect(InetAddress.getByName(address.getIP()), address.getPort(), false);

                manager.setNetHandler(new INetHandlerStatusClient()
                {
                    private long currentSystemTime = 0L;

                    @Override
                    public void handleServerInfo(SPacketServerInfo packet)
                    {
                        this.currentSystemTime = Minecraft.getSystemTime();
                        manager.sendPacket(new CPacketPing(this.currentSystemTime));
                    }

                    @Override
                    public void handlePong(SPacketPong packet)
                    {
                        long i = this.currentSystemTime;
                        long j = Minecraft.getSystemTime();
                        IndicatiaEventHandler.currentServerPing = (int) (j - i);
                    }

                    @Override
                    public void onDisconnect(ITextComponent component) {}
                });
                manager.sendPacket(new C00Handshake(address.getIP(), address.getPort(), EnumConnectionState.STATUS, true));
                manager.sendPacket(new CPacketServerQuery());
            }
            catch (Exception e) {}
        });
    }

    private static void runAFK(EntityPlayerSP player)
    {
        if (IndicatiaEventHandler.isAFK)
        {
            IndicatiaEventHandler.afkTicks++;
            int tick = IndicatiaEventHandler.afkTicks;
            int messageMin = 1200 * ConfigManagerIN.indicatia_general.afkMessageTime;
            String s = "s";
            float angle = (float)(tick % 2 == 0 ? Math.random() : -Math.random());

            if (tick == 0)
            {
                s = "";
            }
            if (ConfigManagerIN.indicatia_general.enableAFKMessage)
            {
                if (tick % messageMin == 0)
                {
                    String reason = IndicatiaEventHandler.afkReason;
                    reason = reason.isEmpty() ? "" : ", Reason : " + reason;
                    player.sendChatMessage("AFK : " + StringUtils.ticksToElapsedTime(tick) + " minute" + s + reason);
                }
            }

            if (IndicatiaEventHandler.afkMode.equals("idle"))
            {
                player.turn(angle, angle);
            }
            else if (IndicatiaEventHandler.afkMode.equals("360"))
            {
                player.turn((float)(Math.random() + 1.0F), 0.0F);
            }
            else if (IndicatiaEventHandler.afkMode.equals("360_move"))
            {
                player.turn((float)(Math.random() + 1.0F), 0.0F);
                IndicatiaEventHandler.afkMoveTicks++;
                IndicatiaEventHandler.afkMoveTicks %= 8;
            }
            else
            {
                player.turn(angle, angle);
                IndicatiaEventHandler.afkMoveTicks++;
                IndicatiaEventHandler.afkMoveTicks %= 8;
            }
        }
        else
        {
            IndicatiaEventHandler.afkTicks = 0;
        }
    }

    private static void stopCommandTicks()
    {
        if (IndicatiaEventHandler.isAFK)
        {
            IndicatiaEventHandler.isAFK = false;
            IndicatiaEventHandler.afkReason = "";
            IndicatiaEventHandler.afkTicks = 0;
            IndicatiaEventHandler.afkMoveTicks = 0;
            IndicatiaEventHandler.afkMode = "idle";
            LoggerIN.info("Stopping AFK Command");
        }
        if (IndicatiaEventHandler.autoFish)
        {
            IndicatiaEventHandler.autoFish = false;
            IndicatiaEventHandler.autoFishTick = 0;
            LoggerIN.info("Stopping Autofish Command");
        }
    }

    private static void processAutoFish(Minecraft mc)
    {
        if (IndicatiaEventHandler.autoFish)
        {
            ++IndicatiaEventHandler.autoFishTick;
            IndicatiaEventHandler.autoFishTick %= 4;

            if (mc.objectMouseOver != null && mc.world != null && mc.playerController != null && mc.entityRenderer != null)
            {
                if (IndicatiaEventHandler.autoFishTick % 4 == 0)
                {
                    for (EnumHand hand : CachedEnum.handValues)
                    {
                        boolean mainHand = mc.player.getHeldItemMainhand().getItem() instanceof ItemFishingRod;
                        boolean offHand = mc.player.getHeldItemOffhand().getItem() instanceof ItemFishingRod;

                        if (mc.player.getHeldItemMainhand().getItem() instanceof ItemFishingRod)
                        {
                            offHand = false;
                        }

                        if (mainHand || offHand)
                        {
                            ItemStack held = mc.player.getHeldItem(hand);

                            if (mc.objectMouseOver.typeOfHit == RayTraceResult.Type.BLOCK)
                            {
                                BlockPos pos = mc.objectMouseOver.getBlockPos();

                                if (mc.world.getBlockState(pos).getMaterial() != Material.AIR)
                                {
                                    int count = held.getCount();
                                    EnumActionResult result = mc.playerController.processRightClickBlock(mc.player, mc.world, pos, mc.objectMouseOver.sideHit, mc.objectMouseOver.hitVec, hand);

                                    if (result == EnumActionResult.SUCCESS)
                                    {
                                        mc.player.swingArm(hand);

                                        if (!held.isEmpty() && (held.getCount() != count || mc.playerController.isInCreativeMode()))
                                        {
                                            mc.entityRenderer.itemRenderer.resetEquippedProgress(hand);
                                        }
                                        return;
                                    }
                                }
                            }
                            if (!held.isEmpty() && mc.playerController.processRightClick(mc.player, mc.world, hand) == EnumActionResult.SUCCESS)
                            {
                                mc.entityRenderer.itemRenderer.resetEquippedProgress(hand);
                                return;
                            }
                        }
                        else
                        {
                            IndicatiaEventHandler.autoFish = false;
                            IndicatiaEventHandler.autoFishTick = 0;
                            mc.player.sendMessage(JsonUtils.create(LangUtils.translate("message.must_hold_fishing_rod")));
                            return;
                        }
                    }
                }
            }
        }
        else
        {
            IndicatiaEventHandler.autoFishTick = 0;
        }
    }

    private static float getSmoothEyeHeight(EntityPlayer player)
    {
        if (IndicatiaEventHandler.sneakingOld != player.isSneaking() || IndicatiaEventHandler.sneakTimeOld <= 0L)
        {
            IndicatiaEventHandler.sneakTimeOld = System.currentTimeMillis();
        }

        IndicatiaEventHandler.sneakingOld = player.isSneaking();
        float defaultEyeHeight = 1.62F;
        double sneakPress = 0.0006D;
        double sneakValue = 0.005D;
        int sneakTime = -35;
        long smoothRatio = 88L;

        if (player.isSneaking())
        {
            int sneakSystemTime = (int)(IndicatiaEventHandler.sneakTimeOld + smoothRatio - System.currentTimeMillis());

            if (sneakSystemTime > sneakTime)
            {
                defaultEyeHeight += (float)(sneakSystemTime * sneakPress);

                if (defaultEyeHeight < 0.0F || defaultEyeHeight > 10.0F)
                {
                    defaultEyeHeight = 1.54F;
                }
            }
            else
            {
                defaultEyeHeight = (float)(defaultEyeHeight - sneakValue);
            }
        }
        else
        {
            int sneakSystemTime = (int)(IndicatiaEventHandler.sneakTimeOld + smoothRatio - System.currentTimeMillis());

            if (sneakSystemTime > sneakTime)
            {
                defaultEyeHeight -= (float)(sneakSystemTime * sneakPress);
                defaultEyeHeight = (float)(defaultEyeHeight - sneakValue);

                if (defaultEyeHeight < 0.0F)
                {
                    defaultEyeHeight = 1.62F;
                }
            }
            else
            {
                defaultEyeHeight -= 0.0F;
            }
        }
        return defaultEyeHeight;
    }
}