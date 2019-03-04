package fi.dy.masa.minihud.util;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import com.google.common.collect.ArrayListMultimap;
import com.google.gson.JsonObject;
import fi.dy.masa.malilib.util.Constants;
import fi.dy.masa.malilib.util.FileUtils;
import fi.dy.masa.malilib.util.InfoUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.MiniHUD;
import fi.dy.masa.minihud.Reference;
import fi.dy.masa.minihud.mixin.IMixinChunkProviderServer;
import fi.dy.masa.minihud.renderer.OverlayRendererSpawnableColumnHeights;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.*;
import net.minecraft.world.gen.feature.structure.*;

public class DataStorage
{
    private static final Pattern PATTERN_CARPET_TPS = Pattern.compile("TPS: (?<tps>[0-9]+[\\.,][0-9]) MSPT: (?<mspt>[0-9]+[\\.,][0-9])");
    private static final DataStorage INSTANCE = new DataStorage();

    public static final int CARPET_ID_BOUNDINGBOX_MARKERS = 3;
    public static final int CARPET_ID_LARGE_BOUNDINGBOX_MARKERS_START = 7;
    public static final int CARPET_ID_LARGE_BOUNDINGBOX_MARKERS = 8;

    private boolean worldSeedValid;
    private boolean serverTPSValid;
    private boolean carpetServer;
    private boolean worldSpawnValid;
    private boolean hasStructureDataFromServer;
    private boolean structuresDirty;
    private boolean structuresNeedUpdating;
    private long worldSeed;
    private long lastServerTick;
    private long lastServerTimeUpdate;
    private BlockPos lastStructureUpdatePos;
    private double serverTPS;
    private double serverMSPT;
    private int droppedChunksHashSize = -1;
    private BlockPos worldSpawn = BlockPos.ORIGIN;
    private Vec3d distanceReferencePoint = Vec3d.ZERO;
    private final Set<ChunkPos> chunkHeightmapsToCheck = new HashSet<>();
    private final Map<ChunkPos, Integer> spawnableSubChunks = new HashMap<>();
    private final ArrayListMultimap<StructureType, StructureData> structures = ArrayListMultimap.create();
    private final Minecraft mc = Minecraft.getInstance();

    public static DataStorage getInstance()
    {
        return INSTANCE;
    }

    public void onWorldLoad()
    {
        this.worldSeedValid = false;
        this.serverTPSValid = false;
        this.carpetServer = false;
        this.worldSpawnValid = false;
        this.structuresNeedUpdating = true;
        this.hasStructureDataFromServer = false;
        this.structuresDirty = false;

        this.lastStructureUpdatePos = null;
        this.structures.clear();
        this.worldSeed = 0;
    }

    public void setWorldSeed(long seed)
    {
        this.worldSeed = seed;
        this.worldSeedValid = true;
    }

    public void setWorldSpawn(BlockPos spawn)
    {
        this.worldSpawn = spawn;
        this.worldSpawnValid = true;
    }

    public boolean isWorldSeedKnown(DimensionType dimension)
    {
        if (this.worldSeedValid)
        {
            return true;
        }
        else if (this.mc.isSingleplayer())
        {
            MinecraftServer server = this.mc.getIntegratedServer();
            World worldTmp = server.getWorld(dimension);
            return worldTmp != null;
        }

        return false;
    }

    public long getWorldSeed(DimensionType dimension)
    {
        if (this.worldSeedValid == false && this.mc.isSingleplayer())
        {
            MinecraftServer server = this.mc.getIntegratedServer();
            World worldTmp = server.getWorld(dimension);

            if (worldTmp != null)
            {
                this.setWorldSeed(worldTmp.getSeed());
            }
        }

        return this.worldSeed;
    }

    public boolean isWorldSpawnKnown()
    {
        return this.worldSpawnValid || mc.world != null;
    }

    public BlockPos getWorldSpawn()
    {
        if (this.worldSpawnValid == false)
        {
            World world = mc.world;

            if (world != null)
            {
                this.worldSpawn = world.getSpawnPoint();
                this.worldSpawnValid = true;
            }
        }

        return this.worldSpawn;
    }

    public boolean isServerTPSValid()
    {
        return this.serverTPSValid;
    }

    public boolean isCarpetServer()
    {
        return this.carpetServer;
    }

    public double getServerTPS()
    {
        return this.serverTPS;
    }

