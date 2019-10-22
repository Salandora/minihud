package fi.dy.masa.minihud.renderer.shapes;

import java.util.HashSet;
import java.util.List;
import javax.annotation.Nullable;
import org.lwjgl.opengl.GL11;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import fi.dy.masa.malilib.config.values.BlockSnap;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.EntityUtils;
import fi.dy.masa.malilib.util.JsonUtils;
import fi.dy.masa.malilib.util.LayerRange;
import fi.dy.masa.malilib.util.StringUtils;
import fi.dy.masa.minihud.util.ShapeRenderType;

public abstract class ShapeCircleBase extends ShapeBase
{
    protected static final EnumFacing[] FACING_ALL = new EnumFacing[] { EnumFacing.DOWN, EnumFacing.UP, EnumFacing.NORTH, EnumFacing.SOUTH, EnumFacing.WEST, EnumFacing.EAST };

    protected BlockSnap snap = BlockSnap.CENTER;
    protected EnumFacing mainAxis = EnumFacing.UP;
    protected double radius;
    protected double radiusSq;
    protected double maxRadius = 256.0; // TODO use per-chunk VBOs or something to allow bigger shapes?
    protected Vec3d center = Vec3d.ZERO;
    protected Vec3d effectiveCenter = Vec3d.ZERO;
    protected Vec3d lastUpdatePos = Vec3d.ZERO;
    protected long lastUpdateTime;

    public ShapeCircleBase(ShapeType type, Color4f color, double radius)
    {
        super(type, color);

        this.setRadius(radius);

        Entity entity = EntityUtils.getCameraEntity();

        if (entity != null)
        {
            Vec3d center = entity.getPositionVector();
            center = new Vec3d(Math.floor(center.x) + 0.5, Math.floor(center.y), Math.floor(center.z) + 0.5);
            this.setCenter(center);
        }
        else
        {
            this.setCenter(Vec3d.ZERO);
        }
    }

    public Vec3d getCenter()
    {
        return this.center;
    }

    public void setCenter(Vec3d center)
    {
        this.center = center;
        this.updateEffectiveCenter();
    }

    public double getRadius()
    {
        return this.radius;
    }

    public void setRadius(double radius)
    {
        if (radius >= 0.0 && radius <= this.maxRadius)
        {
            this.radius = radius;
            this.radiusSq = radius * radius;
            this.setNeedsUpdate();
        }
    }

    public EnumFacing getMainAxis()
    {
        return this.mainAxis;
    }

    public void setMainAxis(EnumFacing mainAxis)
    {
        this.mainAxis = mainAxis;
        this.setNeedsUpdate();
    }

    protected BlockPos getCenterBlock()
    {
        return new BlockPos(this.center);
    }

    public BlockSnap getBlockSnap()
    {
        return this.snap;
    }

    public void setBlockSnap(BlockSnap snap)
    {
        this.snap = snap;
        this.updateEffectiveCenter();
    }

    protected void updateEffectiveCenter()
    {
        Vec3d center = this.center;

        if (this.snap == BlockSnap.CENTER)
        {
            this.effectiveCenter = new Vec3d(Math.floor(center.x) + 0.5, Math.floor(center.y), Math.floor(center.z) + 0.5);
        }
        else if (this.snap == BlockSnap.CORNER)
        {
            this.effectiveCenter = new Vec3d(Math.floor(center.x), Math.floor(center.y), Math.floor(center.z));
        }
        else
        {
            this.effectiveCenter = center;
        }

        this.center = this.effectiveCenter;

        this.setNeedsUpdate();
    }

    protected void onPostUpdate(Vec3d updatePosition)
    {
        this.needsUpdate = false;
        this.lastUpdatePos = updatePosition;
        this.lastUpdateTime = System.currentTimeMillis();
    }

    @Override
    public void allocateGlResources()
    {
        this.allocateBuffer(GL11.GL_QUADS);
    }

