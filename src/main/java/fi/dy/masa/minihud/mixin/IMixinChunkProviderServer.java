package fi.dy.masa.minihud.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;

@Mixin(ChunkProviderServer.class)
public interface IMixinChunkProviderServer
{
    @Accessor
    LongSet getDroppedChunks();

    @Accessor("chunkGenerator")
    IChunkGenerator getChunkGenerator();
}