    public double getServerMSPT()
    {
        return this.serverMSPT;
    }

    public boolean hasStructureDataChanged()
    {
        return this.structuresDirty;
    }

    public void setStructuresNeedUpdating()
    {
        this.structuresNeedUpdating = true;
    }

    public int getDroppedChunksHashSize()
    {
        if (this.droppedChunksHashSize > 0)
        {
            return this.droppedChunksHashSize;
        }

        if (mc.isSingleplayer())
        {
            return MiscUtils.getCurrentHashSize(mc.getIntegratedServer().getWorld(mc.player.getEntityWorld().dimension.getType()));
        }
        else
        {
            return 0xFFFF;
        }
    }

    public Vec3d getDistanceReferencePoint()
    {
        return this.distanceReferencePoint;
    }

    public void setDistanceReferencePoint(Vec3d pos)
    {
        this.distanceReferencePoint = pos;
        String str = String.format("x: %.2f, y: %.2f, z: %.2f", pos.x, pos.y, pos.z);
        InfoUtils.printActionbarMessage("minihud.message.distance_reference_point_set", str);
    }

    public void markChunkForHeightmapCheck(int chunkX, int chunkZ)
    {
        OverlayRendererSpawnableColumnHeights.markChunkChanged(chunkX, chunkZ);
        this.chunkHeightmapsToCheck.add(new ChunkPos(chunkX, chunkZ));
    }

    public void checkQueuedDirtyChunkHeightmaps()
    {
        WorldClient world = this.mc.world;

        if (world != null)
        {
            if (this.chunkHeightmapsToCheck.isEmpty() == false)
            {
                for (ChunkPos pos : this.chunkHeightmapsToCheck)
                {
                    Chunk chunk = world.getChunk(pos.x, pos.z);
                    Heightmap hm = chunk.getHeightmap(Heightmap.Type.LIGHT_BLOCKING);
                    int maxHeight = -1;

                    for (int x = 0; x < 16; ++x)
                    {
                        for (int z = 0; z < 16; ++z)
                        {
                            int h = hm.getHeight(x, z);

                            if (h > maxHeight)
                            {
                                maxHeight = h;
                            }
                        }
                    }

                    int subChunks;

                    if (maxHeight >= 0)
                    {
                        subChunks = MathHelper.clamp((maxHeight / 16) + 1, 1, 16);
                    }
                    // Void world? Use the topFilledSegment, see WorldEntitySpawner.getRandomChunkPosition()
                    else
                    {
                        subChunks = MathHelper.clamp((chunk.getTopFilledSegment() + 16) / 16, 1, 16);
                    }

                    //System.out.printf("@ %d, %d - subChunks: %d, maxHeight: %d\n", pos.x, pos.z, subChunks, maxHeight);

                    this.spawnableSubChunks.put(pos, subChunks);
                }
            }
        }
        else
        {
            this.spawnableSubChunks.clear();
        }

        this.chunkHeightmapsToCheck.clear();
    }

    public void onChunkUnload(int chunkX, int chunkZ)
    {
        ChunkPos pos = new ChunkPos(chunkX, chunkZ);
        this.chunkHeightmapsToCheck.remove(pos);
        this.spawnableSubChunks.remove(pos);
    }

    public int getSpawnableSubChunkCountFor(int chunkX, int chunkZ)
    {
        Integer count = this.spawnableSubChunks.get(new ChunkPos(chunkX, chunkZ));
        return count != null ? count.intValue() : -1;
    }

    public boolean onSendChatMessage(EntityPlayer player, String message)
    {
        String[] parts = message.split(" ");

        if (parts[0].equals("minihud-seed"))
        {
            if (parts.length == 2)
            {
                try
                {
                    this.setWorldSeed(Long.parseLong(parts[1]));
                    MiscUtils.printInfoMessage("minihud.message.seed_set", Long.valueOf(this.worldSeed));
                }
                catch (NumberFormatException e)
                {
                    MiscUtils.printInfoMessage("minihud.message.error.invalid_seed");
                }
            }
            else if (this.worldSeedValid && parts.length == 1)
            {
                MiscUtils.printInfoMessage("minihud.message.seed_set", Long.valueOf(this.worldSeed));
            }

            return true;
        }
        else if (parts[0].equals("minihud-dropped-chunks-hash-size"))
        {
            if (parts.length == 2)
            {
                try
                {
                    this.droppedChunksHashSize = Integer.parseInt(parts[1]);
                    MiscUtils.printInfoMessage("minihud.message.dropped_chunks_hash_size_set", Integer.valueOf(this.droppedChunksHashSize));
                }
                catch (NumberFormatException e)
                {
                    MiscUtils.printInfoMessage("minihud.message.error.invalid_dropped_chunks_hash_size");
                }
            }
            else if (parts.length == 1)
            {
                MiscUtils.printInfoMessage("minihud.message.dropped_chunks_hash_size_set", Integer.valueOf(this.getDroppedChunksHashSize()));
            }

            return true;
        }

        return false;
    }

