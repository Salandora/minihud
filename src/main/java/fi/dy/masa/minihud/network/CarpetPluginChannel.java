package fi.dy.masa.minihud.network;

import fi.dy.masa.minihud.util.DataStorage;
import net.minecraft.network.PacketBuffer;
import org.dimdev.rift.network.ClientMessageContext;
import org.dimdev.rift.network.Message;
import org.dimdev.rift.network.ServerMessageContext;

public class CarpetPluginChannel extends Message {
    public static void sendBoundingBoxMarkerRequest() {
        CarpetPluginChannel channel = new CarpetPluginChannel(DataStorage.CARPET_ID_BOUNDINGBOX_MARKERS);
        channel.sendToServer();
    }

    private int request;

    public CarpetPluginChannel() {}

    public CarpetPluginChannel(int request) {
        this.request = request;
    }

    @Override
    public void write(PacketBuffer buffer) {
        buffer.writeInt(request);
    }

    @Override
    public void read(PacketBuffer buffer) {
        DataStorage.getInstance().updateStructureDataFromServer(buffer);
    }

    @Override
    public void process(ClientMessageContext context) {
    }

    @Override
    public void process(ServerMessageContext context) {
    }
}
