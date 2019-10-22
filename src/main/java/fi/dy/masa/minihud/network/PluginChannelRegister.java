package fi.dy.masa.minihud.network;

import com.google.common.base.Charsets;
import fi.dy.masa.minihud.MiniHUD;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import org.dimdev.rift.network.ClientMessageContext;
import org.dimdev.rift.network.Message;
import org.dimdev.rift.network.ServerMessageContext;

public class PluginChannelRegister extends Message {
    public static ResourceLocation REGISTER = new ResourceLocation("pluginchannelmanager", "register");

    @Override
    public void read(PacketBuffer buffer) {
    }

    @Override
    public void process(ClientMessageContext context) {
    }

    @Override
    public void process(ServerMessageContext context) {
    }

    @Override
    public void write(PacketBuffer buffer) {
        StringBuilder channelList = new StringBuilder();

        //channelList.append("\u0000");
        channelList.append(MiniHUD.CHANNEL_CARPET_CLIENT_OLD);
        channelList.append("\u0000");
        channelList.append(MiniHUD.CHANNEL_CARPET_CLIENT_NEW);

        buffer.writeBytes(channelList.toString().getBytes(Charsets.UTF_8));
    }
}
