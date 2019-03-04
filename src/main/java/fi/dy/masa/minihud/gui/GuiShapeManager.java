package fi.dy.masa.minihud.gui;

import javax.annotation.Nullable;
import com.google.common.collect.ImmutableList;
import fi.dy.masa.malilib.gui.GuiListBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ISelectionListener;
import fi.dy.masa.malilib.gui.widgets.WidgetDropDownList;
import fi.dy.masa.minihud.config.InfoToggle;
import fi.dy.masa.minihud.gui.GuiConfigs.ConfigGuiTab;
import fi.dy.masa.minihud.gui.widgets.WidgetListShapes;
import fi.dy.masa.minihud.gui.widgets.WidgetShapeEntry;
import fi.dy.masa.minihud.renderer.shapes.ShapeBase;
import fi.dy.masa.minihud.renderer.shapes.ShapeDespawnSphere;
import fi.dy.masa.minihud.renderer.shapes.ShapeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;

public class GuiShapeManager extends GuiListBase<ShapeBase, WidgetShapeEntry, WidgetListShapes>
                             implements ISelectionListener<ShapeBase>
{
    public GuiShapeManager()
    {
        super(10, 68);

        this.title = I18n.format("minihud.gui.title.shape_manager");
    }

    @Override
    protected int getBrowserWidth()
    {
        return this.width - 20;
    }

    @Override
    protected int getBrowserHeight()
    {
        return this.height - 68;
    }

    @Override
    public void initGui()
    {
        super.initGui();

        int x = 10;
        int y = 26;

        for (ConfigGuiTab tab : ConfigGuiTab.values())
        {
            x += this.createTabButton(x, y, -1, tab);
        }

        y += 24;

        this.addButton(this.width - 10, y, ButtonListener.Type.ADD_SHAPE);
    }

    protected int addButton(int x, int y, ButtonListener.Type type)
    {
        ButtonGeneric button = new ButtonGeneric(x, y, -1, 20, type.getDisplayName());
        button.x -= button.getWidth();

        this.addButton(button, new ButtonListener(ButtonListener.Type.ADD_SHAPE, this));
        WidgetDropDownList<InfoToggle> dd = new WidgetDropDownList<InfoToggle>(button.x - 160, y, 140, 18, 200, 6, this.zLevel + 1, ImmutableList.copyOf(InfoToggle.values()));
        this.addWidget(dd);

        return button.getWidth();
    }

    private int createTabButton(int x, int y, int width, ConfigGuiTab tab)
    {
        ButtonGeneric button = new ButtonGeneric(x, y, width, 20, tab.getDisplayName());
        button.enabled = GuiConfigs.tab != tab;
        this.addButton(button, new ButtonListenerTab(tab));

        return button.getWidth() + 2;
    }

    @Override
    public void onSelectionChange(@Nullable ShapeBase entry)
    {
        ShapeBase old = ShapeManager.INSTANCE.getSelectedShape();
        ShapeManager.INSTANCE.setSelectedShape(old == entry ? null : entry);
    }

    @Override
    protected WidgetListShapes createListWidget(int listX, int listY)
    {
        return new WidgetListShapes(listX, listY, this.getBrowserWidth(), this.getBrowserHeight(), this.zLevel, this);
    }

    private static class ButtonListener implements IButtonActionListener<ButtonGeneric>
    {
        private final Type type;
        private final GuiShapeManager gui;

        public ButtonListener(Type type, GuiShapeManager gui)
        {
            this.type = type;
            this.gui = gui;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
            if (this.type == Type.ADD_SHAPE)
            {
                // TODO
                ShapeManager.INSTANCE.addShape(new ShapeDespawnSphere());
                this.gui.getListWidget().refreshEntries();
            }
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            this.actionPerformed(control);
        }

        public enum Type
        {
            ADD_SHAPE   ("minihud.gui.button.add_shape");

            private final String translationKey;

            private Type(String translationKey)
            {
                this.translationKey = translationKey;
            }

            public String getDisplayName()
            {
                return I18n.format(this.translationKey);
            }
        }
    }

    private static class ButtonListenerTab implements IButtonActionListener<ButtonGeneric>
    {
        private final ConfigGuiTab tab;

        public ButtonListenerTab(ConfigGuiTab tab)
        {
            this.tab = tab;
        }

        @Override
        public void actionPerformed(ButtonGeneric control)
        {
        }

        @Override
        public void actionPerformedWithButton(ButtonGeneric control, int mouseButton)
        {
            GuiConfigs.tab = this.tab;
            Minecraft.getInstance().displayGuiScreen(new GuiConfigs());
        }
    }
}