    @Override
    public void draw(double x, double y, double z)
    {
        GlStateManager.pushMatrix();
        this.preRender(x, y, z);

        this.renderObjects.get(0).draw();

        // Render the lines as quads with glPolygonMode(GL_LINE)
        GlStateManager.polygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        GlStateManager.disableBlend();
        this.renderObjects.get(0).draw();
        GlStateManager.polygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
        GlStateManager.enableBlend();

        GlStateManager.popMatrix();
    }

    @Override
    public JsonObject toJson()
    {
        JsonObject obj = super.toJson();

        obj.add("center", JsonUtils.vec3dToJson(this.center));
        obj.add("snap", new JsonPrimitive(this.snap.getStringValue()));

        return obj;
    }

    @Override
    public void fromJson(JsonObject obj)
    {
        super.fromJson(obj);

        // The snap value has to be set before the center
        if (JsonUtils.hasString(obj, "snap"))
        {
            this.snap = BlockSnap.fromStringStatic(JsonUtils.getString(obj, "snap"));
        }

        Vec3d center = JsonUtils.vec3dFromJson(obj, "center");

        if (center != null)
        {
            this.setCenter(center);
        }
    }

    @Override
    public List<String> getWidgetHoverLines()
    {
        List<String> lines = super.getWidgetHoverLines();
        Vec3d c = this.center;

        String aq = GuiBase.TXT_AQUA;
        String bl = GuiBase.TXT_BLUE;
        String gl = GuiBase.TXT_GOLD;
        String gr = GuiBase.TXT_GRAY;
        String rst = GuiBase.TXT_GRAY;

        lines.add(gr + StringUtils.translate("minihud.gui.label.radius_value", gl + String.valueOf(this.getRadius()) + rst));
        lines.add(gr + StringUtils.translate("minihud.gui.label.center_value",
                String.format("x: %s%.2f%s, y: %s%.2f%s, z: %s%.2f%s",
                        bl, c.x, rst, bl, c.y, rst, bl, c.z, rst)));
        lines.add(gr + StringUtils.translate("minihud.gui.label.block_snap", aq + this.snap.getDisplayName() + rst));

        if (this.snap != BlockSnap.NONE)
        {
            c = this.effectiveCenter;
            lines.add(gr + StringUtils.translate("minihud.gui.label.effective_center_value",
                    String.format("x: %s%.2f%s, y: %s%.2f%s, z: %s%.2f%s",
                        bl, c.x, rst, bl, c.y, rst, bl, c.z, rst)));
        }

        return lines;
    }

    protected void renderPositions(HashSet<BlockPos> positions, EnumFacing[] sides, EnumFacing mainAxis, Color4f color)
    {
        boolean full = this.renderType == ShapeRenderType.FULL_BLOCK;
        boolean outer = this.renderType == ShapeRenderType.OUTER_EDGE;
        boolean inner = this.renderType == ShapeRenderType.INNER_EDGE;
        LayerRange range = this.layerRange;
        BlockPos.MutableBlockPos posMutable = new BlockPos.MutableBlockPos();

        for (BlockPos pos : positions)
        {
            if (range.isPositionWithinRange(pos))
            {
                for (int i = 0; i < sides.length; ++i)
                {
                    EnumFacing side = sides[i];
                    posMutable.setPos(pos.getX() + side.getXOffset(), pos.getY() + side.getYOffset(), pos.getZ() + side.getZOffset());

                    if (positions.contains(posMutable) == false)
                    {
                        boolean render = full;

                        if (full == false)
                        {
                            boolean onOrIn = this.isPositionOnOrInsideRing(posMutable.getX(), posMutable.getY(), posMutable.getZ(), side, mainAxis);
                            render |= ((outer && onOrIn == false) || (inner && onOrIn));
                        }

                        if (render)
                        {
                            fi.dy.masa.malilib.render.RenderUtils.drawBlockSpaceSideBatchedQuads(pos, side, color, 0, BUFFER_1);
                        }
                    }
                }
            }
        }
    }

