package fi.dy.masa.minihud;

import fi.dy.masa.malilib.event.WorldLoadHandler;
import fi.dy.masa.minihud.event.WorldLoadListener;
import fi.dy.masa.minihud.hotkeys.KeyCallbacks;
import fi.dy.masa.minihud.network.CarpetPluginChannel;
import fi.dy.masa.minihud.network.PluginChannelRegister;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dimdev.rift.listener.MessageAdder;
import org.dimdev.rift.listener.client.ClientTickable;
import org.dimdev.rift.network.Message;
import org.dimdev.riftloader.listener.InitializationListener;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import fi.dy.masa.malilib.config.ConfigManager;
import fi.dy.masa.malilib.event.InitializationHandler;
import fi.dy.masa.malilib.event.InputEventHandler;
import fi.dy.masa.malilib.event.RenderEventHandler;
import fi.dy.masa.malilib.hotkeys.IHotkeyCallback;
import fi.dy.masa.malilib.hotkeys.IKeybind;
import fi.dy.masa.malilib.hotkeys.KeyAction;
import fi.dy.masa.malilib.hotkeys.KeyCallbackToggleBoolean;
import fi.dy.masa.malilib.interfaces.IInitializationHandler;
import fi.dy.masa.minihud.config.Configs;
import fi.dy.masa.minihud.event.InputHandler;
import fi.dy.masa.minihud.event.RenderHandler;
import fi.dy.masa.minihud.gui.GuiConfigs;
import net.minecraft.client.Minecraft;

public class MiniHUD implements ClientTickable, InitializationListener, MessageAdder
{
    public static final Logger logger = LogManager.getLogger(Reference.MOD_ID);

    public static final String CARPET_CHANNEL_NAME = "carpetclient";

    @Override
    public void onInitialization()
    {
        MixinBootstrap.init();
        Mixins.addConfiguration("mixins.minihud.json");

        InitializationHandler.getInstance().registerInitializationHandler(new InitHandler());
    }

    @Override
    public void clientTick(Minecraft mc)
    {
        RenderHandler.getInstance().updateData(mc);
    }

    @Override
    public void registerMessages(IRegistry<Class<? extends Message>> registry) {
        registry.put(PluginChannelRegister.REGISTER, PluginChannelRegister.class);
        registry.put(new ResourceLocation(CARPET_CHANNEL_NAME), CarpetPluginChannel.class);
    }

    private static class InitHandler implements IInitializationHandler
    {
        @Override
        public void registerModHandlers()
        {
            ConfigManager.getInstance().registerConfigHandler(Reference.MOD_ID, new Configs());
            InputEventHandler.getInstance().registerKeybindProvider(InputHandler.getInstance());

            RenderHandler renderer = RenderHandler.getInstance();
            RenderEventHandler.getInstance().registerGameOverlayRenderer(renderer);
            RenderEventHandler.getInstance().registerTooltipLastRenderer(renderer);
            RenderEventHandler.getInstance().registerWorldLastRenderer(renderer);

            WorldLoadListener listener = new WorldLoadListener();
            WorldLoadHandler.getInstance().registerWorldLoadPreHandler(listener);
            WorldLoadHandler.getInstance().registerWorldLoadPostHandler(listener);

            KeyCallbacks.init();
        }
    }
}
