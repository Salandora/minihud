package fi.dy.masa.minihud.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Random;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.config.Configs;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MutableBoundingBox;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;

public class MiscUtils
{
    private static final Random RAND = new Random();

    public static long bytesToMb(long bytes)
    {
        return bytes / 1024L / 1024L;
    }

    public static boolean canSlimeSpawnAt(int posX, int posZ, long worldSeed)
    {
        return canSlimeSpawnInChunk(posX >> 4, posZ >> 4, worldSeed);
    }

    public static boolean canSlimeSpawnInChunk(int chunkX, int chunkZ, long worldSeed)
    {
        long slimeSeed = 987234911L;
        long rngSeed = worldSeed +
                       (long) (chunkX * chunkX *  4987142) + (long) (chunkX * 5947611) +
                       (long) (chunkZ * chunkZ) * 4392871L + (long) (chunkZ * 389711) ^ slimeSeed;

        RAND.setSeed(rngSeed);

        return RAND.nextInt(10) == 0;
    }

    public static int getChunkUnloadBucket(int chunkX, int chunkZ)
    {
        if (Configs.Generic.CHUNK_UNLOAD_BUCKET_WITH_SIZE.getBooleanValue())
        {
            return getChunkOrder(chunkX, chunkZ, DataStorage.getInstance().getDroppedChunksHashSize());
        }
        // The old simple calculation, without knowledge of the HashSet size
        else
        {
            int longHash = Long.valueOf(ChunkPos.asLong(chunkX, chunkZ)).hashCode();
            return (longHash ^ (longHash >>> 16)) & 0xFFFF;
        }
    }

    public static void printInfoMessage(String key, Object... args)
    {
        Minecraft.getInstance().ingameGUI.addChatMessage(ChatType.GAME_INFO, new TextComponentTranslation(key, args));
    }

    /**
     * This method has been taken from the Carpet mod, by gnembon
     */
    public static int getCurrentHashSize(WorldServer server)
    {
        /*
        IMixinChunkProviderServer provider = (IMixinChunkProviderServer) (Object) server.getChunkProvider();

        try
        {
            LongSet droppedChunks = provider.getDroppedChunks();
            Field field = droppedChunks.getClass().getDeclaredField("map");
            field.setAccessible(true);

            @SuppressWarnings("unchecked")
            HashMap<Object, Object> map = (HashMap<Object, Object>) field.get(droppedChunks);
            field = map.getClass().getDeclaredField("table");
            field.setAccessible(true);

            Object[] table = (Object []) field.get(map);

            if (table == null)
            {
                return 2;
            }

            return table.length;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        */

        // FIXME 1.13
        return 1;
    }


    /**
     * This method has been taken from the Carpet mod, by gnembon
     */
    public static int getChunkOrder(int chunkX, int chunkZ, int hashSize)
    {
        try
        {
            Method method = HashMap.class.getDeclaredMethod("hash", Object.class);
            method.setAccessible(true);

            return (Integer) method.invoke(null, Long.hashCode(ChunkPos.asLong(chunkX, chunkZ))) & (hashSize - 1);
        }
        catch (Exception e)
        {
            MiniHUD.logger.error("Error while trying to get the chunk unload order");
            return -1;
        }
    }

    public static boolean isStructureWithinRange(MutableBoundingBox bb, BlockPos playerPos, int maxRange)
    {
        if (playerPos.getX() < (bb.minX - maxRange) ||
            playerPos.getX() > (bb.maxX + maxRange) ||
            playerPos.getZ() < (bb.minZ - maxRange) ||
            playerPos.getZ() > (bb.maxZ + maxRange))
        {
            return false;
        }

        return true;
    }

    public static boolean areBoxesEqual(MutableBoundingBox bb1, MutableBoundingBox bb2)
    {
        return bb1.minX == bb2.minX && bb1.minY == bb2.minY && bb1.minZ == bb2.minZ &&
               bb1.maxX == bb2.maxX && bb1.maxY == bb2.maxY && bb1.maxZ == bb2.maxZ;
    }
}
