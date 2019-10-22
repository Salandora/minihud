package fi.dy.masa.minihud;

import com.google.common.collect.ImmutableList;
import fi.dy.masa.minihud.network.CarpetPluginChannel;
import fi.dy.masa.minihud.network.CarpetPluginChannelPacketSplitter;
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

    public static final String CHANNEL_CARPET_CLIENT_OLD = "carpetclient";
    public static final String CHANNEL_CARPET_CLIENT_NEW = "carpet:client";

    private final ImmutableList<String> pluginChannels = ImmutableList.of(CHANNEL_CARPET_CLIENT_OLD, CHANNEL_CARPET_CLIENT_NEW);

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

        registry.put(new ResourceLocation(CHANNEL_CARPET_CLIENT_OLD), CarpetPluginChannel.class);
        registry.put(new ResourceLocation(CHANNEL_CARPET_CLIENT_NEW), CarpetPluginChannelPacketSplitter.class);
    }
}