    public void onChatMessage(ITextComponent message)
    {
        if (message instanceof TextComponentTranslation)
        {
            TextComponentTranslation text = (TextComponentTranslation) message;

            // The vanilla "/seed" command
            if ("commands.seed.success".equals(text.getKey()))
            {
                try
                {
                    String str = text.getString();
                    int i1 = str.indexOf("[");
                    int i2 = str.indexOf("]");

                    if (i1 != -1 && i2 != -1)
                    {
                        this.setWorldSeed(Long.parseLong(str.substring(i1 + 1, i2)));
                        MiniHUD.logger.info("Received world seed from the vanilla /seed command: {}", this.worldSeed);
                        MiscUtils.printInfoMessage("minihud.message.seed_set", Long.valueOf(this.worldSeed));
                    }
                }
                catch (Exception e)
                {
                    MiniHUD.logger.warn("Failed to read the world seed from '{}'", text.getFormatArgs()[0], e);
                }
            }
            // The "/jed seed" command
            else if ("jed.commands.seed.success".equals(text.getKey()))
            {
                try
                {
                    this.setWorldSeed(Long.parseLong(text.getFormatArgs()[1].toString()));
                    MiniHUD.logger.info("Received world seed from the JED '/jed seed' command: {}", this.worldSeed);
                    MiscUtils.printInfoMessage("minihud.message.seed_set", Long.valueOf(this.worldSeed));
                }
                catch (Exception e)
                {
                    MiniHUD.logger.warn("Failed to read the world seed from '{}'", text.getFormatArgs()[1], e);
                }
            }
            else if ("commands.setworldspawn.success".equals(text.getKey()) && text.getFormatArgs().length == 3)
            {
                try
                {
                    Object[] o = text.getFormatArgs();
                    int x = Integer.parseInt(o[0].toString());
                    int y = Integer.parseInt(o[1].toString());
                    int z = Integer.parseInt(o[2].toString());

                    this.setWorldSpawn(new BlockPos(x, y, z));

                    String spawnStr = String.format("x: %d, y: %d, z: %d", this.worldSpawn.getX(), this.worldSpawn.getY(), this.worldSpawn.getZ());
                    MiniHUD.logger.info("Received world spawn from the vanilla /setworldspawn command: {}", spawnStr);
                    MiscUtils.printInfoMessage("minihud.message.spawn_set", spawnStr);
                }
                catch (Exception e)
                {
                    MiniHUD.logger.warn("Failed to read the world spawn point from '{}'", text.getFormatArgs(), e);
                }
            }
        }
    }

    public void onServerTimeUpdate(long totalWorldTime)
    {
        // Carpet server sends the TPS and MSPT values via the player list footer data,
        // and for single player the data is grabbed directly from the integrated server.
        if (this.carpetServer == false && this.mc.isSingleplayer() == false)
        {
            long currentTime = System.nanoTime();

            if (this.serverTPSValid)
            {
                long elapsedTicks = totalWorldTime - this.lastServerTick;

                if (elapsedTicks > 0)
                {
                    this.serverMSPT = ((double) (currentTime - this.lastServerTimeUpdate) / (double) elapsedTicks) / 1000000D;
                    this.serverTPS = this.serverMSPT <= 50 ? 20D : (1000D / this.serverMSPT);
                }
            }

            this.lastServerTick = totalWorldTime;
            this.lastServerTimeUpdate = currentTime;
            this.serverTPSValid = true;
        }
    }

