package fi.dy.masa.minihud.network;

import fi.dy.masa.malilib.network.PacketSplitter;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.util.DataStorage;
import net.minecraft.network.PacketBuffer;
import org.dimdev.rift.network.ClientMessageContext;
import org.dimdev.rift.network.Message;
import org.dimdev.rift.network.ServerMessageContext;

public class CarpetPluginChannelPacketSplitter extends Message {
    public static void sendBoundingBoxMarkerRequest() {
        CarpetPluginChannelPacketSplitter channel = new CarpetPluginChannelPacketSplitter(DataStorage.CARPET_ID_BOUNDINGBOX_MARKERS);
        channel.sendToServer();
    }

    private int request;

    public CarpetPluginChannelPacketSplitter() {}

    public CarpetPluginChannelPacketSplitter(int request) {
        this.request = request;
    }

    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeInt(request);
    }

    @Override
    public void read(PacketBuffer data) {
        PacketBuffer buffer = PacketSplitter.receive(MiniHUD.CHANNEL_CARPET_CLIENT_NEW, data);

        // Received the complete packet
        if (buffer != null)
        {
            DataStorage.getInstance().updateStructureDataFromCarpetServer(buffer);
        }
    }

    @Override
    public void process(ClientMessageContext context) {
    }

    @Override
    public void process(ServerMessageContext context) {
    }
}