    protected void addPositionsOnHorizontalRing(HashSet<BlockPos> positions, BlockPos.MutableBlockPos posMutable, EnumFacing direction)
    {
        if (this.movePositionToRing(posMutable, direction, EnumFacing.UP))
        {
            BlockPos posFirst = posMutable.toImmutable();
            positions.add(posFirst);
            double r = (double) this.radius;
            int failsafe = (int) (2.5 * Math.PI * r); // somewhat over double the circumference

            while (--failsafe > 0)
            {
                direction = this.getNextPositionOnHorizontalRing(posMutable, direction);

                if (direction == null || posMutable.equals(posFirst))
                {
                    break;
                }

                positions.add(posMutable.toImmutable());
            }
        }
    }

    protected void addPositionsOnVerticalRing(HashSet<BlockPos> positions, BlockPos.MutableBlockPos posMutable, EnumFacing direction, EnumFacing mainAxis)
    {
        if (this.movePositionToRing(posMutable, direction, mainAxis))
        {
            BlockPos posFirst = posMutable.toImmutable();
            positions.add(posFirst);
            double r = (double) this.radius;
            int failsafe = (int) (2.5 * Math.PI * r); // somewhat over double the circumference

            while (--failsafe > 0)
            {
                direction = this.getNextPositionOnVerticalRing(posMutable, direction, mainAxis);

                if (direction == null || posMutable.equals(posFirst))
                {
                    break;
                }

                positions.add(posMutable.toImmutable());
            }
        }
    }

    protected boolean movePositionToRing(BlockPos.MutableBlockPos posMutable, EnumFacing dir, EnumFacing mainAxis)
    {
        int x = posMutable.getX();
        int y = posMutable.getY();
        int z = posMutable.getZ();
        int xNext = x;
        int yNext = y;
        int zNext = z;
        int failsafe = 0;
        final int failsafeMax = (int) this.radius + 5;

        while (this.isPositionOnOrInsideRing(xNext, yNext, zNext, dir, mainAxis) && ++failsafe < failsafeMax)
        {
            x = xNext;
            y = yNext;
            z = zNext;
            xNext += dir.getXOffset();
            yNext += dir.getYOffset();
            zNext += dir.getZOffset();
        }

        // Successfully entered the loop at least once
        if (failsafe > 0)
        {
            posMutable.setPos(x, y, z);
            return true;
        }

        return false;
    }

    @Nullable
    protected EnumFacing getNextPositionOnHorizontalRing(BlockPos.MutableBlockPos posMutable, EnumFacing dir)
    {
        EnumFacing dirOut = dir;
        EnumFacing ccw90 = getNextDirRotating(dir);
        final int y = posMutable.getY();

        for (int i = 0; i < 4; ++i)
        {
            int x = posMutable.getX() + dir.getXOffset();
            int z = posMutable.getZ() + dir.getZOffset();

            // First check the adjacent position
            if (this.isPositionOnOrInsideRing(x, y, z, dir, EnumFacing.UP))
            {
                posMutable.setPos(x, y, z);
                return dirOut;
            }

            // Then check the diagonal position
            x += ccw90.getXOffset();
            z += ccw90.getZOffset();

            if (this.isPositionOnOrInsideRing(x, y, z, dir, EnumFacing.UP))
            {
                posMutable.setPos(x, y, z);
                return dirOut;
            }

            // Delay the next direction by one cycle, so that it won't get updated too soon on the diagonals
            dirOut = dir;
            dir = getNextDirRotating(dir);
            ccw90 = getNextDirRotating(dir);
        }

        return null;
    }

