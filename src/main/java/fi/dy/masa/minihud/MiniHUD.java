package fi.dy.masa.minihud;

import fi.dy.masa.minihud.network.CarpetPluginChannel;
import fi.dy.masa.minihud.network.PluginChannelRegister;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dimdev.rift.listener.MessageAdder;
import org.dimdev.rift.network.Message;
import org.dimdev.riftloader.listener.InitializationListener;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.Mixins;
import fi.dy.masa.malilib.event.InitializationHandler;

public class MiniHUD implements InitializationListener, MessageAdder
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
    public void registerMessages(IRegistry<Class<? extends Message>> registry) {
        registry.put(PluginChannelRegister.REGISTER, PluginChannelRegister.class);
        registry.put(new ResourceLocation(CARPET_CHANNEL_NAME), CarpetPluginChannel.class);
    }
}