    public void updateIntegratedServerTPS()
    {
        if (this.mc != null && this.mc.player != null && this.mc.getIntegratedServer() != null)
        {
            this.serverMSPT = (double) MathHelper.average(this.mc.getIntegratedServer().tickTimeArray) / 1000000D;
            this.serverTPS = this.serverMSPT <= 50 ? 20D : (1000D / this.serverMSPT);
            this.serverTPSValid = true;
        }
    }

    /**
     * Gets a copy of the structure data map, and clears the dirty flag
     * @return
     */
    public ArrayListMultimap<StructureType, StructureData> getCopyOfStructureData()
    {
        ArrayListMultimap<StructureType, StructureData> copy = ArrayListMultimap.create();

        synchronized (this.structures)
        {
            for (StructureType type : StructureType.values())
            {
                Collection<StructureData> values = this.structures.get(type);

                if (values.isEmpty() == false)
                {
                    copy.putAll(type, values);
                }
            }

            this.structuresDirty = false;
        }

        return copy;
    }

    public void updateStructureData()
    {
        if (this.mc != null && this.mc.world != null && this.mc.player != null)
        {
            final BlockPos playerPos = new BlockPos(this.mc.player);

            if (this.mc.isSingleplayer())
            {
                if (this.structuresNeedUpdating(playerPos, 32))
                {
                    this.updateStructureDataFromIntegratedServer(playerPos);
                }
            }
            else if (this.hasStructureDataFromServer == false)
            {
                if (this.structuresNeedUpdating(playerPos, 1024))
                {
                    this.updateStructureDataFromNBTFiles(playerPos);
                }
            }
        }
    }

    private boolean structuresNeedUpdating(BlockPos playerPos, int hysteresis)
    {
        return this.structuresNeedUpdating || this.lastStructureUpdatePos == null ||
                Math.abs(playerPos.getX() - this.lastStructureUpdatePos.getX()) >= hysteresis ||
                Math.abs(playerPos.getY() - this.lastStructureUpdatePos.getY()) >= hysteresis ||
                Math.abs(playerPos.getZ() - this.lastStructureUpdatePos.getZ()) >= hysteresis;
    }

    private void updateStructureDataFromIntegratedServer(final BlockPos playerPos)
    {
        final DimensionType dimension = this.mc.player.dimension;
        final WorldServer world = this.mc.getIntegratedServer().getWorld(dimension);

        synchronized (this.structures)
        {
            this.structures.clear();
        }

        if (world != null)
        {
            final IChunkGenerator chunkGenerator = ((IMixinChunkProviderServer) world.getChunkProvider()).getChunkGenerator();
            final DataStorage storage = this;
            final int maxRange = (this.mc.gameSettings.renderDistanceChunks + 4) * 16;

            world.addScheduledTask(new Runnable()
            {
                @Override
                public void run()
                {
                    synchronized (storage.structures)
                    {
                        storage.addStructureDataFromGenerator(chunkGenerator, playerPos, maxRange);
                    }
                }
            });
        }

        this.lastStructureUpdatePos = playerPos;
        this.structuresNeedUpdating = false;
    }

    private void updateStructureDataFromNBTFiles(final BlockPos playerPos)
    {
        synchronized (this.structures)
        {
            this.structures.clear();

            File dir = this.getLocalStructureFileDirectory();

            if (dir != null && dir.exists() && dir.isDirectory())
            {
                for (StructureType type : StructureType.values())
                {
                    if (type.isTemple() == false)
                    {
                        NBTTagCompound nbt = FileUtils.readNBTFile(new File(dir, type.getStructureName() + ".dat"));

                        if (nbt != null)
                        {
                            StructureData.readAndAddStructuresToMap(this.structures, nbt, type);
                        }
                    }
                }

                NBTTagCompound nbt = FileUtils.readNBTFile(new File(dir, "Temple.dat"));

                if (nbt != null)
                {
                    StructureData.readAndAddTemplesToMap(this.structures, nbt);
                }

                MiniHUD.logger.info("Structure data updated from local structure files, structures: {}", this.structures.size());
            }
        }

        this.lastStructureUpdatePos = playerPos;
        this.structuresDirty = true;
        this.structuresNeedUpdating = false;
    }