    @Nullable
    protected EnumFacing getNextPositionOnVerticalRing(BlockPos.MutableBlockPos posMutable, EnumFacing dir, EnumFacing mainAxis)
    {
        EnumFacing dirOut = dir;
        EnumFacing ccw90 = getNextDirRotatingVertical(dir, mainAxis);

        for (int i = 0; i < 4; ++i)
        {
            int x = posMutable.getX() + dir.getXOffset();
            int y = posMutable.getY() + dir.getYOffset();
            int z = posMutable.getZ() + dir.getZOffset();

            // First check the adjacent position
            if (this.isPositionOnOrInsideRing(x, y, z, dir, mainAxis))
            {
                posMutable.setPos(x, y, z);
                return dirOut;
            }

            // Then check the diagonal position
            x += ccw90.getXOffset();
            y += ccw90.getYOffset();
            z += ccw90.getZOffset();

            if (this.isPositionOnOrInsideRing(x, y, z, dir, mainAxis))
            {
                posMutable.setPos(x, y, z);
                return dirOut;
            }

            // Delay the next direction by one cycle, so that it won't get updated too soon on the diagonals
            dirOut = dir;
            dir = getNextDirRotatingVertical(dir, mainAxis);
            ccw90 = getNextDirRotatingVertical(dir, mainAxis);
        }

        return null;
    }

    protected boolean isPositionOnOrInsideRing(int blockX, int blockY, int blockZ, EnumFacing outSide, EnumFacing mainAxis)
    {
        double x = (double) blockX + 0.5;
        double y = (double) blockY + 0.5;
        double z = (double) blockZ + 0.5;
        double dist = this.effectiveCenter.squareDistanceTo(x, y, z);
        double diff = this.radiusSq - dist;

        if (diff > 0)
        {
            return true;
        }

        double xAdj = (double) blockX + outSide.getXOffset() + 0.5;
        double yAdj = (double) blockY + outSide.getYOffset() + 0.5;
        double zAdj = (double) blockZ + outSide.getZOffset() + 0.5;
        double distAdj = this.effectiveCenter.squareDistanceTo(xAdj, yAdj, zAdj);
        double diffAdj = this.radiusSq - distAdj;

        return diffAdj > 0 && Math.abs(diff) < Math.abs(diffAdj);
    }

    protected boolean isAdjacentPositionOutside(BlockPos pos, EnumFacing dir, EnumFacing mainAxis)
    {
        return this.isPositionOnOrInsideRing(pos.getX() + dir.getXOffset(), pos.getY() + dir.getYOffset(), pos.getZ() + dir.getZOffset(), dir, mainAxis) == false;
    }

    /**
     * Returns the next horizontal direction in sequence, rotating counter-clockwise
     * @param dirIn
     * @return
     */
    protected static EnumFacing getNextDirRotating(EnumFacing dirIn)
    {
        switch (dirIn)
        {
            case EAST:  return EnumFacing.NORTH;
            case NORTH: return EnumFacing.WEST;
            case WEST:  return EnumFacing.SOUTH;
            case SOUTH: return EnumFacing.EAST;
            default:    return EnumFacing.NORTH;
        }
    }

    /**
     * Returns the next direction in sequence, rotating up to north
     * @param dirIn
     * @return
     */
    protected static EnumFacing getNextDirRotatingVertical(EnumFacing dirIn, EnumFacing mainAxis)
    {
        switch (mainAxis)
        {
            case UP:
            case DOWN:
                switch (dirIn)
                {
                    case UP:    return EnumFacing.NORTH;
                    case NORTH: return EnumFacing.DOWN;
                    case DOWN:  return EnumFacing.SOUTH;
                    case SOUTH: return EnumFacing.UP;
                    default:    return EnumFacing.NORTH;
                }

            case NORTH:
            case SOUTH:
                switch (dirIn)
                {
                    case UP:    return EnumFacing.EAST;
                    case EAST:  return EnumFacing.DOWN;
                    case DOWN:  return EnumFacing.WEST;
                    case WEST:  return EnumFacing.UP;
                    default:    return EnumFacing.EAST;
                }

            case WEST:
            case EAST:
                switch (dirIn)
                {
                    case UP:    return EnumFacing.SOUTH;
                    case SOUTH: return EnumFacing.DOWN;
                    case DOWN:  return EnumFacing.NORTH;
                    case NORTH: return EnumFacing.UP;
                    default:    return EnumFacing.SOUTH;
                }
        }

        return EnumFacing.UP;
    }
}