    public void updateStructureDataFromServer(PacketBuffer data)
    {
        try
        {
            data.readerIndex(0);

            if (data.readerIndex() < data.writerIndex() - 4)
            {
                int type = data.readInt();

                if (type == CARPET_ID_BOUNDINGBOX_MARKERS)
                {
                    this.readStructureDataCarpetAll(data.readCompoundTag());
                }
                else if (type == CARPET_ID_LARGE_BOUNDINGBOX_MARKERS_START)
                {
                    NBTTagCompound nbt = data.readCompoundTag();
                    int boxCount = data.readVarInt();
                    this.readStructureDataCarpetSplitHeader(nbt, boxCount);
                }
                else if (type == CARPET_ID_LARGE_BOUNDINGBOX_MARKERS)
                {
                    int boxCount = data.readByte();
                    this.readStructureDataCarpetSplitBoxes(data, boxCount);
                }
            }

            data.readerIndex(0);
        }
        catch (Exception e)
        {
            MiniHUD.logger.warn("Failed to read structure data from Carpet mod packet", e);
        }
    }

    private void readStructureDataCarpetAll(NBTTagCompound nbt)
    {
        NBTTagList tagList = nbt.getList("Boxes", Constants.NBT.TAG_LIST);
        this.setWorldSeed(nbt.getLong("Seed"));

        synchronized (this.structures)
        {
            this.structures.clear();
            StructureData.readStructureDataCarpetAllBoxes(this.structures, tagList);
            this.hasStructureDataFromServer = true;
            this.structuresDirty = true;
            MiniHUD.logger.info("Structure data updated from Carpet server (all), structures: {}", this.structures.size());
        }
    }

    private void readStructureDataCarpetSplitHeader(NBTTagCompound nbt, int boxCount)
    {
        this.setWorldSeed(nbt.getLong("Seed"));

        synchronized (this.structures)
        {
            this.structures.clear();
            StructureData.readStructureDataCarpetIndividualBoxesHeader(boxCount);
        }
    }

    private void readStructureDataCarpetSplitBoxes(PacketBuffer data, int boxCount) throws IOException
    {
        synchronized (this.structures)
        {
            for (int i = 0; i < boxCount; ++i)
            {
                NBTTagCompound nbt = data.readCompoundTag();
                StructureData.readStructureDataCarpetIndividualBoxes(this.structures, nbt);
            }

            this.hasStructureDataFromServer = true;
            this.structuresDirty = true;
        }
    }

    private void addStructureDataFromGenerator(IChunkGenerator chunkGenerator, BlockPos playerPos, int maxRange)
    {
        if (chunkGenerator instanceof ChunkGeneratorOverworld)
        {
            this.addStructuresWithinRange(StructureType.OCEAN_MONUMENT, ((ChunkGeneratorOverworld)chunkGenerator).getStructureReferenceToStartMap(Structure.OCEAN_MONUMENT), playerPos, maxRange);
            this.addStructuresWithinRange(((ChunkGeneratorOverworld)chunkGenerator).getStructureReferenceToStartMap(Structure.OCEAN_RUIN), playerPos, maxRange);
            this.addStructuresWithinRange(((ChunkGeneratorOverworld)chunkGenerator).getStructureReferenceToStartMap(Structure.DESERT_PYRAMID), playerPos, maxRange);
            this.addStructuresWithinRange(((ChunkGeneratorOverworld)chunkGenerator).getStructureReferenceToStartMap(Structure.JUNGLE_PYRAMID), playerPos, maxRange);
            this.addStructuresWithinRange(StructureType.STRONGHOLD, ((ChunkGeneratorOverworld)chunkGenerator).getStructureReferenceToStartMap(Structure.STRONGHOLD), playerPos, maxRange);
            this.addStructuresWithinRange(StructureType.VILLAGE, ((ChunkGeneratorOverworld)chunkGenerator).getStructureReferenceToStartMap(Structure.VILLAGE), playerPos, maxRange);
            this.addStructuresWithinRange(StructureType.MANSION, ((ChunkGeneratorOverworld)chunkGenerator).getStructureReferenceToStartMap(Structure.WOODLAND_MANSION), playerPos, maxRange);
            this.addStructuresWithinRange(((ChunkGeneratorOverworld)chunkGenerator).getStructureReferenceToStartMap(Structure.SWAMP_HUT), playerPos, maxRange);
            this.addStructuresWithinRange(((ChunkGeneratorOverworld)chunkGenerator).getStructureReferenceToStartMap(Structure.IGLOO), playerPos, maxRange);
        }
        else if (chunkGenerator instanceof ChunkGeneratorNether)
        {
            this.addStructuresWithinRange(StructureType.NETHER_FORTRESS, ((ChunkGeneratorNether)chunkGenerator).getStructureReferenceToStartMap(Structure.FORTRESS), playerPos, maxRange);
        }
        else if (chunkGenerator instanceof ChunkGeneratorEnd)
        {
            this.addStructuresWithinRange(StructureType.END_CITY, ((ChunkGeneratorNether)chunkGenerator).getStructureReferenceToStartMap(Structure.END_CITY), playerPos, maxRange);
        }
        else if (chunkGenerator instanceof ChunkGeneratorFlat)
        {
            this.addStructuresWithinRange(StructureType.OCEAN_MONUMENT, ((ChunkGeneratorOverworld)chunkGenerator).getStructureReferenceToStartMap(Structure.OCEAN_MONUMENT), playerPos, maxRange);
            this.addStructuresWithinRange(((ChunkGeneratorOverworld)chunkGenerator).getStructureReferenceToStartMap(Structure.DESERT_PYRAMID), playerPos, maxRange);
            this.addStructuresWithinRange(StructureType.STRONGHOLD, ((ChunkGeneratorOverworld)chunkGenerator).getStructureReferenceToStartMap(Structure.STRONGHOLD), playerPos, maxRange);
            this.addStructuresWithinRange(StructureType.VILLAGE, ((ChunkGeneratorOverworld)chunkGenerator).getStructureReferenceToStartMap(Structure.VILLAGE), playerPos, maxRange);
        }

        this.structuresDirty = true;
        MiniHUD.logger.info("Structure data updated from the integrated server");
    }

    private void addStructuresWithinRange(StructureType type, Long2ObjectMap<StructureStart> structureMap, BlockPos playerPos, int maxRange)
    {
        for (StructureStart start : structureMap.values())
        {
            if (start.getBoundingBox() != null && MiscUtils.isStructureWithinRange(start.getBoundingBox(), playerPos, maxRange))
            {
                this.structures.put(type, StructureData.fromStructure(start));
            }
        }
    }

    private void addStructuresWithinRange(Long2ObjectMap<StructureStart> structureMap, BlockPos playerPos, int maxRange)
    {
        for (StructureStart start : structureMap.values())
        {
            List<StructurePiece> components = start.getComponents();

            if (components.size() == 1 && MiscUtils.isStructureWithinRange(start.getBoundingBox(), playerPos, maxRange))
            {
                String id = StructureIO.getStructureComponentName(components.get(0));
                StructureType type = StructureType.templeTypeFromComponentId(id);

                if (type != null)
                {
                    this.structures.put(type, StructureData.fromStructure(start));
                }
            }
        }
    }

    public void handleCarpetServerTPSData(ITextComponent textComponent)
    {
        if (textComponent.getFormattedText().isEmpty() == false)
        {
            String text = TextFormatting.getTextWithoutFormattingCodes(textComponent.getString());
            String[] lines = text.split("\n");

            for (String line : lines)
            {
                Matcher matcher = PATTERN_CARPET_TPS.matcher(line);

                if (matcher.matches())
                {
                    try
                    {
                        this.serverTPS = Double.parseDouble(matcher.group("tps"));
                        this.serverMSPT = Double.parseDouble(matcher.group("mspt"));
                        this.serverTPSValid = true;
                        this.carpetServer = true;
                        return;
                    }
                    catch (NumberFormatException e)
                    {
                    }
                }
            }
        }

        this.serverTPSValid = false;
    }

    @Nullable
    private File getLocalStructureFileDirectory()
    {
        String dirName = StringUtils.getWorldOrServerName();

        if (dirName != null)
        {
            File dir = new File(new File(FileUtils.getConfigDirectory(), Reference.MOD_ID), "structures");
            return new File(dir, dirName);
        }

        return null;
    }

    public JsonObject toJson()
    {
        JsonObject obj = new JsonObject();

        obj.add("distance_pos", JsonUtils.vec3dToJson(this.distanceReferencePoint));

        return obj;
    }

    public void fromJson(JsonObject obj)
    {
        Vec3d pos = JsonUtils.vec3dFromJson(obj, "distance_pos");

        if (pos != null)
        {
            this.distanceReferencePoint = pos;
        }
        else
        {
            this.distanceReferencePoint = Vec3d.ZERO;
        }
    }
}
